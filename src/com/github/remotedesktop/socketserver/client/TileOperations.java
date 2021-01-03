package com.github.remotedesktop.socketserver.client;

import java.io.IOException;

public interface TileOperations {
	
	void updateTile(int x, int y) throws IOException;

	void updateScreen(String cursor) throws IOException;

	void stop();

}