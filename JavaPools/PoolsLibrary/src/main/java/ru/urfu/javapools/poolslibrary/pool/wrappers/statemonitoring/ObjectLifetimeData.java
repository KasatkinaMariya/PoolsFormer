package ru.urfu.javapools.poolslibrary.pool.wrappers.statemonitoring;

public class ObjectLifetimeData<TK> {

    private TK _key;
    private long _сreationTimeStamp;
    private long _lastUsageTimeStamp;

    public ObjectLifetimeData(TK key) {
    	
        _key = key;
        _сreationTimeStamp = System.currentTimeMillis();
        _lastUsageTimeStamp = System.currentTimeMillis();
    }

    public void update() {  	
    	_lastUsageTimeStamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("C='%s' LU='%s'",
        					  _сreationTimeStamp, _lastUsageTimeStamp);
    }

	public TK getKey() {
		return _key;
	}

	public long getCreationTimeStamp() {
		return _сreationTimeStamp;
	}

	public long getLastUsageTimeStamp() {
		return _lastUsageTimeStamp;
	}
	
	public ObjectLifetimeData<TK> setCreationTimeStamp(long creationTimeStamp) {
		_сreationTimeStamp = creationTimeStamp;
		return this;		
	}
	
	public ObjectLifetimeData<TK> setLastUsageTimeStamp(long lastUsageTimeStamp) {
		_lastUsageTimeStamp = lastUsageTimeStamp;
		return this;		
	}
}
