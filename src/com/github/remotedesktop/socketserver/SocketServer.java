package com.github.remotedesktop.socketserver;

import static java.lang.System.arraycopy;
import static java.lang.Thread.State.NEW;
import static java.nio.channels.SelectionKey.OP_READ;

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

public abstract class SocketServer implements Runnable {
	public static final int BUFFER_SIZE = 1 * 1024 * 1024;

	protected final String id;
	protected final ByteBuffer buffer;
	protected Selector selector;
	protected AbstractSelectableChannel channel;
	protected final InetSocketAddress address;
	private volatile boolean running;

	protected enum MulticastGroup {
		SENDER, RECEIVER;
	}

	private class Attachment {
		public MulticastGroup role;
//		public byte[] debugData;
		public ByteBuffer data;
		public String context;

		public String toString() {
			return "role: " + String.valueOf(role) + ", context: " + String.valueOf(context) + ", data: "
					+ new String(data == null ? new byte[0] : data.array())
//					+" debugData: " + new String(debugData)
			;
		}
	}

	public SocketServer(String id, String addr, int port) throws IOException {
		this(id, addr == null ? new InetSocketAddress(port) : new InetSocketAddress(InetAddress.getByName(addr), port));
	}

	public SocketServer(String id, InetSocketAddress address) throws IOException {
		this.id = id;
		this.address = address;
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.channel = channel(address);
		this.selector = selector();
	}

	protected abstract AbstractSelectableChannel channel(InetSocketAddress address) throws IOException;

	protected abstract void handleIncomingData(SelectionKey sender, byte[] data) throws IOException;

	protected abstract Selector openSelector() throws IOException;

	protected abstract SelectionKey channelRegister(Selector selector) throws IOException;

	protected abstract void select() throws IOException;

	protected abstract void cancelKey(SelectionKey key);

	private final Selector selector() throws IOException {
		Selector selector = openSelector();
		registerKeyForAttachments(channelRegister(selector));
		return selector;
	}

	private void registerKeyForAttachments(SelectionKey key) {
		key.attach(new Attachment());
	}

	protected void write(SelectionKey key) throws IOException {
		ByteBuffer buffer = getDataBuffer(key);
		setDataBuffer(key, null);

		SocketChannel channel = (SocketChannel) key.channel();
		assert (buffer != null);

		while (buffer.hasRemaining()) {
			channel.write(buffer);
		}
		key.interestOps(OP_READ);
	}

	protected void writeTo(SelectionKey key, ByteBuffer buffer) throws IOException {
		setDataBuffer(key, buffer);
		write(key);
	}

	protected void writeToGroup(MulticastGroup role, ByteBuffer buffer) throws IOException {
		int clients = 0;
		buffer.mark();
		for (SelectionKey key : selector.keys()) {
			if (role == ((Attachment) (key.attachment())).role) {
				buffer.reset();
				SocketChannel channel = (SocketChannel) key.channel();
				clients++;
				try {
					while (buffer.hasRemaining()) {
						channel.write(buffer);
					}
				} catch (IOException e) {
					// do not let the exception escape to top-level
					cancelKey(key);
				}
			}
		}
		if (clients == 0) {
			System.out.println("write to group: " + role + " nobody cares");
		}
	}

	public synchronized void start() {
		Thread executionThread = new Thread(this, id);
		running = true;
		executionThread.start();
		while (executionThread.getState() == NEW)
			;
	}

	public void stop() throws IOException {
		running = false;
	}

	@Override
	public void run() {
		while (running) {
			runMainLoop();
		}
		cleanUp();
	}

	private void cleanUp() {
		for (SelectionKey key : selector.keys()) {
			close(key.channel());
		}
		close(selector);
	}

	private void runMainLoop() {
		try {
			select();

			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			// System.out.println("main oop key length:::" + selector.keys().size());

			while (iterator.hasNext()) {
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
					e.printStackTrace();
					cancelKey(key);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
				ee.printStackTrace();
			}
		}
	}

	protected void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		registerKeyForAttachments(socketChannel.register(selector, OP_READ));
	}

	protected void connect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		boolean connected = channel.finishConnect();
		if (!connected) {
			System.out.println("not yet connected");
		}
		channel.configureBlocking(false);
		registerKeyForAttachments(channel.register(selector, OP_READ));
	}

	protected void read(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		buffer.clear();
		int c = 0;

		do {
			c = channel.read(buffer);
		} while (c > 0);

		if (c == -1) {
			throw new IOException("disconnected");
		}
		int count = buffer.position();
		if (count == 0) {
			return; // short read, try again
		}
		byte[] data;
		ByteBuffer remainBuffer = getDataBuffer(key);
		setDataBuffer(key, null);

		if (remainBuffer != null) {
			count += remainBuffer.capacity();
			data = new byte[count];
			arraycopy(remainBuffer.array(), 0, data, 0, remainBuffer.capacity());
			arraycopy(buffer.array(), 0, data, remainBuffer.capacity(), buffer.position());
		} else {
			data = new byte[count];
			arraycopy(buffer.array(), 0, data, 0, buffer.position());
		}
		handleIncomingData(key, data);
	}

	protected ByteBuffer getDataBuffer(SelectionKey key) {
		return ((Attachment) key.attachment()).data;
	}

	protected void setDataBuffer(SelectionKey key, ByteBuffer data) {
		((Attachment) key.attachment()).data = data;
	}

	protected MulticastGroup getMulticastGroup(SelectionKey key) {
		return ((Attachment) key.attachment()).role;
	}

	protected void setMulticastGroup(SelectionKey key, MulticastGroup role) {
		((Attachment) key.attachment()).role = role;
	}

	protected String getContext(SelectionKey key) {
		return ((Attachment) key.attachment()).context;
	}

	protected void setDebugContext(SelectionKey key, String context) {
		((Attachment) key.attachment()).context = context;
	}

//	protected byte[] getDebugDatat(SelectionKey key) {
//		return ((Attachment) key.attachment()).debugData;
//	}
//
//	protected void setDebugDatat(SelectionKey key, byte[] data) {
//		((Attachment) key.attachment()).debugData = data;
//	}

	protected void close(Closeable... closeables) {
		try {
			for (Closeable closeable : closeables)
				closeable.close();
		} catch (IOException e) {
			//
		}
	}
}
