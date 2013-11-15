package aceim.app.utils.linq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class KindaLinqQuery<T> {

	private final Collection<T> collection;
	private KindaLinqRule<T> rule;
	
	public KindaLinqQuery(Collection<T> collection) {
		this.collection = collection;
	}

	public KindaLinqQuery<T> where(KindaLinqRule<T> rule) {
		this.rule = rule;
		return this;
	}

	public List<T> all() {
		List<T> all = new ArrayList<T>();
		for(T item : collection) {
			if (rule.match(item)) {
				all.add(item);
			}
		}
		
		return all;
	}

	public T first() {
		for(T item : collection) {
			if (rule.match(item)) {
				return item;
			}
		}
		
		return null;
	}
}
