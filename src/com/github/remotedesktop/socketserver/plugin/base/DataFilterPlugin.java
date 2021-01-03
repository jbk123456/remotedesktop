package com.github.remotedesktop.socketserver.plugin.base;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface DataFilterPlugin {
	byte[] filter(SelectionKey key, byte[] data) throws IOException;
}