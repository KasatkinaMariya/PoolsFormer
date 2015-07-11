package ru.urfu.javapools.poolslibrary.pool.wrappers.statemonitoring;

public class PWObjectStateMonitoringSettings {

	private int _timeSpanBetweenRevivalsInSeconds;
	private Integer _maxObjectLifetimeInSeconds;
	private Integer _maxObjectIdleTimeSpanInSeconds;
	
	public int getTimeSpanBetweenRevivalsInSeconds() {
		return _timeSpanBetweenRevivalsInSeconds;
	}
	
	public Integer getMaxObjectLifetimeInSeconds() {
		return _maxObjectLifetimeInSeconds;
	}
	
	public Integer getMaxObjectIdleTimeSpanInSeconds() {
		return _maxObjectIdleTimeSpanInSeconds;
	}
	
	public void setTimeSpanBetweenRevivalsInSeconds(int timeSpanBetweenRevivalsInSeconds) {
		_timeSpanBetweenRevivalsInSeconds = timeSpanBetweenRevivalsInSeconds;
	}
	
	public void setMaxObjectLifetimeInSeconds(Integer maxObjectLifetimeInSeconds) {
		_maxObjectLifetimeInSeconds = maxObjectLifetimeInSeconds;
	}
	
	public void setMaxObjectIdleTimeSpanInSeconds(Integer maxObjectIdleTimeSpanInSeconds) {
		_maxObjectIdleTimeSpanInSeconds = maxObjectIdleTimeSpanInSeconds;
	}	
}