package com.github.remotedesktop.socketserver;

import static com.github.remotedesktop.socketserver.Attachment.addWriteBuffer;
import static com.github.remotedesktop.socketserver.Attachment.getMulticastGroup;
import static com.github.remotedesktop.socketserver.Attachment.getPushBackBuffer;
import static com.github.remotedesktop.socketserver.Attachment.getWriteBuffer;
import static com.github.remotedesktop.socketserver.Attachment.setPushBackBuffer;
import static java.lang.System.arraycopy;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.plugin.base.DataFilterPlugin;
import com.github.remotedesktop.socketserver.plugin.base.ServiceHandlerPlugin;

public abstract class SocketServer implements Runnable {
	static final Logger LOGGER = Logger.getLogger(SocketServer.class.getName());

	public static final int BUFFER_SIZE = 65535; // 8 * buffer cache size

	private Object waitForFinishLock = new Object();
	private Object stopLock = new Object();
	private boolean isFinish = false;
	private boolean running = true;

	protected String id;
	protected ByteBuffer incomingBuffer;
	protected Selector selector;
	protected AbstractSelectableChannel channel;
	protected InetSocketAddress address;

	protected final List<DataFilterPlugin> dataFilters = new ArrayList<>();
	protected final List<ServiceHandlerPlugin> serviceHandlers = new ArrayList<>(
			Collections.singletonList((req, res) -> service(req, res)));

	public SocketServer() {
	}

	protected final void init(String id, String addr, int port) throws IOException {
		init(id, addr == null ? new InetSocketAddress(port) : new InetSocketAddress(InetAddress.getByName(addr), port));
	}

	private void init(String id, InetSocketAddress address) throws IOException {
		this.id = id;
		this.incomingBuffer = ByteBuffer.allocate(getRecvBufferSize());
		this.address = address;
		this.channel = channel(address);
		this.selector = selector();
	}

	protected AbstractSelectableChannel getChannel() {
		return channel;
	}

	protected SelectableChannel getChannel(SelectionKey key) {
		return key.channel();
	}

	private final Selector selector() throws IOException {
		Selector selector = openSelector();
		enableAttachments(channelRegister(selector));
		return selector;
	}

	protected void enableAttachments(SelectionKey key) {
		key.attach(new Attachment());
	}

	public void start() {
		new Thread(this, id).start();
	}

