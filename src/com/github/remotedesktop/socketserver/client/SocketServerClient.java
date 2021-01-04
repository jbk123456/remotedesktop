package com.github.remotedesktop.socketserver.client;

import static com.github.remotedesktop.socketserver.Attachment.addWriteBuffer;
import static java.nio.channels.SelectionKey.OP_CONNECT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.SocketServer;

public class SocketServerClient extends SocketServer {
	static final Logger LOGGER = Logger.getLogger(SocketServerClient.class.getName());

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
		return getChannel().register(selector, OP_CONNECT);
	}

	public void writeToServerBuffer(ByteBuffer buffer) {
		SelectionKey key = getChannel().keyFor(selector);
		addWriteBuffer(key, buffer);
	}

	public void writeToServer(ByteBuffer buffer) throws IOException {
		SelectionKey key = getChannel().keyFor(selector);

		addWriteBuffer(key, buffer);
		write(key);
	}

	@Override
	protected void cancelKey(SelectionKey key) {
		LOGGER.info(String.format("HttpClient: cancel key: %s (%s)", key, key.attachment()));

		key.cancel();
		close(key.channel());
		throw new IllegalStateException("lost connection to server: " + key.attachment()); // try again
	}
}
