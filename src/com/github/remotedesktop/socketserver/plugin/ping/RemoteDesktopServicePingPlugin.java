package com.github.remotedesktop.socketserver.plugin.ping;

import java.io.IOException;
import java.util.logging.Logger;

import com.github.remotedesktop.socketserver.Request;
import com.github.remotedesktop.socketserver.Response;
import com.github.remotedesktop.socketserver.plugin.base.ServiceHandlerPlugin;

public class RemoteDesktopServicePingPlugin implements ServiceHandlerPlugin {
	static final Logger LOGGER = Logger.getLogger(RemoteDesktopServicePingPlugin.class.getName());

	@Override
	public boolean service(Request req, Response res) throws IOException {
		String path = req.getURI().getPath();
		switch (path) {

		case "/ping": {
			LOGGER.fine("received ping");
			res.message(req.getData());
			return true;
		}
		}
		return false;
	}
}
