package com.github.remotedesktop.socketserver.service;

import java.io.IOException;

public class KeyboardAndMouseSerializer {

	private final StringBuilder b;

	public KeyboardAndMouseSerializer() throws IOException {
		b = new StringBuilder();
	}
	public void reset() {
		b.setLength(0);
	}
	public byte[] getBytes() throws IOException {
		return b.toString().getBytes();
	}

	private void writekeyStroke(int scancode, int mask) throws IOException {
		b.append("GET /k");
		b.append("?");
		b.append("v=");
		b.append(scancode);
		b.append("&mask=");
		b.append(mask);
		b.append("\r\n\r\n");
	}

	public void keyStroke(int keycode, int mask) throws IOException {
		writekeyStroke(keycode, mask);
	}

	public void mouseMove(int x, int y) throws IOException {
		writemouseMove(x, y);
	}

	private void writemouseMove(int x, int y) throws IOException {
		b.append("GET /m");
		b.append("?");
		b.append("x=");
		b.append(x);
		b.append("&y=");
		b.append(y);
		b.append("\r\n\r\n");
	}

	public void mousePress(int buttons) throws IOException {
		writemousePress(buttons);
	}

	private void writemousePress(int mask) throws IOException {
		b.append("GET /p");
		b.append("?");
		b.append("v=");
		b.append(mask);
		b.append("\r\n\r\n");

	}

	public void mouseRelease(int buttons) throws IOException {
		writemouseRelease(buttons);
	}

	private void writemouseRelease(int mask) throws IOException {
		b.append("GET /r");
		b.append("?");
		b.append("v=");
		b.append(mask);
		b.append("\r\n\r\n");

	}

	public void mouseStroke(int buttons) throws IOException {
		mousePress(buttons);
		mouseRelease(buttons);
	}

}
