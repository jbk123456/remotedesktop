package com.github.remotedesktop.socketserver.client;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.github.remotedesktop.Config;

public class LockScreen implements Runnable {
	private static final Logger logger = Logger.getLogger(LockScreen.class.getName());

	public static final int TIMEOUT = Config.lockscreen;

	private Thread runner;
	private boolean running = true;
	private KVMManager kvmman;

	private GraphicsDevice device;

	private boolean lockScreenShown;

	public LockScreen(KVMManager kvmman) {
		this.kvmman = kvmman;
		init();
	}

	public void startLockScreen() {
		if (runner == null) {
			runner = new Thread(this, getClass().getName());
			runner.start();
		}
	}

	@Override
	public void run() {
		while (running) {
			try {
				tryLockScreen();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, "received stop signal");
				stop();
			}
		}
		logger.info("lock screen stopped");

	}

	void tryLockScreen() {
		if (TIMEOUT > 0) {
			long t = kvmman.getTime();
			long t0 = (t - kvmman.getLastInteractionTime()) / 1000;
			boolean mustLockScreen = t0 >= TIMEOUT;

			if (mustLockScreen) {
				showLockScreen();
			}
		}
	}

	private Frame getFrame() {
		JFrame frame = new JFrame("screen lock");
		frame.setUndecorated(true);
		frame.setResizable(false);
		frame.setAlwaysOnTop(true);
		frame.getContentPane().setBackground(Color.BLACK);
		return frame;
	}

	private void init() {
		GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
		device = graphics.getDefaultScreenDevice();
	}

	public void showLockScreen() {
		if (!lockScreenShown) {
			lockScreenShown = true;
			System.out.println("lock screen");
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					device.setFullScreenWindow(getFrame());
				}
			});
		}
	}
	public void hideLockScreen() {

		if (lockScreenShown) {
			lockScreenShown = false;
			System.out.println("unlock screen");
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					device.setFullScreenWindow(null);
				}
			});
		}
	}

	public void stop() {
		logger.info("keep alive stop called");
		running = false;
	}

}
