package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.remotedesktop.Config;

public class ScreenScanner implements Runnable {

	private static final Logger logger = Logger.getLogger(ScreenScanner.class.getName());

	private KVMManager kvmman;
	private TileManager tileman;
	private TileOperations tileobs;
	private Thread runner;
	private boolean running = true;
	private boolean clientsConnected = true; // need to send at least one screen initially
	private float fps;
	
	public ScreenScanner(KVMManager kvmman, TileManager tileman, TileOperations tileobs) {
		this.kvmman = kvmman;
		this.tileman = tileman;
		this.tileobs = tileobs;
		fps = Config.fps;
	}

	public void run() {
		BufferedImage captureScreen = kvmman.captureScreen();
		int width = (int) captureScreen.getWidth();
		int height = (int) captureScreen.getHeight();
		tileman.setSize(width, height);
		while (running) {
			try {
				if (!clientsConnected) {
					Thread.sleep((long)(1000 / fps));
					continue;
				}
				long t0 = System.currentTimeMillis();
				captureScreen = kvmman.captureScreen();
				if (captureScreen.getHeight() != height || captureScreen.getWidth() != width) {
					throw new IllegalArgumentException("screen size changed");
				}
				long tcap = System.currentTimeMillis()-t0;
				tileman.processImage(captureScreen, TileManager.MAX_TILE, TileManager.MAX_TILE);
				long tcreat = System.currentTimeMillis()-tcap-t0;
				notifyObservers(kvmman.getPointer());
				long t1 = System.currentTimeMillis();
				long td = t1 - t0;
				long t = (long) (1000 / fps);
				long tsleep = t-td;
				logger.finer(String.format("t: %d, td: %d, tsleep: %d, tcap: %d, tcreat: %d", t, td, tsleep, tcap, tcreat));
				if (tsleep>0) {
					Thread.sleep(tsleep);
				}
			} catch (Throwable e) {
				logger.log(Level.SEVERE, "capture screen", e);
				tileobs.stop();
				break;
			}
		}
		logger.info("screen scanner stopped");
	}

	private void notifyObservers(String cursor) {
		for (int i = 0; i < tileman.getNumXTile(); i++) {
			for (int j = 0; j < tileman.getNumYTile(); j++) {
				Tile tile = tileman.getTile(i, j);
				if (tile.isDirty()) {
					tileobs.updateTile(i, j);
				}
				tileman.getTile(i, j).clearDirty();
			}
		}
		tileobs.updateTileFinish(cursor);
	}

	public void startScreenScanning() {
		if (runner == null) {
			runner = new Thread(this, getClass().getName());
			runner.start();
		}
	}
	public void stop() {
		logger.info("screen scanner stop called");
		running = false;
		if (runner != null) {
			runner.interrupt();
			runner = null;
		}
	}

	public void updateFps(float fps) {
		this.fps = fps;
	}
	public void setClientsConnected(boolean b) {
		this.clientsConnected=b;
	}

}
