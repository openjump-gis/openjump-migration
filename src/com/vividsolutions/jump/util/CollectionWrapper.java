package com.vividsolutions.jump.util;

import java.util.Collection;
import java.util.Iterator;

public abstract class CollectionWrapper<T> implements Collection<T> {

	public abstract Collection<T> getCollection();

	public int size() {
		return getCollection().size();
	}

	public void clear() {
		getCollection().clear();
	}

	public boolean isEmpty() {
		return getCollection().isEmpty();
	}

	public Object[] toArray() {
		return getCollection().toArray();
	}

	public boolean add(T o) {
		return getCollection().add(o);
	}

	public boolean contains(Object o) {
		return getCollection().contains(o);
	}

	public boolean remove(Object o) {
		return getCollection().remove(o);
	}

	public boolean addAll(Collection<? extends T> c) {
		return getCollection().addAll(c);
	}

	public boolean containsAll(Collection<?> c) {
		return getCollection().containsAll(c);
	}

	public boolean removeAll(Collection<?> c) {
		return getCollection().removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return getCollection().retainAll(c);
	}

	public Iterator<T> iterator() {
		return getCollection().iterator();
	}

	public <T> T[] toArray(T[] a) {
		return getCollection().toArray(a);
	}
}