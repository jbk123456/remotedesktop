package com.github.remotedesktop.socketserver.client;

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

		tileman.setSize((int) kvmman.getScreenBound().getWidth(), (int) kvmman.getScreenBound().getHeight());
		while (true) {
			try {

				tileman.processImage(kvmman.captureScreen(), 6, 6);
				notifyObservers();
				Thread.sleep((int) (1000 / Config.fps));
				//Thread.sleep(10000);
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
