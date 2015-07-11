package ru.urfu.javapools.poolslibrary.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;

public class MaxCountReachedExceptionMatcher
	extends TypeSafeMatcher<ObjectsMaxCountReachedException>
{
	private final TestKey _expectedKey;
	private final int _expectedMaxCount;
		
	public MaxCountReachedExceptionMatcher(TestKey expectedKey, int expectedMaxCount) {
		
		super();
		
		_expectedKey = expectedKey;
		_expectedMaxCount = expectedMaxCount;
	}

	@Override
	protected boolean matchesSafely(ObjectsMaxCountReachedException exception) {		
		return exception.getKey().equals(_expectedKey)
			&& exception.getMaxObjectCount() == _expectedMaxCount;
	}
	
	@Override
	public void describeTo(Description arg0) {
	}	
}
