package ru.urfu.javapools.poolslibrary.utils;

public class DateUtils {

	private static long _DEFAULT_TIME_TOLERANCE_IN_MILLS = 5;
 
	public static boolean datesAreClose (long date1, long date2) {
    	return datesAreClose(date1, date2, _DEFAULT_TIME_TOLERANCE_IN_MILLS);
    }
    
    public static boolean datesAreClose (long date1, long date2, long timeToleranceInMills) {
    	return Math.abs(date1 - date2) < timeToleranceInMills;
    }
    
    public static boolean datesDifferenceIsMoreThan (long date1, long date2, long minimumDifferInMills) {
    	return Math.abs(date1 - date2) > minimumDifferInMills;
    }
    
    public static long currentTime () {
    	return System.currentTimeMillis();
    }
    
    public static void setDefaultTimeTolerance (long timeToleranceInMills) {
    	_DEFAULT_TIME_TOLERANCE_IN_MILLS = timeToleranceInMills;
    }
}
