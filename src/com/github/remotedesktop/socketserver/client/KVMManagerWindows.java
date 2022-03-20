package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

import com.github.remotedesktop.socketserver.client.jna.WindowCapture;

public class KVMManagerWindows extends KVMManagerJava {

	private WindowCapture cap;

	protected KVMManagerWindows() throws AWTException {
		cap = new WindowCapture();
	}

	@Override
	protected BufferedImage createScreenCapture(Rectangle screenbound) {
		return cap.getImage();
	}

	@Override
	public String getPointer() {
		return cap.getPointer();
	}
	@Override
	public void keepScreenOn(boolean toggle) {
		if (toggle) {
			cap.keepScreenOn(toggle);
		}
	}
}
