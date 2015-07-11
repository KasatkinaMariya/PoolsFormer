package ru.urfu.javapools.poolslibrary.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.urfu.javapools.poolslibrary.controller.PoolException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;

public class PoolExceptionMatcher extends TypeSafeMatcher<PoolException> {

	private final TestKey _expectedKey;
	
	public PoolExceptionMatcher (TestKey expectedKey) {
		
		super();
		
		_expectedKey = expectedKey;
	}
	
	@Override
	protected boolean matchesSafely(PoolException exception) {
		
		return (exception.getKey() == null && _expectedKey == null)
			|| _expectedKey.equals(exception.getKey());
	}		
	
	@Override
	protected void describeMismatchSafely(PoolException exception, Description mismatchDescription) {
		
		super.describeMismatchSafely(exception, mismatchDescription);
		
		String message = String.format("Key in PoolException should be '%s', but was '%s'",
										_expectedKey, exception.getKey());
		mismatchDescription.appendText(message);
	}
	
	@Override
	public void describeTo(Description description) {	
	}
}