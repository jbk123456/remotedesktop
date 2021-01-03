package com.github.remotedesktop.socketserver;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.logging.Logger;

public final class Attachment {
	static final Logger LOGGER = Logger.getLogger(HttpServer.class.getName());

	public Group role;
	public LinkedList<ByteBuffer> writeBuffer = new LinkedList<>();
	public ByteBuffer pushBackBuffer;
	public String context;

	public String toString() {
		return "role: " + String.valueOf(role) + ", context: " + String.valueOf(context) + ", data: "
				+ new String((writeBuffer.isEmpty()) ? new byte[0] : writeBuffer.get(0).array());
	}

	public static LinkedList<ByteBuffer> getWriteBuffer(SelectionKey key) {
		return ((Attachment) key.attachment()).writeBuffer;
	}

	public static void addWriteBuffer(SelectionKey key, ByteBuffer data) {
		((Attachment) key.attachment()).writeBuffer.add(data);
	}

	public static ByteBuffer getPushBackBuffer(SelectionKey key) {
		return ((Attachment) key.attachment()).pushBackBuffer;
	}

	public static void setPushBackBuffer(SelectionKey key, ByteBuffer data) {
		if (data!=null) {
		LOGGER.finest(String.format("will push back len %d data to push back buffer (%d)", data.remaining(), data.capacity()));
		}
		((Attachment) key.attachment()).pushBackBuffer = data;
	}

	public static Group getMulticastGroup(SelectionKey key) {
		return ((Attachment) key.attachment()).role;
	}

	public static void setMulticastGroup(SelectionKey key, Group role) {
		((Attachment) key.attachment()).role = role;
	}

	public static String getDebugContext(SelectionKey key) {
		return ((Attachment) key.attachment()).context;
	}

	public static void setDebugContext(SelectionKey key, String context) {
		((Attachment) key.attachment()).context = context;
	}

}