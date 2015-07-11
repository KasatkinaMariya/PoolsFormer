package ru.urfu.javapools.poolslibrary.pool.wrappers.autoreleasing;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolController;
import ru.urfu.javapools.poolslibrary.matchers.InvalidOperationExceptionMatcher;
import ru.urfu.javapools.poolslibrary.mocks.ControllerMocks;
import ru.urfu.javapools.poolslibrary.mocks.ObjectUtilizerMocks;
import ru.urfu.javapools.poolslibrary.mocks.PoolMocks;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class PWAutoReleasingWrapperTests {

	private PWAutoReleasingWrapper<TestKey,TestResource> _pool;
	
	private final TestKey _key = new TestKey(728);
	
	private PoolController<TestKey,TestResource> _successfulControllerMock;
	private PoolController<TestKey,TestResource> _throwingControllerMock;	
	private IPool<TestKey, TestResource> _baseSuccessfulPoolMock;
	private IObjectUtilizer<TestKey,TestResource> _objectUtilizerMock;
	
	@Rule
	public ExpectedException _thrown = ExpectedException.none();
	
	@Before
	public void before() throws Exception {
		
		_successfulControllerMock = ControllerMocks.getNewSuccessful();
		_throwingControllerMock = ControllerMocks.getNewThrowing(_key);
		_baseSuccessfulPoolMock = PoolMocks.getNewReturningSerialWithKey();
		_objectUtilizerMock = ObjectUtilizerMocks.getNew();
		
		_pool = new PWAutoReleasingWrapper<TestKey, TestResource>(_baseSuccessfulPoolMock, _objectUtilizerMock);
	}	

	@Test
	public void controllerWasNotSet_obtainThrowsInvalidPoolOperationException() throws Exception {
		
		_thrown.expect(InvalidPoolOperationException.class);
		_thrown.expectMessage("PWAutoReleasingWrapper needs specified instance of PoolController. " +
                			  "Call PWAutoReleasingWrapper.setPoolController(..) before starting usage of pool");
		_thrown.expect(new InvalidOperationExceptionMatcher(null, null));
		
		_pool.obtain(_key, null);
	}
	
	@Test
	public void objectReleasedItself_controllerIsCalled() throws Exception {
		
		_pool.setPoolController(_successfulControllerMock);
		TestResource obtained = _pool.obtain(_key, null);
		
		obtained.notifyAboutJobCompletion();
		
		verify(_successfulControllerMock).release(obtained);
	}
	
	@Test
	public void controllerThrew_poolDoesNotThrowAndObjectIsUtilized() throws Exception {
		
		_pool.setPoolController(_throwingControllerMock);
		TestResource obtained = _pool.obtain(_key, null);
		
		obtained.notifyAboutJobCompletion();
		
		verify(_objectUtilizerMock).utilize(org.mockito.Mockito.any(TestKey.class), eq(obtained), eq(_pool));
	}
}