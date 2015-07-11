package ru.urfu.javapools.poolslibrary.objectutilization;

public class GoneObjectEvent<TK,TV> {

	private Object _reporter;
	private TK _key;
	private TV _poolObject;
	
	@Override
	public int hashCode() {
		return _poolObject.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {

    	if (null == obj)
    		return false;
    	if (this == obj)
    		return true;    	    	
    	if (!(obj instanceof GoneObjectEvent<?,?>))
    		return false;
    	    	
    	@SuppressWarnings("unchecked")
		GoneObjectEvent<TK,TV> another = (GoneObjectEvent<TK,TV>)obj;
    	return _reporter.equals(another._reporter)
    		&& _key.equals(another._key)
    		&& _poolObject.equals(another._poolObject);
	}
	
	public Object getReporter() {
		return _reporter;
	}
	
	public TK getKey() {
		return _key;
	}
	
	public TV getPoolObject() {
		return _poolObject;
	}
	
	public GoneObjectEvent<TK,TV> setReporter(Object reporter) {
		_reporter = reporter;
		return this;
	}
	
	public GoneObjectEvent<TK,TV> setKey(TK key) {
		_key = key;
		return this;
	}
	
	public GoneObjectEvent<TK,TV> setPoolObject(TV poolObject) {
		_poolObject = poolObject;
		return this;
	}	 
}
