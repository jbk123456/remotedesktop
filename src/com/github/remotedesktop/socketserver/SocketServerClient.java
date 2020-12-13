package com.github.remotedesktop.socketserver;

import static com.github.remotedesktop.socketserver.SocketServerAttachment.addWriteBuffer;
import static java.nio.channels.SelectionKey.OP_CONNECT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.logging.Logger;

public class SocketServerClient extends SocketServer {
	private static final Logger logger = Logger.getLogger(SocketServerClient.class.getName());
	protected ResponseHandler handler;

	public SocketServerClient(String id, String hostname, int port) throws IOException {
		super(id, new InetSocketAddress(hostname, port));
	}

	public void setResponseHandler(ResponseHandler handler) {
		this.handler = handler;
	}

	@Override
	protected AbstractSelectableChannel channel(InetSocketAddress address) throws IOException {
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(address);
		return channel;
	}

	@Override
	protected Selector openSelector() throws IOException {
		return Selector.open();
	}

	@Override
	protected SelectionKey channelRegister(Selector selector) throws IOException {
		return channel.register(selector, OP_CONNECT);
	}

	@Override
	protected void handleIncomingData(SelectionKey sender, byte[] data) throws IOException {
		handler.onMessage(sender, data);
	}

	public void writeToServerBuffer(ByteBuffer buffer) {
		SelectionKey key = channel.keyFor(selector);
		addWriteBuffer(key, buffer);
	}

	public void writeToServer(ByteBuffer buffer) throws IOException {
		SelectionKey key = channel.keyFor(selector);

		addWriteBuffer(key, buffer);
		write(key);
	}

	@Override
	protected void cancelKey(SelectionKey key) {
		logger.fine("HttpClient: cancel key for: " + key.channel() + " " + key.attachment());

		key.cancel();
		close(key.channel());
		throw new IllegalStateException("lost connection to server: " + key.attachment()); // try again
	}
}
