package com.github.remotedesktop.socketserver.service;

public class TizeSerializationManaer {

	public final int MAX_TILE = 10;
	private TileSerializer tiles[][];
	private int numxtile;
	private int numytile;

	public TizeSerializationManaer() {
		tiles = new TileSerializer[MAX_TILE][MAX_TILE];
		numxtile = MAX_TILE;
		numytile = MAX_TILE;
	}

	public void processImage(byte[] image, int x, int y, int w, int h) {
		getTile(x, y).updateImage2(image, w, h);

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
