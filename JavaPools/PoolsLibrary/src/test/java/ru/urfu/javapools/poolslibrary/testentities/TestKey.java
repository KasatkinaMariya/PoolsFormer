package ru.urfu.javapools.poolslibrary.testentities;

public class TestKey {

	private int _identifier;
	
    public TestKey(int identifier) {
		_identifier = identifier;
	}

	@Override
    public boolean equals(Object obj) {
    	    	
    	if (null == obj)
    		return false;
    	if (this == obj)
    		return true;    	    	
    	if (!(obj instanceof TestKey))
    		return false;
    	    	
    	TestKey another = (TestKey)obj;
    	return _identifier == another._identifier;
    }
    
    @Override
    public int hashCode() {
    	return _identifier;
    }
    
    @Override
    public String toString() {
    	return String.format("Id='%s'", _identifier);
    }
    
    public int getIdentifier() {
    	return _identifier;
    }
}