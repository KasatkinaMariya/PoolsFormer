package ru.urfu.javapools.poolslibrary.locks;

public class FairWaiter {

	private boolean _isNotified = false;

	public synchronized void doWait() throws InterruptedException {

		while (!_isNotified)
			this.wait();

		_isNotified = false;
	}

	public synchronized void doNotify() {

		_isNotified = true;
		this.notify();
	}
}