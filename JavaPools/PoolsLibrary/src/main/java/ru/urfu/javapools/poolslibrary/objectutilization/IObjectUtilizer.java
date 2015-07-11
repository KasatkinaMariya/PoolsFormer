package ru.urfu.javapools.poolslibrary.objectutilization;

public interface IObjectUtilizer<TK,TV> {
	
	void utilize(TK key, TV pooObject, Object caller);
	
	void addListener(IObjectUtilizationListener<TK,TV> listener);
	void removeListener(IObjectUtilizationListener<TK,TV> listener);	
	int getListenersCount();
}