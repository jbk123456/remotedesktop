package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.github.remotedesktop.socketserver.client.jna.WindowCapture;

public class KVMManager {

	private Map<Integer, Integer> keymap;
	private WindowCapture cap;
	private Robot robot;
	private Rectangle screenbound;
	private long t0 = 0;

	public KVMManager() throws AWTException {
		robot = new java.awt.Robot();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		screenbound = gd.getDefaultConfiguration().getBounds();
		keymap = new HashMap<>();
		assignKeyMap();
		robot.setAutoWaitForIdle(true);

		try {
			cap = new WindowCapture();
		} catch (Throwable e) {
			// ignore
		}
	}

	private void assignKeyMap() {
		keymap.put(new Integer(16), new Integer(-2)); // KeyEvent.VK_SHIFT Shift
		keymap.put(new Integer(17), new Integer(-2)); // KeyEvent.VK_CONTROL Ctrl
		keymap.put(new Integer(18), new Integer(-2)); // KeyEvent.VK_ALT Alt
		keymap.put(new Integer(225), new Integer(-2)); // KeyEvent.VK_ALT_GRAPH AltGr

		keymap.put(new Integer(173), new Integer(KeyEvent.VK_MINUS)); // -

		keymap.put(new Integer(27), new Integer(KeyEvent.VK_ESCAPE)); // Esc
//		keymap.put(new Integer(192), new Integer(KeyEvent.VK_BACK_QUOTE)); // `
		keymap.put(new Integer(49), new Integer(KeyEvent.VK_1)); // 1
		keymap.put(new Integer(50), new Integer(KeyEvent.VK_2)); // 2
		keymap.put(new Integer(51), new Integer(KeyEvent.VK_3)); // 3
		keymap.put(new Integer(52), new Integer(KeyEvent.VK_4)); // 4
		keymap.put(new Integer(53), new Integer(KeyEvent.VK_5)); // 5
		keymap.put(new Integer(54), new Integer(KeyEvent.VK_6)); // 6
//		keymap.put(new Integer(55), new Integer(KeyEvent.VK_7)); // 7
//		keymap.put(new Integer(56), new Integer(KeyEvent.VK_8)); // 8
//		keymap.put(new Integer(57), new Integer(KeyEvent.VK_9)); // 9
//		keymap.put(new Integer(48), new Integer(KeyEvent.VK_0)); // 0
		keymap.put(new Integer(189), new Integer(KeyEvent.VK_MINUS)); // -
		keymap.put(new Integer(187), new Integer(KeyEvent.VK_EQUALS)); // =
		keymap.put(new Integer(8), new Integer(KeyEvent.VK_BACK_SPACE)); // Backspace

		keymap.put(new Integer(9), new Integer(KeyEvent.VK_TAB)); // Tab
//		keymap.put(new Integer(81), new Integer(KeyEvent.VK_Q)); // Q
		keymap.put(new Integer(87), new Integer(KeyEvent.VK_W)); // W
		keymap.put(new Integer(69), new Integer(KeyEvent.VK_E)); // E
		keymap.put(new Integer(82), new Integer(KeyEvent.VK_R)); // R
		keymap.put(new Integer(84), new Integer(KeyEvent.VK_T)); // T
		keymap.put(new Integer(89), new Integer(KeyEvent.VK_Y)); // Y
		keymap.put(new Integer(85), new Integer(KeyEvent.VK_U)); // U
		keymap.put(new Integer(73), new Integer(KeyEvent.VK_I)); // I
		keymap.put(new Integer(79), new Integer(KeyEvent.VK_O)); // O
		keymap.put(new Integer(80), new Integer(KeyEvent.VK_P)); // P
//		keymap.put(new Integer(219), new Integer(KeyEvent.VK_OPEN_BRACKET)); // [
//		keymap.put(new Integer(221), new Integer(KeyEvent.VK_CLOSE_BRACKET)); // ]
//		keymap.put(new Integer(220), new Integer(KeyEvent.VK_BACK_SLASH)); // \
		keymap.put(new Integer(65), new Integer(KeyEvent.VK_A)); // A
		keymap.put(new Integer(83), new Integer(KeyEvent.VK_S)); // S
		keymap.put(new Integer(68), new Integer(KeyEvent.VK_D)); // D
		keymap.put(new Integer(70), new Integer(KeyEvent.VK_F)); // F
		keymap.put(new Integer(71), new Integer(KeyEvent.VK_G)); // G
		keymap.put(new Integer(72), new Integer(KeyEvent.VK_H)); // H
		keymap.put(new Integer(74), new Integer(KeyEvent.VK_J)); // J
		keymap.put(new Integer(75), new Integer(KeyEvent.VK_K)); // K
		keymap.put(new Integer(76), new Integer(KeyEvent.VK_L)); // L
		keymap.put(new Integer(186), new Integer(KeyEvent.VK_SEMICOLON)); // ;

		keymap.put(new Integer(13), new Integer(KeyEvent.VK_ENTER)); // Enter
		keymap.put(new Integer(90), new Integer(KeyEvent.VK_Z)); // Z
		keymap.put(new Integer(88), new Integer(KeyEvent.VK_X)); // X
		keymap.put(new Integer(67), new Integer(KeyEvent.VK_C)); // C
		keymap.put(new Integer(86), new Integer(KeyEvent.VK_V)); // V
		keymap.put(new Integer(66), new Integer(KeyEvent.VK_B)); // B
		keymap.put(new Integer(78), new Integer(KeyEvent.VK_N)); // N
		keymap.put(new Integer(77), new Integer(KeyEvent.VK_M)); // M
		keymap.put(new Integer(188), new Integer(KeyEvent.VK_COMMA)); // ,
//		keymap.put(new Integer(190), new Integer(KeyEvent.VK_DECIMAL)); // .
		keymap.put(new Integer(191), new Integer(KeyEvent.VK_SLASH)); // /
		keymap.put(new Integer(32), new Integer(KeyEvent.VK_SPACE)); // Space
		keymap.put(new Integer(112), new Integer(KeyEvent.VK_F1)); // F1
		keymap.put(new Integer(113), new Integer(KeyEvent.VK_F2)); // F2
		keymap.put(new Integer(114), new Integer(KeyEvent.VK_F3)); // F3
		keymap.put(new Integer(115), new Integer(KeyEvent.VK_F4)); // F4
		keymap.put(new Integer(116), new Integer(KeyEvent.VK_F5)); // F5
		keymap.put(new Integer(117), new Integer(KeyEvent.VK_F6)); // F6
		keymap.put(new Integer(118), new Integer(KeyEvent.VK_F7)); // F7
		keymap.put(new Integer(119), new Integer(KeyEvent.VK_F8)); // F8
		keymap.put(new Integer(120), new Integer(KeyEvent.VK_F9)); // F9
		keymap.put(new Integer(121), new Integer(KeyEvent.VK_F10)); // F10
		keymap.put(new Integer(122), new Integer(KeyEvent.VK_F11)); // F11
		keymap.put(new Integer(123), new Integer(KeyEvent.VK_F12)); // F12
		keymap.put(new Integer(111), new Integer(KeyEvent.VK_SLASH)); // /
		keymap.put(new Integer(42), new Integer(KeyEvent.VK_ASTERISK)); // *
		keymap.put(new Integer(45), new Integer(KeyEvent.VK_MINUS)); // -
		keymap.put(new Integer(43), new Integer(KeyEvent.VK_PLUS)); // +
		keymap.put(new Integer(46), new Integer(KeyEvent.VK_DELETE)); // Del
		keymap.put(new Integer(13), new Integer(KeyEvent.VK_ENTER)); // Enter
		keymap.put(new Integer(36), new Integer(KeyEvent.VK_HOME)); // Home
		keymap.put(new Integer(38), new Integer(KeyEvent.VK_UP)); // Up
		keymap.put(new Integer(33), new Integer(KeyEvent.VK_PAGE_UP)); // PgUp
		keymap.put(new Integer(37), new Integer(KeyEvent.VK_LEFT)); // Left
		keymap.put(new Integer(39), new Integer(KeyEvent.VK_RIGHT)); // Right
		keymap.put(new Integer(35), new Integer(KeyEvent.VK_END)); // End
		keymap.put(new Integer(40), new Integer(KeyEvent.VK_DOWN)); // Down
		keymap.put(new Integer(34), new Integer(KeyEvent.VK_PAGE_DOWN)); // PgDn
		keymap.put(new Integer(45), new Integer(KeyEvent.VK_INSERT)); // Ins
		keymap.put(new Integer(46), new Integer(KeyEvent.VK_DELETE)); // Del
	}

