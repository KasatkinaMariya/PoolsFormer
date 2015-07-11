package ru.urfu.javapools.poolslibrary.pool.basicfunctionality;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;

public enum LoadBalancingStrategy {

	DISTRIBUTED_AMONG_ALL_OBJECTS,
    INTENSIVE_ON_RECENTLY_USED_OBJECTS;
	
	public <TV> Collection<TV> createStorage () {
		
		switch (this) {
			case DISTRIBUTED_AMONG_ALL_OBJECTS:
				return new LinkedList<TV>();
			case INTENSIVE_ON_RECENTLY_USED_OBJECTS:
				return new Stack<TV>();
			default:
				return new LinkedList<TV>();	
		}
	}
}
