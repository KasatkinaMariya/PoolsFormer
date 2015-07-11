package ru.urfu.javapools.poolslibrary.objectactions.notification;

public class UserDefinedActionError<TV> {

    private UserDefinedActionType _userDefinedActionType;
    private TV _object;
    private Exception _exception;
    
    @Override
    public boolean equals(Object obj) {
    	
    	if (null == obj)
    		return false;
    	if (this == obj)
    		return true;    	    	
    	if (!(obj instanceof UserDefinedActionError<?>))
    		return false;    	    	

		@SuppressWarnings("rawtypes")
		UserDefinedActionError another = (UserDefinedActionError)obj;

        return _userDefinedActionType == another._userDefinedActionType
               && _object.equals(another._object)
               && _exception.getClass() == another._exception.getClass();
    }

    @Override
    public int hashCode() {
        return _userDefinedActionType.hashCode();
    }

	public UserDefinedActionType getUserDefinedActionType() {
		return _userDefinedActionType;
	}

	public TV getObject() {
		return _object;
	}

	public Exception getException() {
		return _exception;
	}

	public UserDefinedActionError<TV> setUserDefinedActionType(UserDefinedActionType userDefinedActionType) {
		_userDefinedActionType = userDefinedActionType;
		return this;
	}

	public UserDefinedActionError<TV> setObject(TV object) {
		_object = object;
		return this;
	}

	public UserDefinedActionError<TV> setException(Exception exception) {
		_exception = exception;
		return this;
	}
}