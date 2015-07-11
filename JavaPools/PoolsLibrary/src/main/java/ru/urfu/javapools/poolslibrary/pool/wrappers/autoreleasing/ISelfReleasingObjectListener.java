package ru.urfu.javapools.poolslibrary.pool.wrappers.autoreleasing;

public interface ISelfReleasingObjectListener<TV> {
	
	public void onSelfReleasing (TV selfReleasedObject);
}