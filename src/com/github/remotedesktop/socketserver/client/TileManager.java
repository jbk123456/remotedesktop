package com.github.remotedesktop.socketserver.client;

import java.awt.image.BufferedImage;

public class TileManager {

	public static final int MAX_TILE = 5;
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

	public void processImage(BufferedImage image, int x, int y) {
		BufferedImage subimage;
		int subw, subh;
		numxtile = x;
		numytile = y;
		for (int i = 0; i < numxtile; i++) {
			for (int j = 0; j < numytile; j++) {
				if (tiles[i][j] == null)
					tiles[i][j] = new Tile();
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

}
