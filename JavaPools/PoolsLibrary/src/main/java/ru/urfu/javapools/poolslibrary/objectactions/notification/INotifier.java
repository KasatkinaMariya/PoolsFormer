package ru.urfu.javapools.poolslibrary.objectactions.notification;

public interface INotifier {
	
	<TV> void notify(UserDefinedActionError<TV> actionError);
}