package com.github.remotedesktop;

import com.github.remotedesktop.socketserver.client.DisplayServer;
import com.github.remotedesktop.socketserver.service.http.HttpServer;

public class Launcher {
	private static final String PREFIX = "REMOTEDESKTOP_";

	public static void main(String args[]) throws Exception {
		boolean refreshIni = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().startsWith("--help")) {
				System.out.println("Usage: java -jar remotedesktop.jar --service=true --port=6502:  Starts a HTTP Server listening for requests.\r\nUsage: java -jar remotedesktop.jar --service=false --server=IP --port=6502: Connects to HTTP Server reading mouse and keyboard data and sending it the current desktop as a video stream.\r\nOther options: host, port, quality (0.0-1.0), fps");
				System.exit(1);
			}
			if (args[i].toLowerCase().startsWith("--service=")) {
				Config.default_start_as_service = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--host=") || args[i].toLowerCase().startsWith("--server=")) {
				Config.default_start_as_service = "false";
				Config.default_http_server = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--port=")) {
				Config.default_http_port = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--quality=")) {
				Config.default_jpeg_quality = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--fps=")) {
				Config.default_fps = args[i].split("=")[1];
				refreshIni = true;
			}
		}
		if (System.getProperty(PREFIX + Config.START_AS_SERVICE) != null) {
			Config.default_start_as_service = System.getProperty(PREFIX + Config.START_AS_SERVICE);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.HTTP_SERVER) != null) {
			Config.default_http_server = System.getProperty(PREFIX + Config.HTTP_SERVER);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.HTTP_PORT) != null) {
			Config.default_http_port = System.getProperty(PREFIX + Config.HTTP_PORT);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.JPEG_QUALITY) != null) {
			Config.default_jpeg_quality = System.getProperty(PREFIX + Config.JPEG_QUALITY);
			refreshIni = true;
		}
		if (System.getProperty(PREFIX + Config.FPS) != null) {
			Config.default_fps = System.getProperty(PREFIX + Config.FPS);
			refreshIni = true;
		}
		Config.load("remotedesktop.ini", refreshIni);

		if (Config.start_as_service) {
			HttpServer server = new HttpServer(null, Config.http_port);
			server.start();
		} else {
			DisplayServer a = new DisplayServer("displayserver", Config.http_server, Config.http_port);
			a.start();
		}

	}
}
