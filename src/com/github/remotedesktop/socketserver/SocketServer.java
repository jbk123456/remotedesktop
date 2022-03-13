package com.github.remotedesktop.socketserver;

import static com.github.remotedesktop.socketserver.SocketServerAttachment.getWriteBuffer;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.getMulticastGroup;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.getPushBackBuffer;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.addWriteBuffer;
import static com.github.remotedesktop.socketserver.SocketServerAttachment.setPushBackBuffer;
import static java.lang.System.arraycopy;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SocketServer implements Runnable {
	private static final Logger logger = Logger.getLogger(SocketServer.class.getName());

	public static final int BUFFER_SIZE = 65535; // 8 * buffer cache size

	private Object waitForFinishLock = new Object();
	private Object stopLock = new Object();
	private boolean isFinish = false;
	private boolean running = true;

	protected final String id;
	protected final ByteBuffer incomingBuffer;
	protected Selector selector;
	protected AbstractSelectableChannel channel;
	protected final InetSocketAddress address;

	public SocketServer(String id, String addr, int port) throws IOException {
		this(id, addr == null ? new InetSocketAddress(port) : new InetSocketAddress(InetAddress.getByName(addr), port));
	}

	public SocketServer(String id, InetSocketAddress address) throws IOException {
		this.id = id;
		this.address = address;
		this.incomingBuffer = ByteBuffer.allocate(getRecvBufferSize());
		this.channel = channel(address);
		this.selector = selector();
	}

	private final Selector selector() throws IOException {
		Selector selector = openSelector();
		enableAttachments(channelRegister(selector));
		return selector;
	}

	private void enableAttachments(SelectionKey key) {
		key.attach(new SocketServerAttachment());
	}


	public void start() {
		new Thread(this, id).start();
	}

	public void stop() {
		synchronized (stopLock) {
			if (running) {
				logger.info("socket server stop called");
				running = false;
				cleanUp();
			}
		}
	}

	@Override
	public void run() {
		try {
			while (running) {
				runMainLoop();
			}
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "socket server terminated", t);
		} finally {
			logger.info("stopping socket server");
			running = false;
			cleanUp();
			synchronized (waitForFinishLock) {
				logger.info("stopped socket server");
				isFinish = true;
				waitForFinishLock.notifyAll();
			}
		}
	}

	public void waitForFinish() throws InterruptedException {
		synchronized (waitForFinishLock) {
			while (!isFinish) {
				waitForFinishLock.wait();
			}
		}
	}

	private void cleanUp() {
		for (SelectionKey key : selector.keys()) {
			close(key.channel());
		}
		close(selector);
	}

	protected int getRecvBufferSize() {
		return BUFFER_SIZE;
	}
	
	private void runMainLoop() {
		try {
			select();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "socket server terminated", e);
			try {
				Thread.sleep(10000);
				if (selector.isOpen())
					close(selector);
				if (channel.isOpen())
					close(channel);

				// try again
				this.channel = channel(address);
				this.selector = selector();
			} catch (Exception ee) {
				logger.log(Level.SEVERE, "fatal error", ee);
			}
		}
	}

	private void select() throws IOException {
		selector.select();

		Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

		while (running && iterator.hasNext()) {
			SelectionKey key = iterator.next();
			try {
				iterator.remove();

				if (!key.isValid()) {
					continue;
				}

				if (key.isConnectable()) {
					connect(key);
				} else if (key.isAcceptable()) {
					accept(key);
				} else if (key.isReadable()) {
					read(key);
				} else if (key.isWritable()) {
					write(key);
				}
			} catch (IOException e) {
				logger.log(Level.SEVERE, "select", e);
				cancelKey(key);
			}
		}
	}

	protected void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		enableAttachments(socketChannel.register(selector, OP_READ));
	}

	protected void connect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		boolean connected = channel.finishConnect();
		if (!connected) {
			logger.fine("not yet connected");
		}
		channel.configureBlocking(false);
		enableAttachments(channel.register(selector, OP_READ));
	}

	protected final void read(SelectionKey key) throws IOException {
		byte[] data;
		int c = 0;
		SocketChannel channel = (SocketChannel) key.channel();
		incomingBuffer.clear();

		do {
			c = channel.read(incomingBuffer);
		} while (c > 0);

		logger.finest(String.format("read %d bytes", incomingBuffer.position()));
		
		if (c == -1) {
			logger.log(Level.WARNING, "cannot read data, connection lost");
			throw new IOException("disconnected");
		}
		int count = incomingBuffer.position();
		if (count == 0) {
			logger.fine("could not read data yet, sleeping...");
			key.interestOps(OP_READ);
			return; // short read, try again
		}

		ByteBuffer remainBuffer = getPushBackBuffer(key);
		setPushBackBuffer(key, null);

		if (remainBuffer != null) {
			logger.finest(String.format("merge %d bytes from back buffer", remainBuffer.capacity()));
			count += remainBuffer.capacity();
			logger.finest(String.format("total bytes read: %d", count));
			data = new byte[count];
			arraycopy(remainBuffer.array(), 0, data, 0, remainBuffer.capacity());
			arraycopy(incomingBuffer.array(), 0, data, remainBuffer.capacity(), incomingBuffer.position());
		} else {
			data = new byte[count];
			arraycopy(incomingBuffer.array(), 0, data, 0, incomingBuffer.position());
		}

		handleIncomingData(key, data);

		key.interestOps(OP_READ);
	}

	protected final void write(SelectionKey key) throws IOException {
		ByteBuffer buffer;

		while ((buffer = getWriteBuffer(key).peek()) != null) {
			SocketChannel channel = (SocketChannel) key.channel();

			while (buffer.hasRemaining()) {

				logger.finest(String.format("about to write %d bytes", buffer.remaining()));
				int count = channel.write(buffer);
				logger.finest(String.format("wrote %d bytes", count));
				
				if (count==-1) {
					throw new IOException("short write");
				}
				if (count == 0) {
					logger.finest("could not write yet, sleeping...");
					key.interestOps(OP_WRITE);
					return;
				}
			}
			getWriteBuffer(key).removeFirst();
		}
		key.interestOps(OP_READ);
	}

	protected final int writeToGroup(SocketServerMulticastGroup role, ByteBuffer buffer) {
		int clients = 0;
		for (SelectionKey key : selector.keys()) {
			if (role == getMulticastGroup(key)) {
				addWriteBuffer(key, buffer.duplicate());
				clients++;
				try {
					write(key);
				} catch (IOException e) {
					logger.log(Level.WARNING, "write to group", e);
					// do not let the exception escape to top-level
					// as this would cancel the wrong key
					cancelKey(key);
					clients--;
				}
			}
		}
		return clients;
	}

	protected final void writeTo(SelectionKey key, ByteBuffer buffer) throws IOException {
		addWriteBuffer(key, buffer);
		write(key);
	}

	protected final void close(Closeable... closeables) {
		try {
			for (Closeable closeable : closeables)
				closeable.close();
		} catch (IOException e) {
			//
		}
	}

	protected abstract AbstractSelectableChannel channel(InetSocketAddress address) throws IOException;

	protected abstract SelectionKey channelRegister(Selector selector) throws IOException;

	protected abstract void handleIncomingData(SelectionKey sender, byte[] data) throws IOException;

	protected abstract Selector openSelector() throws IOException;

	protected abstract void cancelKey(SelectionKey key);

}
