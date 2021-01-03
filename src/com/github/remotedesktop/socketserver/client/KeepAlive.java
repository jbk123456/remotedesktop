package com.github.remotedesktop.socketserver.client;

import java.util.logging.Level;
import java.util.logging.Logger;

public class KeepAlive implements Runnable {
	static final Logger LOGGER = Logger.getLogger(KeepAlive.class.getName());

	public static final int TIMEOUT = 30;

	private Thread runner;
	private boolean running = true;
	private KVMManager kvmman;
	private boolean numLockOn;

	public KeepAlive(KVMManager kvmman) {
		this.kvmman = kvmman;
	}

	public void startKeepAlive() {
		if (runner == null) {
			runner = new Thread(this, getClass().getName());
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
				LOGGER.log(Level.SEVERE, "received stop signal");
				stop();
			}
		}
		LOGGER.info("keep alive stopped");

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
		LOGGER.info("keep alive stop called");
		running = false;
	}

}
