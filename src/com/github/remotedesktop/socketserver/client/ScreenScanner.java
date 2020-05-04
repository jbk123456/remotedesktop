package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;

import com.github.remotedesktop.Config;

public class ScreenScanner implements Runnable {

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
      tileman.setSize((int)captureScreen.getWidth(), (int)captureScreen.getHeight());
		while (true) {
			try {

				tileman.processImage(kvmman.captureScreen(), TileManager.MAX_TILE, TileManager.MAX_TILE);
				notifyObservers();
				Thread.sleep((int) (1000 / Config.fps));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void notifyObservers() {
		for (int i = 0; i < tileman.getNumXTile(); i++) {
			for (int j = 0; j < tileman.getNumYTile(); j++) {
				Tile tile = tileman.getTile(i, j);
				if (tile.isDirty()) {
					tileobs.updateTile(i, j);
				}
				tileman.getTile(i, j).clearDirty();
			}
		}
		tileobs.updateTileFinish();
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
