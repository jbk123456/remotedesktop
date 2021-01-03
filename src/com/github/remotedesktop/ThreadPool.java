package com.github.remotedesktop;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadPool {
	static final Logger LOGGER = Logger.getLogger(ThreadPool.class.getName());

	private String name;
	private int threads;
	private int idles;
	private int poolMaxSize;

	private LinkedList<Runnable> runnables = new LinkedList<>();
	private List<Delegate> delegates = new LinkedList<>();

	/**
	 * Creates a new thread pool.
	 *
	 * @param name        - The name of the pool threads.
	 * @param poolMaxSize - The max. number of threads, must be &gt;= 1.
	 */
	public ThreadPool(String name, int poolMaxSize) {
		if (poolMaxSize < 1)
			throw new IllegalArgumentException("poolMaxSize must be >0");

		init(name, poolMaxSize);
	}

	/**
	 * Threads continue to pull runnables and run them in the thread environment.
	 */
	protected class Delegate extends Thread {
		protected boolean terminate = false;

		public Delegate(String name) {
			super(name);
		}

		public Delegate(ThreadGroup group, String name) {
			super(group, name);
		}

		protected void terminate() {
		}

		protected void end() {
		}

		protected void createThread(String name) {
			startNewThread(name);
		}

		public void run() {
			try {
				while (!terminate) {
					getNextRunnable().run();
					end();
				}
			} catch (InterruptedException e) {
				/* ignore */
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "runnable terminated unexpected", t);
				createThread(getName());
			} finally {
				terminate();
			}
		}
	}

	protected Delegate createDelegate(String name) {
		return new Delegate(name);
	}

	protected void startNewThread(String name) {
		Delegate d = createDelegate(name);
		delegates.add(d);
		d.start();
	}

	/*
	 * Helper: Pull a runnable off the list of runnables. If there's no work, sleep
	 * the thread until we receive a notify.
	 */
	private synchronized Runnable getNextRunnable() throws InterruptedException {
		while (runnables.isEmpty()) {
			idles++;
			wait();
			idles--;
		}
		return runnables.removeFirst();
	}

	/**
	 * Push a runnable to the list of runnables. The notify will fail if all threads
	 * are busy. Since the pool contains at least one thread, it will pull the
	 * runnable off the list when it becomes available.
	 *
	 * @param r - The runnable
	 */
	public synchronized void start(Runnable r) {
		runnables.add(r);
		if (idles == 0 && threads < poolMaxSize) {
			threads++;
			startNewThread(name + "#" + String.valueOf(threads));
		} else
			notify();
	}

	protected void init(String name, int poolMaxSize) {
		this.name = name;
		this.poolMaxSize = poolMaxSize;
	}

	/**
	 * Terminate all threads in the pool.
	 */
	public synchronized void destroy() {
		for (Iterator<Delegate> ii = delegates.iterator(); ii.hasNext();) {
			Delegate d = ii.next();
			d.terminate = true;
			d.interrupt();
		}
	}

}
