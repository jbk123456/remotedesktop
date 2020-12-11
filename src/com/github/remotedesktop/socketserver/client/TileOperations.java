package com.github.remotedesktop.socketserver.client;

public interface TileOperations {
	
	void updateTile(int x, int y);

	void updateTileFinish(String cursor);

	void stop();

}