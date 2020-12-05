package com.github.remotedesktop.socketserver.http;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.SocketServerClient;

public class HttpClientTestAdapter extends SocketServerClient {

	public HttpClientTestAdapter(String id, String hostname, int port, ResponseHandler handler) throws IOException {
		super(id, hostname, port);
		setResponseHandler(handler);
	}
	public void sendMessage(String message) throws IOException {
		sendMessage(message.getBytes());
	}

	public void sendMessage(byte[] data) throws IOException {
		writeToServer(ByteBuffer.wrap(data));
	}

}
