package ru.urfu.javapools.poolslibrary.function;

@FunctionalInterface
public interface FunctionThatMayThrow<T,R> {

	R apply (T t) throws Exception;
}
