package ru.urfu.javapools.poolslibrary.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class CreationFailedExceptionMatcher
	extends TypeSafeMatcher<ObjectCreationFailedException>
{
	private final TestKey _expectedKey;
	private final FunctionThatMayThrow<TestKey,TestResource> _expectedDelegate;
	private final Throwable _expectedCause;
	
	public CreationFailedExceptionMatcher(TestKey expectedKey,
										  FunctionThatMayThrow<TestKey,TestResource> expectedDelegate,
										  Throwable expectedCause) {
		
		super();
		
		_expectedKey = expectedKey;
		_expectedDelegate = expectedDelegate;
		_expectedCause = expectedCause;
	}

	@Override
	protected boolean matchesSafely(ObjectCreationFailedException exception) {
		return exception.getKey().equals(_expectedKey)
			&& ((exception.getUsedCreateDelegate() == null && _expectedDelegate == null)
				|| exception.getUsedCreateDelegate().equals(_expectedDelegate))
			&& exception.getCause().getClass() == _expectedCause.getClass();
	}

	@Override
	public void describeTo(Description description) {
	}
}
