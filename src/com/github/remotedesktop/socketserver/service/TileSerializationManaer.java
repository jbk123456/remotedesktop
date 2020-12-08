package com.github.remotedesktop.socketserver.service;

import com.github.remotedesktop.socketserver.client.TileManager;

public class TileSerializationManaer {

	private TileSerializer tiles[][];
	private int numxtile;
	private int numytile;

	public TileSerializationManaer() {
		tiles = new TileSerializer[TileManager.MAX_TILE][TileManager.MAX_TILE];
		numxtile = TileManager.MAX_TILE;
		numytile = TileManager.MAX_TILE;
	}

	public void processImage(byte[] image, int x, int y, int w, int h) {
		TileSerializer tile = getTile(x, y);
		synchronized (tile) {
			tile.updateImage2(image, w, h);
		}
	}

	public TileSerializer getTile(int x, int y) {
		if (tiles[x][y] == null) {
			return tiles[x][y] = new TileSerializer();
		}
		return tiles[x][y];
	}

	public int getNumXTile() {
		return numxtile;
	}

	public int getNumYTile() {
		return numytile;
	}

}
