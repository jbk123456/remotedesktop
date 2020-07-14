package com.github.remotedesktop;

import java.awt.HeadlessException;
import java.io.IOException;
import java.io.PrintStream;

import com.github.remotedesktop.socketserver.client.DisplayServer;
import com.github.remotedesktop.socketserver.service.http.HttpServer;

public class Launcher {
	private static final String PREFIX = "REMOTEDESKTOP_";

	public static void main(String args[]) throws Exception {
		boolean refreshIni = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().startsWith("--help")) {
				System.out.println("Usage: java -jar remotedesktop.jar --service=true --port=6502:  "
						+ "Starts a HTTP Server listening for requests.\r\n"
						+ "Usage: java -jar remotedesktop.jar --service=false --server=IP --port=6502: "
						+ "Connects to HTTP Server reading mouse and keyboard data and sending it the current "
						+ "desktop as a video stream.\r\nOther options: daemon (true/false) host, port, quality (0.0-1.0), fps");
				System.exit(1);
			}
			if (args[i].toLowerCase().startsWith("--service=")) {
				Config.default_start_as_service = args[i].split("=")[1];
				refreshIni = true;
			}
			if (args[i].toLowerCase().startsWith("--daemon=")) {
				Config.default_start_as_daemon = args[i].split("=")[1];
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
		if (System.getProperty(PREFIX + Config.START_AS_DAEMON) != null) {
			Config.default_start_as_daemon = System.getProperty(PREFIX + Config.START_AS_DAEMON);
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

		if (Config.start_as_daemon && !Boolean.getBoolean("IGNORE_DAEMON_FLAG")) {
			final String[] newargs = new String[4];

			newargs[0] = "java";
			newargs[1] = "-DIGNORE_DAEMON_FLAG=true";

			newargs[2] = "-jar";
			newargs[3] = "remotedesktop.jar";

			System.in.close();
			System.out.close();
			System.err.close();

			new Thread(new Runnable() {

				public void run() {
					try {
						Runtime.getRuntime().exec(newargs);
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(13);
					}
				}
			}).start();
			Thread.sleep(20000);
			System.exit(0);
		}

		boolean startAsService = Config.start_as_service;
		if (!startAsService) {
			try {
				System.setOut(new PrintStream("remotedesktop_displayserver_log.txt"));
				System.setErr(new PrintStream("remotedesktop_displayserver_err.txt"));
				DisplayServer a = new DisplayServer("displayserver", Config.http_server, Config.http_port);
				a.start();
			} catch (HeadlessException e) {
				startAsService = true;
			}
		}
		if (startAsService) {
			System.setOut(new PrintStream("remotedesktop_httpserver_log.txt"));
			System.setErr(new PrintStream("remotedesktop_httpserver_err.txt"));
			HttpServer server = new HttpServer(null, Config.http_port);
			server.start();
		}

	}

}
