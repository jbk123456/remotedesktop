package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import com.github.remotedesktop.Config;
import com.github.remotedesktop.ThreadPool;

public class TileManager {
	static final Logger LOGGER = Logger.getLogger(TileManager.class.getName());

	public static final int MAX_TILE = Config.max_tile;

	private ThreadPool pool;

	private Tile tiles[][];
	private int numxtile;
	private int numytile;
	private int tilewidth;
	private int tileheight;
	private int screenwidth;
	private int screenheight;

	public TileManager() {
		tiles = new Tile[MAX_TILE][MAX_TILE];
		numxtile = MAX_TILE;
		numytile = MAX_TILE;
		setSize(640, 480);
	}

	public void setSize(int sw, int sh) {
		screenwidth = sw;
		screenheight = sh;
		tilewidth = screenwidth / numxtile;
		tileheight = screenheight / numytile;
	}

	public void processImage(BufferedImage image, int x, int y) throws InterruptedException {
		BufferedImage subimage;
		int subw, subh;
		numxtile = x;
		numytile = y;
		for (int i = 0; i < numxtile; i++) {
			for (int j = 0; j < numytile; j++) {
				if (tiles[i][j] == null)
					tiles[i][j] = new Tile(pool);
				if (i == numxtile - 1)
					subw = tilewidth + (screenwidth % tilewidth);
				else
					subw = tilewidth;
				if (j == numytile - 1)
					subh = tileheight + (screenheight % tileheight);
				else
					subh = tileheight;
				subimage = image.getSubimage(i * tilewidth, j * tileheight, subw, subh);
				synchronized (tiles[i][j]) {
					tiles[i][j].updateImage2(subimage);
				}
			}
		}
		for (int i = 0; i < numxtile; i++) {
			for (int j = 0; j < numytile; j++) {
				Tile tile = tiles[i][j];
				synchronized (tile) {
					tile.waitForFinish();
				}
			}
		}

	}

	public Tile getTile(int x, int y) {
		return tiles[x][y];
	}

	public int getNumXTile() {
		return numxtile;
	}

	public int getNumYTile() {
		return numytile;
	}

//	public void updateQuality(float quality) {
//		for (int i = 0; i < numxtile; i++) {
//			for (int j = 0; j < numytile; j++) {
//				Tile tile = tiles[i][j];
//				synchronized (tile) {
//					tile.updateQuality(quality);
//				}
//			}
//		}
//	}

//	public void setDirty() {
//		LOGGER.info("marking all tines as dirty");
//		for (int i = 0; i < numxtile; i++) {
//			for (int j = 0; j < numytile; j++) {
//				synchronized (tiles[i][j]) {
//					tiles[i][j].setDirty();
//				}
//			}
//		}
//	}

	public void startRenderPool() {
		LOGGER.info("tile manager start called");
		pool = new ThreadPool(getClass().getName(), Config.threads);
	}

	public void stop() {
		LOGGER.info("tile manager stop called");
		if (pool != null) { // may happen if startRenderPool() wasn't called
			pool.destroy();
		}
	}
}
