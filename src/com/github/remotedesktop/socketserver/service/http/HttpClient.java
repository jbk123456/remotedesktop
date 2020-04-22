package com.github.remotedesktop.socketserver.service.http;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.LinkedList;

import com.github.remotedesktop.socketserver.SocketServer;

public class HttpClient extends SocketServer {
	private ResponseHandler handler;
	private final LinkedList<ByteBuffer> messages;

	public interface ResponseHandler {
		void onMessage(SelectionKey key, byte[] message) throws IOException;
	}

	public HttpClient(String id, String hostname, int port) throws IOException {
		super(id, new InetSocketAddress(hostname, port));
		this.messages = new LinkedList<>();
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
	
	@Override
	protected void write(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		while (!messages.isEmpty()) {
			ByteBuffer message = messages.poll();
			while (message.hasRemaining()) {
				channel.write(message);
			}
		}
		key.interestOps(OP_READ);
	}

	public void writeToServer(ByteBuffer buffer) throws IOException {
		messages.add(buffer);
		SelectionKey key = channel.keyFor(selector);
		
		if (key==null) { // force reconnect
			close(selector);
		}
		
		if (key.isValid()) {
			key.interestOps(OP_WRITE);
		}
	}

}
