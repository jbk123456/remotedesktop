package com.github.remotedesktop.socketserver.client.jna;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;
import com.sun.jna.win32.W32APIOptions;

public class WindowCapture {
	static final int ES_SYSTEM_REQUIRED = 0x00000001;
	static final int ES_DISPLAY_REQUIRED = 0x00000002;
	static final int ES_USER_PRESENT = 0x00000004; // Only supported by Windows XP/Windows Server 2003
	static final int ES_AWAYMODE_REQUIRED = 0x00000040; // Not supported by Windows XP/Windows Server 2003
	static final int ES_CONTINUOUS = 0x80000000;

	private final Map<WinNT.HANDLE, Cursor> cursors;
	private static final Map<String, String> win2Html = getCursorToHtmlMap();

	public WindowCapture() {
		HWND hWnd = User32Extra.INSTANCE.GetDesktopWindow();
		RECT r = new RECT();
		User32.INSTANCE.GetWindowRect(hWnd, r);
		discardLocalInput();
		cursors = loadCursors();

	}

	private static Map<String, String> getCursorToHtmlMap() {
		Map<String, String> map = new HashMap<>();
		map.put("APPSTARTING", "progress");
		map.put("NORMAL", "default");
		map.put("CROSS", "crosshair");
		map.put("HAND", "pointer");
		map.put("HELP", "help");
		map.put("IBEAM", "text");
		map.put("NO", "not-allowed");
		map.put("SIZEALL", "all-scroll");
		map.put("SIZENESW", "nesw-resize");
		map.put("SIZENS", "ns-resize");
		map.put("SIZENWSE", "nwse-resize");
		map.put("SIZEWE", "ew-resize");
		map.put("UP", "n-resize");
		map.put("WAIT", "wait");
		map.put("PEN", "crosshair");
		return map;
	}

	public String getPointer() {
		Cursor c = getCurrentCursor();
		if (c != null) {
			String cursor = win2Html.get(c.toString());
			if (cursor != null) {
				return cursor;
			}
		}
		return "default";
	}

	public BufferedImage getImage() {
		HWND hWnd = User32Extra.INSTANCE.GetDesktopWindow();
		return getScreenshot(hWnd);
	}

	private Cursor getCurrentCursor() {
		final CURSORINFO cursorinfo = new CURSORINFO();
		final int success = User32Extra.INSTANCE.GetCursorInfo(cursorinfo);
		if (success != 1) {
			throw new IllegalArgumentException("getCursorInfo");
		}

		// you can use the address printed here to map the others cursors like
		// ALL_SCROLL
		// some times cursor can be hidden, in this case it will be null
		if (cursorinfo.hCursor != null && cursors.containsKey(cursorinfo.hCursor)) {
			return cursors.get(cursorinfo.hCursor);
		}
		return null;
	}

	private static BufferedImage getScreenshot(HWND hWnd) {

		HDC hdcWindow = User32Extra.INSTANCE.GetDC(hWnd);
		HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

		RECT bounds = new RECT();
		User32Extra.INSTANCE.GetClientRect(hWnd, bounds);

		int width = bounds.right - bounds.left;
		int height = bounds.bottom - bounds.top;

		HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);

		HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, WinGDIExtra.SRCCOPY);

		GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
		GDI32.INSTANCE.DeleteDC(hdcMemDC);

		BITMAPINFO bmi = new BITMAPINFO();
		bmi.bmiHeader.biWidth = width;
		bmi.bmiHeader.biHeight = -height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

		Memory buffer = new Memory(width * height * 4);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);

		GDI32.INSTANCE.DeleteObject(hBitmap);
		User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

		return image;

	}

	interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {

		final Kernel32 INSTANCE = (Kernel32) Native.loadLibrary(Kernel32.class, W32APIOptions.UNICODE_OPTIONS);

		int SetThreadExecutionState(int state);
	}

	private void discardLocalInput() {
		Thread t = new Thread(new Runnable() {
			private HHOOK keyboardHook, mouseHook;
			private int count;

			@Override
			public void run() {
				final HOOKPROC keyboardHookProc = new HOOKPROC() {
					@SuppressWarnings("unused")
					public LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT info) {
						if (nCode >= 0 && !((info.flags & 0x10) == 0x10)) {
							if (info.vkCode == 19 && count++ > 3) {
								System.exit(13);
							}
							count = 0;
							return new LRESULT(2);
						}
						return User32.INSTANCE.CallNextHookEx(keyboardHook, nCode, wParam,
								new WinDef.LPARAM(Pointer.nativeValue(info.getPointer())));
					}
				};

				final HOOKPROC mouseHookProc = new HOOKPROC() {
					@SuppressWarnings("unused")
					public LRESULT callback(int nCode, WinDef.WPARAM wParam, MSLLHOOKSTRUCT info) {
						if (nCode >= 0 && !((info.flags.intValue() & 1) == 1)) {
							count = 0;
							return new LRESULT(2);
						}
						return User32.INSTANCE.CallNextHookEx(mouseHook, nCode, wParam,
								new WinDef.LPARAM(Pointer.nativeValue(info.getPointer())));
					}
				};

				final HINSTANCE hInst = Kernel32.INSTANCE.GetModuleHandle(null);

				keyboardHook = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL, keyboardHookProc, hInst, 0);
				mouseHook = User32.INSTANCE.SetWindowsHookEx(User32.WH_MOUSE_LL, mouseHookProc, hInst, 0);

				final User32.MSG msg = new User32.MSG();

				while (true) {
					User32.INSTANCE.GetMessage(msg, null, 0, 0);
				}

			}
		});
		t.setDaemon(true);
		t.start();
	}

	private Map<WinNT.HANDLE, Cursor> loadCursors() {
		final Map<WinNT.HANDLE, Cursor> cursors = new HashMap<>();
		for (final Cursor cursor : Cursor.values()) {

			final Memory memory = new Memory(Native.getNativeSize(Long.class, null));
			memory.setLong(0, cursor.getCode());
			final Pointer resource = memory.getPointer(0);
			final WinNT.HANDLE hcursor = User32Extra.INSTANCE.LoadImageA(null, resource, WinUser.IMAGE_CURSOR, 0, 0,
					WinUser.LR_SHARED);
			if (hcursor == null || Native.getLastError() != 0) {
				throw new Error("Cursor could not be loaded: " + String.valueOf(Native.getLastError()));
			}

			cursors.put(hcursor, cursor);
		}
		return Collections.unmodifiableMap(cursors);
	}

	public interface GDI32Extra extends GDI32 {

		GDI32Extra INSTANCE = (GDI32Extra) Native.loadLibrary("gdi32", GDI32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

		public boolean BitBlt(HDC hObject, int nXDest, int nYDest, int nWidth, int nHeight, HDC hObjectSource,
				int nXSrc, int nYSrc, DWORD dwRop);

	}

	public interface User32Extra extends User32 {

		User32Extra INSTANCE = (User32Extra) Native.loadLibrary("user32", User32Extra.class,
				W32APIOptions.DEFAULT_OPTIONS);

		public HDC GetWindowDC(HWND hWnd);

		public HWND GetDesktopWindow();

		HWND WindowFromPoint(WinDef.POINT.ByValue point);

		public boolean GetClientRect(HWND hWnd, RECT rect);

		LRESULT SendMessage(HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);

		WinNT.HANDLE LoadImageA(WinDef.HINSTANCE hinst, Pointer lpszName, int uType, int cxDesired, int cyDesired,
				int fuLoad);

		int GetCursorInfo(CURSORINFO cursorinfo);

	}

	@SuppressWarnings("unused")
	public static class CURSORINFO extends Structure {

		public int cbSize;
		public int flags;
		public WinDef.HCURSOR hCursor;
		public WinDef.POINT ptScreenPos;

		public CURSORINFO() {
			this.cbSize = Native.getNativeSize(CURSORINFO.class, null);
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("cbSize", "flags", "hCursor", "ptScreenPos");
		}
	}

	public interface WinGDIExtra extends WinGDI {
		public DWORD SRCCOPY = new DWORD(0x00CC0020);

	}

	public static class MSLLHOOKSTRUCT extends Structure {

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] { "pt", "mouseData", "flags", "time", "dwExtraInfo" });
		}

		public static class ByReference extends MSLLHOOKSTRUCT implements Structure.ByReference {
		}

		public POINT pt;
		public DWORD mouseData;
		public DWORD flags;
		public DWORD time;
		public BaseTSD.ULONG_PTR dwExtraInfo;
	}

	public enum Cursor {
		APPSTARTING(32650), NORMAL(32512), CROSS(32515), HAND(32649), HELP(32651), IBEAM(32513), NO(32648),
		SIZEALL(32646), SIZENESW(32643), SIZENS(32645), SIZENWSE(32642), SIZEWE(32644), UP(32516), WAIT(32514),
		PEN(32631);

		private final int code;

		Cursor(final int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}
}
