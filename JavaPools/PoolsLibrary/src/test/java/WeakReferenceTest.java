import static org.junit.Assert.*;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.junit.Test;

public class WeakReferenceTest {
		
	class Entity {}	
	
	// из обычных коллекций (например, HashSet<T>) необходимо удалять элементы WeakReference самостоятельно  
	@Test(timeout = 30 * 1000)
	public void somethingShouldRemoveWeakReferences() {		
		
		ReferenceQueue<Entity> queue = new ReferenceQueue<Entity>();
		WeakReference<Entity> weakReference = new WeakReference<Entity>(new Entity(), queue);		
		
		HashSet<WeakReference<Entity>> set = new HashSet<WeakReference<Entity>>();
		set.add(weakReference);
		assertNotNull(set.iterator().next().get()); // объект Entity в коллекции есть, живой и здоровый
		
		// сборка мусора рано или поздно запускается, собирает объект Entity и помещает его в очередь
		int i = 0;
		while (queue.poll() == null) {
			System.out.println("Waiting for GC " + i++);
		}
		assertNull(set.iterator().next().get());
		System.out.println("GC has collected Entity object");
		
		// WeakReference на собранный объект Entity остается в коллекции навечно   
		i=0;
		while (set.size() > 0)
			System.out.println("WeakReference hasn't been removed from set " + i++);
		
		// т.е. на выходе имеем, что тот, кто обнаружит в queue новую WeakReference,
		// должен как-то инициировать удаление этой "умершей" WeakReference из set
		
		// в общем случае получается, что сущность, которая содержит WeakReference в каких-то коллекциях,
		// должна передать проверяющему ReferenceQueue (а это еще какой-то отдельный специальный поток) знание о том,
		// как почистить себя от "умерших" WeakReference (с учетом всех ее внутренних особенностей синхронизации и т.д.),
		// что по сложности вроде бы сравнимо со стандартной подпиской на событие об умершем объекте
		// правильно?
	}	
	
	// WeakHashMap удаляет записи с "умершими" ключами сама (не сразу, но удаляет)
	@Test
	public void weakHashMapCleansItself() {		
		
		WeakHashMap<Entity, String> map = new WeakHashMap<Entity, String>();
		map.put(new Entity(), "");
		
		int i=0;
		while (map.size() > 0)
			System.out.println("Waiting for GC " + i++);
		
		// сборка мусора запускается и собирает добавленный в map ключ, после чего
		// WeakHashMap сама удаляет запись, которая была с ним связана - все работает само,
		// никаких дополнительных самостоятельных действий по удалению не требуется
		// это точно правильно :)
	}
	
	// но некоторые обычные коллекции (например, тот же Set<T>) можно построить на основе WeakHashMap,
	// и тогда все снова работает само, не нужно удалять элементы WeakReference самостоятельно  
	@Test
	public void collectionsFromWeakHashMapAlsoCleanThemselves () {
		
		Set<Entity> set = Collections.newSetFromMap(new WeakHashMap<Entity, Boolean>());
		set.add(new Entity());
	
		// WeakReference на собранный объект Entity удаляется сам   
		int i=0;
		while (set.size() > 0)
			System.out.println("Waiting for GC " + i++);
		System.out.println("WeakReference has been removed from set " + i++);
		
		// ура, выход есть, и возможности WeakReference можно использовать не только там, где подходит WeakHashMap
	}
}