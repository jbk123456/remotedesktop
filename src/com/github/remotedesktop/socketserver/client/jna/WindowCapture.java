package com.github.remotedesktop.socketserver.client.jna;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HOOKPROC;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.W32APIOptions;

public class WindowCapture {

	private HWND hWnd;
	private int width, height;
	private WinUser.HHOOK hHook;

	public WindowCapture() {
		if (hWnd == null) {
			hWnd = User32.INSTANCE.GetDesktopWindow();
			RECT r = new RECT();
			User32.INSTANCE.GetWindowRect(hWnd, r);
			width = r.right - r.left;
			height = r.bottom - r.top;
			keepScreenOn();
			discardLocalKeyboardInput();
		}

	}

	private void keepScreenOn() {
		Runnable keepScreenOn = new Runnable() {

			@Override
			public void run() {
				try {
					while (true) {
						Thread.sleep(10000);
						Kernel32.INSTANCE.SetThreadExecutionState(0x80000000 | 0x80000001 | 0x80000002 | 0x00000040);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		Thread t = new Thread(keepScreenOn);
		t.setDaemon(true);
		t.start();
	}

	public BufferedImage getImage() {
		try {
			HWND hWnd = User32.INSTANCE.GetDesktopWindow();
			RECT r = new RECT();
			User32.INSTANCE.GetWindowRect(hWnd, r);
			int width = r.right - r.left;
			int height = r.bottom - r.top;
			if (width == this.width && height == this.height) {
				return getScreenshot(hWnd);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			// capure windows individually
		}
		List<DesktopWindow> list = getAllWindows();
		Collections.reverse(list);
		BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (DesktopWindow w : list) {
			System.out.println("window:::" + w.getTitle() + " " + w.getLocAndSize());
			Graphics2D g2d = im.createGraphics();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) 1.0));
			BufferedImage buff2 = null;
			try {
				buff2 = /* GDI32Util. */getScreenshot(w.getHWND());
			} catch (Exception e) {
				continue;
			}
			g2d.drawImage(buff2, w.getLocAndSize().x, w.getLocAndSize().y, null);
			g2d.dispose();

		}

		return im;
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

	public List<DesktopWindow> getAllWindows() {
		final List<DesktopWindow> result = new LinkedList<DesktopWindow>();
		final WNDENUMPROC lpEnumFunc = new WNDENUMPROC() {

			@Override
			public boolean callback(final HWND hwnd, Pointer arg1) {
				try {
					final boolean visible = User32.INSTANCE.IsWindowVisible(hwnd);
					if (visible) {
						WindowInfo info = getWindowInfo(hwnd);
						Rectangle rec = info.rect.toRectangle();
						;
						if (info.rect.left != -32000 && info.rect.top != 32000 && rec.x < width && rec.y < height
								&& rec.width > 0 && rec.height > 0) {

							result.add(new DesktopWindow(hwnd, info.title, "", rec));
						}
					}
				} catch (final Exception e) {
					// FIXME properly handle whatever error is raised
					e.printStackTrace();
				}
				return true;
			}
		};
		if (!User32.INSTANCE.EnumWindows(lpEnumFunc, null))
			throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
		return result;
	}

	public static WindowInfo getWindowInfo(HWND hWnd) {
		RECT r = new RECT();
		User32.INSTANCE.GetWindowRect(hWnd, r);
		char[] buffer = new char[1024];
		User32.INSTANCE.GetWindowText(hWnd, buffer, buffer.length);
		String title = Native.toString(buffer);
		// System.out.println("window rect:::" + title + " " + r);
		WindowInfo info = new WindowInfo(hWnd, r, title);
		return info;
	}
	
	private class KeyboardHook implements HOOKPROC {
	    public LRESULT callback(int nCode, WinDef.WPARAM wParam, KBDLLHOOKSTRUCT info) {
    		Pointer ptr = info.getPointer();
    		long peer = Pointer.nativeValue(ptr);
    		
	    	if (nCode>=0 && !((info.flags &0x00000010) == 0x00000010)) {

		        return new LRESULT(2);
	    	}
    		return User32.INSTANCE.CallNextHookEx(hHook, nCode, wParam, new WinDef.LPARAM(peer));
	    }
	}
	private void discardLocalKeyboardInput() {
		Thread t = new Thread(new Runnable() {


			@Override
			public void run() {
				HOOKPROC hookProc = new KeyboardHook();
				HINSTANCE hInst = Kernel32.INSTANCE.GetModuleHandle(null);

				hHook = User32.INSTANCE.SetWindowsHookEx(User32.WH_KEYBOARD_LL, hookProc, hInst, 0);
				if (hHook == null)
					return;
				final User32.MSG msg = new User32.MSG();

				while (true) {
					User32.INSTANCE.GetMessage(msg, null, 0, 0);
				}

			}
		});
		t.setDaemon(true);
		t.start();
	}
	
	public static class WindowInfo {
		HWND hwnd;
		RECT rect;
		String title;

		public WindowInfo(HWND hwnd, RECT rect, String title) {
			this.hwnd = hwnd;
			this.rect = rect;
			this.title = title;
		}

		public String toString() {
			return String.format("(%d,%d)-(%d,%d) : \"%s\"", rect.left, rect.top, rect.right, rect.bottom, title);
		}
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

		HWND WindowFromPoint(WinDef.POINT.ByValue point);

		public boolean GetClientRect(HWND hWnd, RECT rect);

		LRESULT SendMessage(HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);

	}

	public interface WinGDIExtra extends WinGDI {

		public DWORD SRCCOPY = new DWORD(0x00CC0020);

	}
}
