package com.github.remotedesktop;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Config {
	private static final Logger logger = Logger.getLogger(Config.class.getName());

	public static String default_log_level = "0"; //INFO
	public static String default_start_as_service = "false";
	public static String default_start_as_daemon = "false";
	public static String default_http_server = "localhost";
	public static String default_http_port = "6502";
	public static String default_jpeg_quality = "0.5";
	public static String default_fps = "8.0";
	public static String default_lock = "false";
	public static String default_lockscreen = "0";
	public static String default_threads = "8";

	private static Properties prop;

	public static final String JPEG_QUALITY = "JPEG_QUALITY";
	public static final String HTTP_PORT = "HTTP_PORT";
	public static final String HTTP_SERVER = "HTTP_SERVER";
	public static final String FPS = "FPS";
	public static final String LOCK = "LOCK";
	public static final String LOCKSCREEN = "LOCKSCREEN";
	public static final String THREADS = "THREADS";
	public static final String START_AS_SERVICE = "START_AS_SERVICE";
	public static final String LOG_LEVEL = "LOG_LEVEL";
	public static final String START_AS_DAEMON = "START_AS_DAEMON";

	public static float jpeg_quality;
	public static int http_port;
	public static String http_server;
	public static float fps;
	public static boolean lock;
	public static int lockscreen;
	public static int threads;
	public static boolean start_as_service;
	public static boolean start_as_daemon;
	public static int log_level;

	public static Properties getDefaultProperties() {
		Properties defaultProp = new Properties();
		defaultProp.setProperty(LOG_LEVEL, default_log_level);
		defaultProp.setProperty(START_AS_SERVICE, default_start_as_service);
		defaultProp.setProperty(START_AS_DAEMON, default_start_as_daemon);
		defaultProp.setProperty(HTTP_SERVER, default_http_server);
		defaultProp.setProperty(HTTP_PORT, default_http_port);
		defaultProp.setProperty(JPEG_QUALITY, default_jpeg_quality);
		defaultProp.setProperty(FPS, default_fps);
		defaultProp.setProperty(LOCK, default_lock);
		defaultProp.setProperty(LOCKSCREEN, default_lockscreen);
		defaultProp.setProperty(THREADS, default_threads);
		return defaultProp;
	}

	public static void assignValue() {
		http_server = prop.getProperty(HTTP_SERVER);
		http_port = Integer.parseInt(prop.getProperty(HTTP_PORT));
		jpeg_quality = Float.parseFloat(prop.getProperty(JPEG_QUALITY));
		fps = Float.parseFloat(prop.getProperty(FPS));
		lock = Boolean.parseBoolean(prop.getProperty(LOCK));
		lockscreen = Integer.parseInt(prop.getProperty(LOCKSCREEN));
		threads = Integer.parseInt(prop.getProperty(THREADS));
		start_as_service = Boolean.parseBoolean(prop.getProperty(START_AS_SERVICE));
		log_level = Integer.parseInt(prop.getProperty(LOG_LEVEL));
		start_as_daemon = Boolean.parseBoolean(prop.getProperty(START_AS_DAEMON));
	}

	public static void load(String path, boolean refresh) {
		prop = getDefaultProperties();
		if (!refresh) {
			try {
				FileInputStream fis = new FileInputStream(path);
				prop.load(fis);
				fis.close();
			} catch (Exception e) {
				refresh = true;
			}
		}
		if (refresh) {
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(path);
				prop.store(fos, "remotedesktop config");
				fos.close();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "write config file", e);
			}
		}
		assignValue();
	}
}