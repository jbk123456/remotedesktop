package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

public class KVMManagerJava extends KVMManager {
	static final Logger LOGGER = Logger.getLogger(KVMManagerJava.class.getName());

	private Robot robot;

	protected KVMManagerJava() throws AWTException {
		super();
		robot = new java.awt.Robot();
		robot.setAutoWaitForIdle(true);
	}

	@Override
	protected void sendKeyRelease(int scancode) {
		robot.keyRelease(scancode);
	}

	@Override
	protected void sendKeyPress(int scancode) {
		robot.keyPress(scancode);
	}

	@Override
	protected void sendMouseMove(int x, int y) {
		robot.mouseMove(x, y);
	}

	@Override
	protected void sendMousePress(int buttons) {
		int mask = 0;
		if ((buttons & 1) != 0)
			mask |= InputEvent.BUTTON1_DOWN_MASK;
		if ((buttons & 2) != 0)
			mask |= InputEvent.BUTTON3_DOWN_MASK;
		if ((buttons & 4) != 0)
			mask |= InputEvent.BUTTON2_DOWN_MASK;
		robot.mousePress(mask);
	}

	@Override
	protected void sendMouseRelease(int buttons) {
		int mask = 0;
		if ((buttons & 1) != 0)
			mask |= InputEvent.BUTTON1_DOWN_MASK;
		if ((buttons & 2) != 0)
			mask |= InputEvent.BUTTON3_DOWN_MASK;
		if ((buttons & 4) != 0)
			mask |= InputEvent.BUTTON2_DOWN_MASK;
		robot.mouseRelease(mask);
	}

	@Override
	public String getPointer() {
		return "default";
	}

	@Override
	public void keepScreenOn(boolean toggle) {
		if (toggle) {
			LOGGER.finer("keep linux screen on by pressing num lock");
			sendKeyPress(KeyEvent.VK_NUM_LOCK); // keep screen on
		} else {
			LOGGER.finer("keep linux screen on by releasing num lock");
			sendKeyRelease(KeyEvent.VK_NUM_LOCK); // keep screen on
		}
	}

	@Override
	protected BufferedImage createScreenCapture(Rectangle screenbound) {
		return robot.createScreenCapture(screenbound);
	}
}
