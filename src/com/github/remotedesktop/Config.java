package com.github.remotedesktop;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
	static final Logger LOGGER = Logger.getLogger(Config.class.getName());

	public static final Map<String, String> DEFAULTS = new HashMap<>();
	static {
		DEFAULTS.put("default_log_level", "-2");
		DEFAULTS.put("default_start_as_service", "false");
		DEFAULTS.put("default_start_as_daemon", "false");
		DEFAULTS.put("default_http_server", "localhost");
		DEFAULTS.put("default_http_port", "6502");
		DEFAULTS.put("default_jpeg_quality", "0.5");
		DEFAULTS.put("default_fps", "15.0");
		DEFAULTS.put("default_lock", "trur");
		DEFAULTS.put("default_udp", "true");
		DEFAULTS.put("default_threads", "8");
		DEFAULTS.put("default_max_tile", "5");
	};;

	public static final Map<String, String> ENV = new HashMap<>();
	static {
		ENV.put("MAX_TILE", "TILES");
		ENV.put("JPEG_QUALITY", "QUALITY");
		ENV.put("HTTP_PORT", "PORT");
		ENV.put("HTTP_SERVER", "HOST");
		ENV.put("FPS", "FPS");
		ENV.put("UDP", "UDP");
		ENV.put("LOCK", "LOCK");
		ENV.put("THREADS", "THREADS");
		ENV.put("START_AS_SERVICE", "SERVICE");
		ENV.put("LOG_LEVEL", "LOG");
		ENV.put("START_AS_DAEMON", "DAEMON");
	};

	public static int max_tile;
	public static float jpeg_quality;
	public static int http_port;
	public static String http_server;
	public static float fps;
	public static boolean udp;
	public static boolean lock;
	public static int threads;
	public static boolean start_as_service;
	public static boolean start_as_daemon;
	public static int log_level;

	private static Properties prop;
	
	public static Properties getDefaultOptions() {
		Properties defaultProp = new Properties();
		for (String key : ENV.keySet()) {
			defaultProp.setProperty(key, DEFAULTS.get("default_" + key.toLowerCase()));
		}
		return defaultProp;
	}

	public static void assignValue() {
		max_tile = Integer.parseInt(prop.getProperty("MAX_TILE"));
		http_server = prop.getProperty("HTTP_SERVER");
		http_port = Integer.parseInt(prop.getProperty("HTTP_PORT"));
		jpeg_quality = Float.parseFloat(prop.getProperty("JPEG_QUALITY"));
		fps = Float.parseFloat(prop.getProperty("FPS"));
		udp = Boolean.parseBoolean(prop.getProperty("UDP"));
		lock = Boolean.parseBoolean(prop.getProperty("LOCK"));
		threads = Integer.parseInt(prop.getProperty("THREADS"));
		start_as_service = Boolean.parseBoolean(prop.getProperty("START_AS_SERVICE"));
		log_level = Integer.parseInt(prop.getProperty("LOG_LEVEL"));
		start_as_daemon = Boolean.parseBoolean(prop.getProperty("START_AS_DAEMON"));
	}

	public static void refresh(String path, boolean override) {
		prop = getDefaultOptions();
		if (!override) {
			try {
				FileInputStream fis = new FileInputStream(path);
				prop.load(fis);
				fis.close();
			} catch (Exception e) {
				LOGGER.log(Level.INFO, "read config file", e);
			}
		}
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(path);
			prop.store(fos, "remotedesktop config");
			fos.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "write config file", e);
		}
		assignValue();
	}
}
