package ru.urfu.javapools.poolslibrary.objectutilization;

import java.util.HashSet;

public class ObjectUtilizer<TK,TV> implements IObjectUtilizer<TK,TV> {

	private final HashSet<IObjectUtilizationListener<TK,TV>> _listeners;
	
	public ObjectUtilizer () {
		_listeners = new HashSet<IObjectUtilizationListener<TK,TV>>();
	}
	
	@Override
	public void utilize(TK key, TV pooObject, Object caller) {
		
		GoneObjectEvent<TK,TV> event = new GoneObjectEvent<TK,TV>() {{
			setReporter(caller);
			setKey(key);
			setPoolObject(pooObject);
		}};
		
		for (IObjectUtilizationListener<TK,TV> listener : _listeners)
			listener.onObjectUtilization(event);			
	}

	@Override
	public void addListener(IObjectUtilizationListener<TK, TV> listener) {
		
		if (listener != null)
			_listeners.add(listener);		
	}
	
	@Override
	public void removeListener(IObjectUtilizationListener<TK, TV> listener) {
		
		if (listener != null)
			_listeners.remove(listener);
	}	

	@Override
	public int getListenersCount() {
		return _listeners.size();
	}
}