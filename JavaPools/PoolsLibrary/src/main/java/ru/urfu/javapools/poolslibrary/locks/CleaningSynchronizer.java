package ru.urfu.javapools.poolslibrary.locks;

public class CleaningSynchronizer {

	private int _currentOperationsCount;
	private boolean _cleaningIsExecuted;
	private FairWaiter _cleaningWaiter;

	public void lockCleaning() throws InterruptedException {

		synchronized (this) {
			_cleaningWaiter = new FairWaiter();	
		}

		boolean isCleaningTurn = false;
		while (!isCleaningTurn) {

			synchronized (this) {
				isCleaningTurn = canGrantCleaningAccess();
				if (isCleaningTurn) {
					_cleaningWaiter = null;
					_cleaningIsExecuted = true;
					return;
				}
			}
			
			try {
				_cleaningWaiter.doWait();
			} catch (InterruptedException e) {
				_cleaningWaiter = null;
			}
		}
	}

	public synchronized void unlockCleaning() {

		_cleaningIsExecuted = false;
		notifyAll();
	}

	public synchronized void lockPoolOperation() throws InterruptedException {

		while (!canGrantPoolOperationAccess())
			wait();

		_currentOperationsCount++;
	}

	public synchronized void unlockPoolOperation() {

		_currentOperationsCount--;

		if (_cleaningWaiter != null)
			_cleaningWaiter.doNotify();
	}

	private boolean canGrantCleaningAccess() {
		return _currentOperationsCount == 0;
	}

	private boolean canGrantPoolOperationAccess() {
		return !_cleaningIsExecuted && _cleaningWaiter == null;
	}
}
