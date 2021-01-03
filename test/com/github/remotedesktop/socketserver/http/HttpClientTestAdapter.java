package com.github.remotedesktop.socketserver.http;

import java.awt.AWTException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;
import com.github.remotedesktop.socketserver.client.DisplayServer;

public class HttpClientTestAdapter extends DisplayServer {
	static final Logger logger = Logger.getLogger(HttpClientTestAdapter.class.getName());

	private ResponseHandler handler;
	
	public HttpClientTestAdapter(String id, String hostname, int port, ResponseHandler handler)
			throws IOException, AWTException {
		super();
		this.handler = handler;
	}

	public HttpClientTestAdapter(String id, String hostname, int port) throws IOException, AWTException {
		this(id, hostname, port, null);
	}

	public void setResponseHandler(ResponseHandler handler) {
		this.handler = handler;
	}

	public void sendMessage(String message) throws IOException {
		sendMessage(message.getBytes());
	}

	public void sendMessage(byte[] data) throws IOException {
		logger.finest("client " + id + " send data length: " + data.length);
		writeToServer(ByteBuffer.wrap(data));
	}

	// Take the assembled request and pretend thad raw data has been received
	@Override
	public boolean service(Request req, Response res) throws IOException {
		//FIXME
		byte[] data = req.getData();
		SelectionKey key = req.getKey();	
		logger.finest("client " + id + " received data length: " + data.length);
		handler.onMessage(key, data);
		return true;
	}
}
