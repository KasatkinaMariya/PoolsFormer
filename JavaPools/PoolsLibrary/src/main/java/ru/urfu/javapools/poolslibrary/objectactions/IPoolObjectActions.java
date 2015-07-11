package ru.urfu.javapools.poolslibrary.objectactions;

public interface IPoolObjectActions<TV> {
	  boolean isValid(TV poolObject);
	  boolean ping(TV poolObject);
	  boolean reset(TV poolObject);
      void close(TV poolObject);
}
