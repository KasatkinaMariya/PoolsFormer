package ru.urfu.javapools.poolslibrary.locks;

import java.util.LinkedList;
import java.util.List;

public class FairLockWithPrecedence {

	private boolean _isLocked = false;
	private Thread _currentLockingThread = null;

	private List<FairWaiter> _importantWaiters = new LinkedList<FairWaiter>();
	private List<FairWaiter> _otherWaiters = new LinkedList<FairWaiter>();
	
	public void lockImportant() throws InterruptedException {
		lockInternal(_importantWaiters);
	}

	public void lock() throws InterruptedException {
		lockInternal(_otherWaiters);
	}

	public synchronized void unlock() {

		if (Thread.currentThread() != _currentLockingThread)
			throw new IllegalMonitorStateException("Calling thread didn't obtain the lock, so it can't unlock it");
		
		_isLocked = false;
		_currentLockingThread = null;

		if (_importantWaiters.size() > 0)
			_importantWaiters.get(0).doNotify();
		else if (_otherWaiters.size() > 0)
			_otherWaiters.get(0).doNotify();
	}
	
	private void lockInternal(List<FairWaiter> queue) throws InterruptedException {
		
		FairWaiter waiter = new FairWaiter();
		synchronized (this) {
			queue.add(waiter);			
		}

		boolean isMyTurn = false;
		while (!isMyTurn) {

			synchronized (this) {			
				isMyTurn = !_isLocked && queue.get(0) == waiter;			
				if (isMyTurn) {
					_isLocked = true;
					_currentLockingThread = Thread.currentThread();
					queue.remove(0);
					return;
				}
			}
			
			try {
				waiter.doWait();
			} catch (InterruptedException e) {
				synchronized (this) {
					queue.remove(waiter);
				}				
				throw e;
			}			
		}
	}
}