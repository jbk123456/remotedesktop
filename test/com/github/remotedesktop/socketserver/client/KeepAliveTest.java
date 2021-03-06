package com.github.remotedesktop.socketserver.client;

import static com.github.remotedesktop.socketserver.client.KeepAlive.TIMEOUT;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KeepAliveTest {

	private static final long MS = 1000L;

	@Mock
	private Robot robot;

	@InjectMocks
	private KVMManager kvmman = spy(getKVMManager());

	@Spy
	private KeepAlive cut = new KeepAlive(kvmman);

	@Test
	public void testBildschirmWirdNichtAusgeschaltet() throws Exception {

		doReturn(TIMEOUT * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(1)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(0)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS + 2 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(1)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS + 3 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(1)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS + 4 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(1)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 2).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(1)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 2 + 1 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(1)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 2 + 2 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(2)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 2 + 3 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(2)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(1)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 2 + 4 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(2)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(2)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 3 + 3 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(2)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(2)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 3 + 4 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(3)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(2)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 3 + 5 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(3)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(2)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 3 + 6 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(3)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(3)).keyRelease(KeyEvent.VK_NUM_LOCK);

		doReturn(TIMEOUT * MS * 4 + 6 * MS).when(kvmman).getTime();
		cut.sendKeepAlive();
		verify(robot, times(4)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(3)).keyRelease(KeyEvent.VK_NUM_LOCK);
		doReturn(TIMEOUT * MS * 4 + 8 * MS).when(kvmman).getTime();

		cut.sendKeepAlive();
		verify(robot, times(4)).keyPress(KeyEvent.VK_NUM_LOCK);
		verify(robot, times(4)).keyRelease(KeyEvent.VK_NUM_LOCK);
	}

	private KVMManager getKVMManager() {
		try {
			return KVMManager.getInstance();
		} catch (AWTException e) {
			return null;
		}
	}
}
