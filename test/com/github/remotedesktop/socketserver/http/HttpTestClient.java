package com.github.remotedesktop.socketserver.http;

import static java.nio.channels.SelectionKey.OP_CONNECT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.SocketServer;

public class HttpTestClient extends SocketServer {
	private final ResponseHandler handler;

	public HttpTestClient(String id, String hostname, int port, ResponseHandler handler) throws IOException {
		super(id, new InetSocketAddress(hostname, port));
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
	protected Selector openSelector()  throws IOException {
		return Selector.open();
	}

	@Override
	protected SelectionKey channelRegister(Selector selector)  throws IOException {
		return channel.register(selector, OP_CONNECT);
	}

	@Override
	protected void handleIncomingData(SelectionKey sender, byte[] data) {
		handler.onMessage(new String(data));
	}

	public void sendMessage(String message) throws IOException {
		byte[] data = message.getBytes();
		SelectionKey key = channel.keyFor(selector);
		setDataBuffer(key, ByteBuffer.wrap(data));
		write(key);
	}

	@Override
	protected void cancelKey(SelectionKey key) {
		key.cancel();
		
	}

	@Override
	protected void select() throws IOException {
		selector.select();
	}

//	public static void main(String[] args) throws Throwable {
//		ChatServer server = new ChatServer(args[0], Integer.parseInt(args[1]));
//		server.start();
//		System.out.println("server running on port: " + server.getPort());
//	}

}
