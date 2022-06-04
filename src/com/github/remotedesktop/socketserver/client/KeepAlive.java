package com.github.remotedesktop.socketserver.client;

import java.util.logging.Level;
import java.util.logging.Logger;

public class KeepAlive implements Runnable {
	private static final Logger logger = Logger.getLogger(KeepAlive.class.getName());

	public static final int TIMEOUT = 30;

	private Thread runner;
	private boolean running = true;
	private KVMManager kvmman;
	private boolean numLockOn;
	private static KeepAlive instance;

	public static KeepAlive getInstance(KVMManager kvmman) {
		if (instance != null) {
			return instance;
		}
		return instance = new KeepAlive(kvmman);
	}
	
	KeepAlive(KVMManager kvmman) {
		this.kvmman = kvmman;
	}

	public void startKeepAlive() {
		if (runner == null) {
			runner = new Thread(this, getClass().getName());
			runner.setDaemon(true);
			runner.start();
		}
	}

	@Override
	public void run() {
		while (running) {
			try {
				sendKeepAlive();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.log(Level.SEVERE, "received stop signal");
			}
		}
		logger.info("keep alive stopped");

	}

	void sendKeepAlive() {
		long t = kvmman.getTime();
		long t0 = (t - kvmman.getLastInputTime()) / 1000;
		boolean mustKeepAlive = t0 >= TIMEOUT;
		boolean mustSwitchOffNumlock = t0 >= 2; // must be < 5 to avoid accessibility functions

		if (mustKeepAlive && !numLockOn) {
			kvmman.keepScreenOn(true);
			numLockOn = true;
			kvmman.setLastInputTime(t);
		} else if (mustSwitchOffNumlock && numLockOn) {
			kvmman.keepScreenOn(false);
			numLockOn = false;
			kvmman.setLastInputTime(t);
		}

	}

	public void stop() {
		logger.info("keep alive stop called (ignored, i am a daemon)");
	}

}
