package ru.urfu.javapools.poolslibrary.pool.wrappers.autoreleasing;

public interface ISelfReleasingObject<TV> {
	
	void notifyAboutJobCompletion();	
	void setListener(ISelfReleasingObjectListener<TV> listener);
}