package com.github.remotedesktop.socketserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.service.http.HttpServer;

public final class SocketServerAttachment {
	private static final Logger logger = Logger.getLogger(HttpServer.class.getName());

	public SocketServerMulticastGroup role;
	public LinkedList<ByteBuffer> writeBuffer = new LinkedList<>();
	public ByteBuffer pushBackBuffer;
	public String context;

	public String toString() {
		return "role: " + String.valueOf(role) + ", context: " + String.valueOf(context) + ", data: "
				+ new String((writeBuffer.isEmpty()) ? new byte[0] : writeBuffer.get(0).array());
	}

	public static LinkedList<ByteBuffer> getWriteBuffer(SelectionKey key) {
		return ((SocketServerAttachment) key.attachment()).writeBuffer;
	}

	public static void addWriteBuffer(SelectionKey key, ByteBuffer data) {
		((SocketServerAttachment) key.attachment()).writeBuffer.add(data);
	}

	public static ByteBuffer getPushBackBuffer(SelectionKey key) {
		return ((SocketServerAttachment) key.attachment()).pushBackBuffer;
	}

	public static void setPushBackBuffer(SelectionKey key, ByteBuffer data) {
		if (data!=null) {
		logger.finest(String.format("will push back len %d data to push back buffer (%d)", data.remaining(), data.capacity()));
		}
		((SocketServerAttachment) key.attachment()).pushBackBuffer = data;
	}

	public static SocketServerMulticastGroup getMulticastGroup(SelectionKey key) {
		return ((SocketServerAttachment) key.attachment()).role;
	}

	public static void setMulticastGroup(SelectionKey key, SocketServerMulticastGroup role) {
		((SocketServerAttachment) key.attachment()).role = role;
	}

	public static String getDebugContext(SelectionKey key) {
		return ((SocketServerAttachment) key.attachment()).context;
	}

	public static void setDebugContext(SelectionKey key, String context) {
		((SocketServerAttachment) key.attachment()).context = context;
	}

}