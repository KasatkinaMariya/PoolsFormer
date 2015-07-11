import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.urfu.javapools.poolslibrary.mocks.UnstubbedMethodAnswer;

public class First {

	@Rule
	public ExpectedException _thrown = ExpectedException.none();
	
	@Test
	public void test1() {
		
		WeakHashMap<Object,String> weakHashMap = new WeakHashMap<Object,String>();
		Map<Object,String> map = Collections.synchronizedMap(weakHashMap);
		//ConcurrentMap<Object,String> cmap = (ConcurrentMap<Object,String>)map;
		
		System.out.println(map.getClass());
		
		/*
		map.put(new Object(), "");
		
		int i=0;
		while (map.size()>0) {
			System.out.println(i++);
		}
		*/
	}
	
	class Entity {}
	
	@Test
	public void test2() {
		
		LinkedList<Waiter> list = new LinkedList<Waiter>();		
		Waiter w1 = new Waiter();
		Waiter w2 = new Waiter();
		
		list.add(w1);
		list.add(w2);
		
		assertTrue(list.get(0) == w2);
		list.remove(0);
		assertTrue(list.get(0) == w1);
	}
}

class Waiter {

	private boolean _isNotified = false;

	public void doWait() throws InterruptedException {

		while (!_isNotified)
			this.wait();

		_isNotified = false;
	}

	public void doNotify() {

		_isNotified = true;
		this.notify();
	}
}