	private int convAscii(int ascii) {
		Integer scancode;
		scancode = (Integer) keymap.get(new Integer(ascii));
		if (scancode == null)
			return -1;
		else
			return scancode.intValue();
	}

	public void keyStroke(int key, int keycode, int mask) {
		if (keycode == 0) {
			return; // ignore composite keys. We might use e.code, but that would be difficult
		}

		int scancode = convAscii(keycode);
		System.out.println("keycode:::" + keycode + " " + scancode + " " + ((char) key));
		if (scancode < 0) {
			if (scancode < -1) {
				return;
			}
			askiiKeyStrokeViaAltNumpad(key, mask);
			return;
		}
		if ((mask & 1) == 1) {
			robot.keyPress(KeyEvent.VK_SHIFT);
		} else if ((mask & 2) == 2) {
			robot.keyPress(KeyEvent.VK_CONTROL);
		} else if ((mask & 4) == 4) {
			robot.keyPress(KeyEvent.VK_ALT);
		}
		robot.keyPress(scancode);
		robot.keyRelease(scancode);
		if ((mask & 1) == 1) {
			robot.keyRelease(KeyEvent.VK_SHIFT);
		} else if ((mask & 2) == 2) {
			robot.keyRelease(KeyEvent.VK_CONTROL);
		} else if ((mask & 4) == 4) {
			robot.keyRelease(KeyEvent.VK_ALT);
		}
		if (scancode == KeyEvent.VK_ESCAPE) {
			mouseRelease(7);
			robot.keyPress(KeyEvent.VK_ALT);
			robot.keyRelease(KeyEvent.VK_ALT);
			robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyRelease(KeyEvent.VK_SHIFT);
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyRelease(KeyEvent.VK_CONTROL);
		}
	}

