package com.github.remotedesktop.socketserver.http;

import java.awt.AWTException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.ResponseHandler;
import com.github.remotedesktop.socketserver.client.DisplayServer;

public class HttpClientTestAdapter extends DisplayServer {
	private static final Logger logger = Logger.getLogger(HttpClientTestAdapter.class.getName());

	public HttpClientTestAdapter(String id, String hostname, int port, ResponseHandler handler)
			throws IOException, AWTException {
		super(id, hostname, port);
		setResponseHandler(handler);
	}

	public void sendMessage(String message) throws IOException {
		sendMessage(message.getBytes());
	}

	public void sendMessage(byte[] data) throws IOException {
		logger.finest("client " + id + " send data length: " + data.length);
		writeToServer(ByteBuffer.wrap(data));
	}

	@Override
	protected void handleIncomingData(SelectionKey sender, byte[] data) throws IOException {
		logger.finest("client " + id + " received data length: " + data.length);
		handler.onMessage(sender, data);

	}

}
