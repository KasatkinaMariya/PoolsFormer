package ru.urfu.javapools.poolslibrary.objectactions;

import ru.urfu.javapools.poolslibrary.function.ConsumerThatMayThrow;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.objectactions.notification.INotifier;
import ru.urfu.javapools.poolslibrary.objectactions.notification.UserDefinedActionError;
import ru.urfu.javapools.poolslibrary.objectactions.notification.UserDefinedActionType;

public class ObjectActionsBasedOnDelegateOrInterface<TV> implements IPoolObjectActions<TV>{

	private final FunctionThatMayThrow<TV, Boolean> _isValidDelegate;
    private final ConsumerThatMayThrow<TV> _pingDelegate;
    private final ConsumerThatMayThrow<TV> _resetDelegate;
    private final ConsumerThatMayThrow<TV> _closeDelegate;
    
    private final Class<TV> _type;
    private final INotifier _notifier;
	
	public ObjectActionsBasedOnDelegateOrInterface(Class<TV> type,
												   ExplicitlyDefinedObjectActions<TV> overridingActions,
			 									   INotifier notifier) {
		_type = type;
		_notifier = notifier;
		
		if (overridingActions.getIsValidDelegate() != null)
			_isValidDelegate = overridingActions.getIsValidDelegate();		
		else if (IValidnessCheckable.class.isAssignableFrom(_type))
			_isValidDelegate = x -> ((IValidnessCheckable)x).isValid();
		else _isValidDelegate = x -> true; 

		_pingDelegate = chooseDelegate(overridingActions.getPingDelegate(), IPingable.class);
		_resetDelegate = chooseDelegate(overridingActions.getResetDelegate(), IStateResettable.class);
		_closeDelegate = chooseDelegate(overridingActions.getCloseDelegate(), AutoCloseable.class);
	}
	
	public ObjectActionsBasedOnDelegateOrInterface(Class<TV> type,
			   									   ExplicitlyDefinedObjectActions<TV> overridingActions) {
		this(type,overridingActions,null);
	}
	
	@Override
	public boolean isValid(TV poolObject) {
		return executeSafely(_isValidDelegate, poolObject, UserDefinedActionType.CHECKING_VALIDNESS);	
	}

	@Override
	public boolean ping(TV poolObject) {
		return executeSafely(_pingDelegate, poolObject, UserDefinedActionType.PINGING);		
	}

	@Override
	public boolean reset(TV poolObject) {
		 return executeSafely(_resetDelegate, poolObject, UserDefinedActionType.RESETTING);
	}

	@Override
	public void close(TV poolObject) {
		executeSafely(_closeDelegate, poolObject, UserDefinedActionType.CLOSING);
	}

    @SuppressWarnings("unchecked")
	private boolean executeSafely(Object operationDelegate, TV poolObject, UserDefinedActionType actionType) {
    	
        try {
        	
        	if (operationDelegate instanceof ConsumerThatMayThrow<?>) {
        		((ConsumerThatMayThrow<TV>)operationDelegate).accept(poolObject);
        		return true;
        	}
        	
        	if (operationDelegate instanceof FunctionThatMayThrow<?,?>)
        		return ((FunctionThatMayThrow<TV,Boolean>)operationDelegate).apply(poolObject);

            throw new Exception("Unexpected type of operation delegate: " + operationDelegate);
        }
        catch (Exception e)
        {
            if (_notifier != null)
                _notifier.notify(new UserDefinedActionError<TV>() {{
                	setUserDefinedActionType(actionType);
                	setObject(poolObject);
                	setException(e);
                }});
                
            return false;
        }
    }
    
	private ConsumerThatMayThrow<TV> chooseDelegate(ConsumerThatMayThrow<TV> explicitlyDefined, Class<?> interfaceType) {
		
        if (explicitlyDefined != null)
            return explicitlyDefined;

        boolean interfaceIsImplemented = interfaceType.isAssignableFrom(_type);
        if (interfaceIsImplemented)
            return x -> interfaceType.getMethods()[0].invoke(x);

        return x -> { };
    }
}