	public static final char[] EXTENDED = { 0xFF, 0xAD, 0x9B, 0x9C, 0x00, 0x9D, 0x00, 0x00, 0x00, 0x00, 0xA6, 0xAE,
			0xAA, 0x00, 0x00, 0x00, 0xF8, 0xF1, 0xFD, 0x00, 0x00, 0xE6, 0x00, 0xFA, 0x00, 0x00, 0xA7, 0xAF, 0xAC, 0xAB,
			0x00, 0xA8, 0x00, 0x00, 0x00, 0x00, 0x8E, 0x8F, 0x92, 0x80, 0x00, 0x90, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0xA5, 0x00, 0x00, 0x00, 0x00, 0x99, 0x00, 0x00, 0x00, 0x00, 0x00, 0x9A, 0x00, 0x00, 0xE1, 0x85, 0xA0,
			0x83, 0x00, 0x84, 0x86, 0x91, 0x87, 0x8A, 0x82, 0x88, 0x89, 0x8D, 0xA1, 0x8C, 0x8B, 0x00, 0xA4, 0x95, 0xA2,
			0x93, 0x00, 0x94, 0xF6, 0x00, 0x97, 0xA3, 0x96, 0x81, 0x00, 0x00, 0x98 };

	private void askiiKeyStrokeViaAltNumpad(int key, int mask) {

		if (key >= 0xA0 && key <= 0xFF && EXTENDED[key - 0xA0] > 0) {
			key = EXTENDED[key - 0xA0];
		}

		robot.keyPress(KeyEvent.VK_ALT);
		String charcode = Integer.toString(key);
		for (char ascii_c : charcode.toCharArray()) {
			int ascii_n = Integer.parseInt(String.valueOf(ascii_c)) + 96;
			robot.keyPress(ascii_n);
			robot.keyRelease(ascii_n);
		}

		robot.keyRelease(KeyEvent.VK_ALT);
	}

	public void mouseMove(int x, int y) {
		System.out.println("mouse move:" + x + " " + y);
		if (x > 0 && y > 0) {
			robot.mouseMove(x, y);
		}
	}

	public void mousePress(int buttons) {
		int mask = 0;
		if ((buttons & 1) != 0)
			mask |= InputEvent.BUTTON1_DOWN_MASK;
		if ((buttons & 2) != 0)
			mask |= InputEvent.BUTTON3_DOWN_MASK;
		if ((buttons & 4) != 0)
			mask |= InputEvent.BUTTON2_DOWN_MASK;
		System.out.println("mouse press:" + mask);
		robot.mousePress(mask);
	}

	public void mouseRelease(int buttons) {
		int mask = 0;
		if ((buttons & 1) != 0)
			mask |= InputEvent.BUTTON1_DOWN_MASK;
		if ((buttons & 2) != 0)
			mask |= InputEvent.BUTTON3_DOWN_MASK;
		if ((buttons & 4) != 0)
			mask |= InputEvent.BUTTON2_DOWN_MASK;
		System.out.println("mouse release:" + mask);
		robot.mouseRelease(mask);
	}

	public BufferedImage captureScreen() {
		long t0 = System.currentTimeMillis() / 1000;
		if (t0 % 2 == 1) {
			robot.keyPress(KeyEvent.VK_NUM_LOCK); // keep screen on
		}
		
		BufferedImage img = (cap != null) ? cap.getImage() : robot.createScreenCapture(screenbound);
		
		if (t0 % 2 == 0) {
			robot.keyRelease(KeyEvent.VK_NUM_LOCK); // keep screen on
		}
		return img;
	}
}
