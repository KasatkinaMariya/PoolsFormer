package ru.urfu.javapools.poolslibrary.testentities;

import java.util.UUID;

import ru.urfu.javapools.poolslibrary.pool.wrappers.autoreleasing.ISelfReleasingObject;
import ru.urfu.javapools.poolslibrary.pool.wrappers.autoreleasing.ISelfReleasingObjectListener;

public class TestResource implements ISelfReleasingObject<TestResource> {
	
    private UUID _uniqueResourceIdentifier;
    private String _value;
	
	private ISelfReleasingObjectListener<TestResource> _releasingListener;

    public TestResource(String value) {
    	
        _value = value;
        _uniqueResourceIdentifier = UUID.randomUUID();
    }
    
    public void appendValue (String toAppend) {
    	_value += toAppend;
    }

	@Override
    public boolean equals(Object obj) {
    	    	
    	if (null == obj)
    		return false;
    	if (this == obj)
    		return true;    	    	
    	if (!(obj instanceof TestResource))
    		return false;
    	    	
    	TestResource another = (TestResource)obj;
    	return _value.equals(another._value);
    }
    
    @Override
    public int hashCode() {
    	return _uniqueResourceIdentifier.hashCode();    
    }

	@Override
	public void notifyAboutJobCompletion() {
		_releasingListener.onSelfReleasing(this);		
	}

	@Override
	public void setListener(ISelfReleasingObjectListener<TestResource> listener) {
		_releasingListener = listener;		
	}
	
	public UUID getUniqueResourceIdentifier() {
		return _uniqueResourceIdentifier;
	}
	
	public String getValue() {
		return _value;
	}
}