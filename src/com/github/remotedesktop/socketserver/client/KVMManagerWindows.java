package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.awt.image.BufferedImage;

import com.github.remotedesktop.socketserver.client.jna.WindowCapture;

public class KVMManagerWindows extends KVMManagerJava {

	private WindowCapture cap;

	protected KVMManagerWindows() throws AWTException {
		cap = new WindowCapture();
	}

	protected BufferedImage createScreenCapture() {
		return cap.getImage();
	}

	public String getPointer() {
		return cap.getPointer();
	}

	public void keepScreenOn(boolean toggle) {
		if (toggle) {
			cap.keepScreenOn(toggle);
		}
	}
}
