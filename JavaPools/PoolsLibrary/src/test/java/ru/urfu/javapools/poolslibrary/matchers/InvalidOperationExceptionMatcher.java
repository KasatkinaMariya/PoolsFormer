package ru.urfu.javapools.poolslibrary.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class InvalidOperationExceptionMatcher
	extends TypeSafeMatcher<InvalidPoolOperationException>
{
	private final TestKey _expectedKey;
	private final TestResource _expectedObject;
		
	public InvalidOperationExceptionMatcher(TestKey expectedKey, TestResource expectedObject) {
		
		super();
		
		_expectedKey = expectedKey;
		_expectedObject = expectedObject;
	}

	@Override
	protected boolean matchesSafely(InvalidPoolOperationException exception) {
		return ((exception.getObject() == null && _expectedObject == null) || exception.getObject().equals(_expectedObject))
			&& ((exception.getKey() == null && _expectedKey == null) || exception.getKey().equals(_expectedKey));
	}
	
	@Override
	public void describeTo(Description description) {
	}
}