	public void stop() {
		synchronized (stopLock) {
			if (running) {
				LOGGER.info("socket server stop called");
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
			LOGGER.log(Level.SEVERE, "socket server terminated", t);
		} finally {
			LOGGER.info("stopping socket server");
			running = false;
			cleanUp();
			synchronized (waitForFinishLock) {
				LOGGER.info("stopped socket server");
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

	protected void cleanUp() {
		for (SelectionKey key : selector.keys()) {
			close(getChannel(key));
		}
		close(selector);
		if (getChannel().isOpen()) {
			close(getChannel());
		}
	}

	protected int getRecvBufferSize() {
		return BUFFER_SIZE;
	}

	private void runMainLoop() {
		try {
			select();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "socket server terminated", e);
			try {
				Thread.sleep(10000);
				if (selector.isOpen())
					close(selector);
				if (getChannel().isOpen())
					close(getChannel());

				// try again
				this.channel = channel(address);
				this.selector = selector();
			} catch (Exception ee) {
				LOGGER.log(Level.SEVERE, "fatal error", ee);
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
				LOGGER.log(Level.SEVERE, "select", e);
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
			LOGGER.fine("not yet connected");
		}
		channel.configureBlocking(false);
		enableAttachments(channel.register(selector, OP_READ));
	}

	protected void read(SelectionKey key) throws IOException {
		byte[] data;
		int c = 0;
		SocketChannel channel = (SocketChannel) getChannel(key);
		incomingBuffer.clear();

		do {
			c = channel.read(incomingBuffer);
		} while (c > 0);

		LOGGER.finest(String.format("read %d bytes", incomingBuffer.position()));
		if (c == -1) {
			LOGGER.log(Level.WARNING, "cannot read data, connection lost");
			throw new IOException("disconnected");
		}
		int count = incomingBuffer.position();
		if (count == 0) {
			LOGGER.fine("could not read data yet, sleeping...");
			key.interestOps(OP_READ);
			return; // short read, try again
		}

		ByteBuffer remainBuffer = getPushBackBuffer(key);
		setPushBackBuffer(key, null);

		if (remainBuffer != null) {
			LOGGER.finest(String.format("merge %d bytes from back buffer", remainBuffer.capacity()));
			count += remainBuffer.capacity();
			LOGGER.finest(String.format("total bytes read: %d", count));
			data = new byte[count];
			arraycopy(remainBuffer.array(), 0, data, 0, remainBuffer.capacity());
			arraycopy(incomingBuffer.array(), 0, data, remainBuffer.capacity(), incomingBuffer.position());
		} else {
			data = new byte[count];
			arraycopy(incomingBuffer.array(), 0, data, 0, incomingBuffer.position());
		}

		try {
			handleIncomingData(key, data);
			key.interestOps(OP_READ);
		} catch (IOException ex) {
			throw ex;
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "read handler failed", e);
			cancelKey(key);
		}
	}

	protected void handleIncomingData(SelectionKey key, byte[] data) throws IOException {

		byte[] filteredData = data;
		for (DataFilterPlugin p : dataFilters) {
			LOGGER.finest(String.format("running DataFilterPlugin: %s", p.getClass().getName()));
			if ((filteredData = p.filter(key, filteredData)) == null) {
				return;
			}
		}

		onMessages(key, filteredData);
	}

	private void onMessages(SelectionKey key, byte[] data) throws IOException {
		Request req = null;
		int remaining = 0;
		do {
			if (remaining > 0) {

				byte[] newdata = new byte[remaining];
				System.arraycopy(data, data.length - remaining, newdata, 0, remaining);
				data = newdata;
			}

			req = new Request(key);
			remaining = req.parse(data);
			if (remaining < 0) {
				LOGGER.fine("could not read data yet, sleeping...");
				setPushBackBuffer(key, ByteBuffer.wrap(data));
				key.interestOps(OP_READ);
				return; // partial read, get the rest
			}
			Response res = new Response();
			for (ServiceHandlerPlugin p : serviceHandlers) {
				if (p.service(req, res)) {
					LOGGER.finest(String.format("running ServiceHandlerPlugin: %s", p.getClass().getName()));
					break;
				}
			}
			if (res.isReady()) {
				writeTo(key, ByteBuffer.wrap(res.getResponse()));
			}
		} while (remaining > 0);

	}

	public final void write(SelectionKey key) throws IOException {
		ByteBuffer buffer;
		while ((buffer = getWriteBuffer(key).peek()) != null) {
			SocketChannel channel = (SocketChannel) getChannel(key);

			while (buffer.hasRemaining()) {

				LOGGER.finest(String.format("about to write %d bytes", buffer.remaining()));
				int count = channel.write(buffer);
				LOGGER.finest(String.format("wrote %d bytes", count));

				if (count == -1) {
					throw new IOException("short write");
				}
				if (count == 0) {
					LOGGER.fine("could not write yet, sleeping...");
					key.interestOps(OP_WRITE);
					return;
				}
			}
			getWriteBuffer(key).removeFirst();
		}
		key.interestOps(OP_READ);
	}

	public final void writeToGroup(Group role, ByteBuffer buffer) {
		int clients = 0;
		for (SelectionKey key : selector.keys()) {
			if (role == getMulticastGroup(key)) {
				addWriteBuffer(key, buffer.duplicate());
				clients++;
				try {
					write(key);
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "write to group", e);
					// do not let the exception escape to top-level
					// as this would cancel the wrong key
					cancelKey(key);
				}
			}
		}
		if (clients == 0) {
			LOGGER.info("write to group: " + role + " nobody cares");
		}
	}

	public final void writeTo(SelectionKey key, ByteBuffer buffer) throws IOException {
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

	private boolean service(Request req, Response res) {
		return false;
	}

	protected abstract AbstractSelectableChannel channel(InetSocketAddress address) throws IOException;

	protected abstract SelectionKey channelRegister(Selector selector) throws IOException;

	protected abstract Selector openSelector() throws IOException;

	protected abstract void cancelKey(SelectionKey key);

}
