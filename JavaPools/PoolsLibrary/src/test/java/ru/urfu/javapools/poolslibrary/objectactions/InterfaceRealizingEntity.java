package ru.urfu.javapools.poolslibrary.objectactions;

class InterfaceRealizingEntity implements IValidnessCheckable, IPingable {
	
    private int _value;
    private boolean _pingImplementationWasCalled;

    public InterfaceRealizingEntity(int value) {
        _value = value;
    }

    @Override
    public boolean isValid() {
        return _value >= 4;
    }

    @Override
    public void ping() {
        _pingImplementationWasCalled = true;
    }
    
    public boolean pingWasCalled() {
    	return _pingImplementationWasCalled;
    }
    
    public int getValue() {
    	return _value;
    }
}