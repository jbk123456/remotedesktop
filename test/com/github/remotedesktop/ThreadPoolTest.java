package com.github.remotedesktop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class ThreadPoolTest {

	ThreadPool pool = new ThreadPool("test", 10);

	int count;

	@Test
	public void testPoolDestroy() throws Exception {
		for (int i = 0; i < 50; i++) {
			pool.start(createRunnable());
		}

		Thread.sleep(1000);
		pool.destroy();
		Thread.sleep(1000);
		
		assertThat(count, is(10));
	}

	private Runnable createRunnable() {
		return new Runnable() {

			@Override
			public void run() {
					try {
						Thread.sleep(120000);
					} catch (InterruptedException e) {
						count++;
					}
			}

		};
	}
}
