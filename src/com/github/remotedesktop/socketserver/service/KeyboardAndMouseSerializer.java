package com.github.remotedesktop.socketserver.service;

import java.io.IOException;

public class KeyboardAndMouseSerializer {

	private final StringBuilder b;
	private String command;
	private String separator;

	public KeyboardAndMouseSerializer(String command, String separator) throws IOException {
		b = new StringBuilder();
		this.command = command;
		this.separator = separator;
	}

	public byte[] getBytes() throws IOException {
		byte[] result = b.toString().getBytes();
		b.setLength(0);
		return result;
	}

	public void keyStroke(int key, int scancode, int mask) throws IOException {
		b.append(command);
		b.append("k?");
		b.append("k=");
		b.append(key);
		b.append("&v=");
		b.append(scancode);
		b.append("&mask=");
		b.append(mask);
		b.append(separator);
	}

	public void mouseMove(int x, int y) throws IOException {
		b.append(command);
		b.append("m?");
		b.append("x=");
		b.append(x);
		b.append("&y=");
		b.append(y);
		b.append(separator);
	}

	public void mousePress(int mask) throws IOException {
		b.append(command);
		b.append("p?");
		b.append("v=");
		b.append(mask);
		b.append(separator);

	}

	public void mouseRelease(int mask) throws IOException {
		b.append(command);
		b.append("r?");
		b.append("v=");
		b.append(mask);
		b.append(separator);

	}

	public void mouseStroke(int buttons) throws IOException {
		mousePress(buttons);
		mouseRelease(buttons);
	}

	public void updateConfig(String key, String val) {
		b.append(command);
		b.append("u?");
		b.append("k=");
		b.append(key);
		b.append("&v=");
		b.append(val);
		b.append(separator);
	}

}
