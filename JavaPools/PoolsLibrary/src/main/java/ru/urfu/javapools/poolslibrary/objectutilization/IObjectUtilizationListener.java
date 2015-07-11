package ru.urfu.javapools.poolslibrary.objectutilization;

public interface IObjectUtilizationListener<TK,TV> {
	
	void onObjectUtilization(GoneObjectEvent<TK,TV> goneObjectEvent);
}