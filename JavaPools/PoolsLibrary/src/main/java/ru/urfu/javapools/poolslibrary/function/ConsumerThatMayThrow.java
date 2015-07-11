package ru.urfu.javapools.poolslibrary.function;

@FunctionalInterface
public interface ConsumerThatMayThrow<T> {

	void accept (T t) throws Exception;
}
