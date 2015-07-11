package ru.urfu.javapools.poolslibrary.lang;

public class MyReference<T> {

    private T _value;

    public MyReference(T value) {
        this._value = value;
    }

    public T get() {
        return _value;
    }

    public void set(T value) {
        _value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
        return _value.equals(obj);
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }

    @Override
    public String toString() {
        return _value.toString();
    }
}
