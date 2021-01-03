package com.github.remotedesktop.socketserver.client;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public abstract class KVMManager {

	private Map<Integer, Integer> keymap;
	private long lastInputTime = 0;
	private GraphicsEnvironment ge;
	protected GraphicsDevice gd;

	public static final KVMManager getInstance() throws AWTException {
		try {
			return new KVMManagerWindows();
		} catch (Throwable e) {
			return new KVMManagerJava();
		}
	}

	protected KVMManager() throws AWTException {
		ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gd = ge.getDefaultScreenDevice();
		keymap = new HashMap<>();
		assignKeyMap();
	}

	protected int convAscii(int ascii) {
		Integer scancode;
		scancode = (Integer) keymap.get(ascii);
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
		if (scancode < 0) {
			if (scancode < -1) {
				return;
			}
			askiiKeyStrokeViaAltNumpad(key, mask);
			return;
		}
		if ((mask & 1) == 1) {
			sendKeyPress(KeyEvent.VK_SHIFT);
		} else if ((mask & 2) == 2) {
			sendKeyPress(KeyEvent.VK_CONTROL);
		} else if ((mask & 4) == 4) {
			sendKeyPress(KeyEvent.VK_ALT);
		}
		sendKeyPress(scancode);
		sendKeyRelease(scancode);
		if ((mask & 1) == 1) {
			sendKeyRelease(KeyEvent.VK_SHIFT);
		} else if ((mask & 2) == 2) {
			sendKeyRelease(KeyEvent.VK_CONTROL);
		} else if ((mask & 4) == 4) {
			sendKeyRelease(KeyEvent.VK_ALT);
		}
		if (scancode == KeyEvent.VK_ESCAPE) {
			mouseRelease(7);
			sendKeyPress(KeyEvent.VK_ALT);
			sendKeyRelease(KeyEvent.VK_ALT);
			sendKeyPress(KeyEvent.VK_SHIFT);
			sendKeyRelease(KeyEvent.VK_SHIFT);
			sendKeyPress(KeyEvent.VK_CONTROL);
			sendKeyRelease(KeyEvent.VK_CONTROL);
		}
		setLastInputTime(getTime());
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

		sendKeyPress(KeyEvent.VK_ALT);
		String charcode = Integer.toString(key);
		for (char ascii_c : charcode.toCharArray()) {
			int ascii_n = Integer.parseInt(String.valueOf(ascii_c)) + 96;
			sendKeyPress(ascii_n);
			sendKeyRelease(ascii_n);
		}

		sendKeyRelease(KeyEvent.VK_ALT);
		setLastInputTime(getTime());
	}

	protected void mouseMove(int x, int y) {
		if (x > 0 && y > 0) {
			sendMouseMove(x, y);
			setLastInputTime(getTime());
		}
	}

	public void mousePress(int buttons) {

		sendMousePress(buttons);
		setLastInputTime(getTime());
	}

	public void mouseRelease(int buttons) {
		sendMouseRelease(buttons);
		setLastInputTime(getTime());
	}

	public long getTime() {
		return System.currentTimeMillis();
	}

	public long getLastInputTime() {
		return lastInputTime;
	}

	public void setLastInputTime(long lastInputTime) {
		this.lastInputTime = lastInputTime;
	}

	public BufferedImage captureScreen() {
		Rectangle screenbound = gd.getDefaultConfiguration().getBounds();
		return createScreenCapture(screenbound);
	}

	private void assignKeyMap() {
		keymap.put((16), (-2)); // KeyEvent.VK_SHIFT Shift
		keymap.put((17), (-2)); // KeyEvent.VK_CONTROL Ctrl
		keymap.put((18), (-2)); // KeyEvent.VK_ALT Alt
		keymap.put((225), (-2)); // KeyEvent.VK_ALT_GRAPH AltGr

		keymap.put((173), (KeyEvent.VK_MINUS)); // -

		keymap.put((27), (KeyEvent.VK_ESCAPE)); // Esc

		keymap.put((49), (KeyEvent.VK_1)); // 1
		keymap.put((50), (KeyEvent.VK_2)); // 2
		keymap.put((51), (KeyEvent.VK_3)); // 3
		keymap.put((52), (KeyEvent.VK_4)); // 4
		keymap.put((53), (KeyEvent.VK_5)); // 5
		keymap.put((54), (KeyEvent.VK_6)); // 6

		keymap.put((189), (KeyEvent.VK_MINUS)); // -
		keymap.put((187), (KeyEvent.VK_EQUALS)); // =
		keymap.put((8), (KeyEvent.VK_BACK_SPACE)); // Backspace

		keymap.put((9), (KeyEvent.VK_TAB)); // Tab

		keymap.put((87), (KeyEvent.VK_W)); // W
		keymap.put((69), (KeyEvent.VK_E)); // E
		keymap.put((82), (KeyEvent.VK_R)); // R
		keymap.put((84), (KeyEvent.VK_T)); // T
		keymap.put((89), (KeyEvent.VK_Y)); // Y
		keymap.put((85), (KeyEvent.VK_U)); // U
		keymap.put((73), (KeyEvent.VK_I)); // I
		keymap.put((79), (KeyEvent.VK_O)); // O
		keymap.put((80), (KeyEvent.VK_P)); // P

		keymap.put((65), (KeyEvent.VK_A)); // A
		keymap.put((83), (KeyEvent.VK_S)); // S
		keymap.put((68), (KeyEvent.VK_D)); // D
		keymap.put((70), (KeyEvent.VK_F)); // F
		keymap.put((71), (KeyEvent.VK_G)); // G
		keymap.put((72), (KeyEvent.VK_H)); // H
		keymap.put((74), (KeyEvent.VK_J)); // J
		keymap.put((75), (KeyEvent.VK_K)); // K
		keymap.put((76), (KeyEvent.VK_L)); // L
		keymap.put((186), (KeyEvent.VK_SEMICOLON)); // ;

		keymap.put((13), (KeyEvent.VK_ENTER)); // Enter
		keymap.put((90), (KeyEvent.VK_Z)); // Z
		keymap.put((88), (KeyEvent.VK_X)); // X
		keymap.put((67), (KeyEvent.VK_C)); // C
		keymap.put((86), (KeyEvent.VK_V)); // V
		keymap.put((66), (KeyEvent.VK_B)); // B
		keymap.put((78), (KeyEvent.VK_N)); // N
		keymap.put((77), (KeyEvent.VK_M)); // M
		keymap.put((188), (KeyEvent.VK_COMMA)); // ,

		keymap.put((191), (KeyEvent.VK_SLASH)); // /
		keymap.put((32), (KeyEvent.VK_SPACE)); // Space
		keymap.put((112), (KeyEvent.VK_F1)); // F1
		keymap.put((113), (KeyEvent.VK_F2)); // F2
		keymap.put((114), (KeyEvent.VK_F3)); // F3
		keymap.put((115), (KeyEvent.VK_F4)); // F4
		keymap.put((116), (KeyEvent.VK_F5)); // F5
		keymap.put((117), (KeyEvent.VK_F6)); // F6
		keymap.put((118), (KeyEvent.VK_F7)); // F7
		keymap.put((119), (KeyEvent.VK_F8)); // F8
		keymap.put((120), (KeyEvent.VK_F9)); // F9
		keymap.put((121), (KeyEvent.VK_F10)); // F10
		keymap.put((122), (KeyEvent.VK_F11)); // F11
		keymap.put((123), (KeyEvent.VK_F12)); // F12
		keymap.put((111), (KeyEvent.VK_SLASH)); // /
		keymap.put((42), (KeyEvent.VK_ASTERISK)); // *
		keymap.put((45), (KeyEvent.VK_MINUS)); // -
		keymap.put((43), (KeyEvent.VK_PLUS)); // +
		keymap.put((46), (KeyEvent.VK_DELETE)); // Del
		keymap.put((13), (KeyEvent.VK_ENTER)); // Enter
		keymap.put((36), (KeyEvent.VK_HOME)); // Home
		keymap.put((38), (KeyEvent.VK_UP)); // Up
		keymap.put((33), (KeyEvent.VK_PAGE_UP)); // PgUp
		keymap.put((37), (KeyEvent.VK_LEFT)); // Left
		keymap.put((39), (KeyEvent.VK_RIGHT)); // Right
		keymap.put((35), (KeyEvent.VK_END)); // End
		keymap.put((40), (KeyEvent.VK_DOWN)); // Down
		keymap.put((34), (KeyEvent.VK_PAGE_DOWN)); // PgDn
		keymap.put((45), (KeyEvent.VK_INSERT)); // Ins
		keymap.put((46), (KeyEvent.VK_DELETE)); // Del
	}

	protected abstract void sendKeyPress(int vkAlt);

	protected abstract void sendKeyRelease(int vkAlt);

	protected abstract void sendMouseMove(int x, int y);

	protected abstract void sendMousePress(int buttons);

	protected abstract void sendMouseRelease(int buttons);

	protected abstract BufferedImage createScreenCapture(Rectangle screenbound);

	public abstract String getPointer();

	public abstract void keepScreenOn(boolean toggle);

}
