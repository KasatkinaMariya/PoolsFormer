package ru.urfu.javapools.poolslibrary.locks;

public class NotReentrantReadWriteLock {

	private int _currentReadersCount;
	private int _currentWritersCount;
	private int _currentWriteRequests;
	
	public synchronized void lockRead() throws InterruptedException {
		
		while (!canGrantReadAccess())
			wait();
		
		_currentReadersCount++;
	}
	
	public synchronized void unlockRead() {
		
		_currentReadersCount--;
		notifyAll();
	}
	
	public synchronized void lockWrite() throws InterruptedException {
		
		_currentWriteRequests++;
		
		 while (!canGrantWriteAccess())
			 wait();
		 
		 _currentWriteRequests--;
		 _currentWritersCount++;
	}
	
	public synchronized void unlockWrite() {
		
		_currentWritersCount--;
		notifyAll();
	}
	
	private boolean canGrantReadAccess() {
		return _currentWriteRequests == 0 && _currentWritersCount == 0;
	}	

	private boolean canGrantWriteAccess() {
		return _currentReadersCount == 0 && _currentWritersCount == 0;
	}
}
