package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.remotedesktop.Config;

public class ScreenScanner implements Runnable {

	private static final Logger logger = Logger.getLogger(ScreenScanner.class.getName());

	private KVMManager kvmman;
	private TileManager tileman;
	private Tile.Observable tileobs;
	private Thread runner;

	public ScreenScanner(KVMManager kvmman_, TileManager tileman_, Tile.Observable tileobs_) {
		kvmman = kvmman_;
		tileman = tileman_;
		tileobs = tileobs_;
	}

	public void run() {
		BufferedImage captureScreen = kvmman.captureScreen();
		int width = (int) captureScreen.getWidth();
		int height = (int) captureScreen.getHeight();
		tileman.setSize(width, height);
		while (true) {
			try {
				captureScreen = kvmman.captureScreen();
				if (captureScreen.getHeight() != height || captureScreen.getWidth() != width) {
					throw new IllegalArgumentException("screen size changed");
				}
				tileman.processImage(captureScreen, TileManager.MAX_TILE, TileManager.MAX_TILE);
				notifyObservers(kvmman.getPointer());
				Thread.sleep((int) (1000 / Config.fps));
			} catch (Throwable e) {
				logger.log(Level.SEVERE, "capture screen", e);
				tileobs.stop();
				break;
			}
		}
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
			runner = new Thread(this);
			runner.start();
		}
	}

	public void startPerformanceTest() {
		startScreenScanning();
	}

}
