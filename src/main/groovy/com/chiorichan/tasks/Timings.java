/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.tasks;

import java.util.Map;
import java.util.WeakHashMap;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.chiorichan.util.ObjectFunc;

/**
 * Provides basic code timings
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class Timings
{
	/*
	 * Epoch Add-able Seconds
	 */
	public static final int SECOND = 1;
	public static final int SECOND_15 = 15;
	public static final int SECOND_30 = 30;
	public static final int MINUTE = 60;
	public static final int HOUR = MINUTE * 60;
	public static final int DAY = HOUR * 24;
	public static final int DAYS_3 = DAY * 3;
	public static final int DAYS_7 = DAY * 7;
	public static final int DAYS_14 = DAY * 14;
	public static final int DAYS_21 = DAY * 3;
	public static final int DAYS_28 = DAY * 28;
	public static final int DAYS_30 = DAY * 30;
	public static final int DAYS_31 = DAY * 31;
	public static final int YEAR = DAY * 365;
	
	/*
	 * Just multiply the unit by the needed value and TADA!
	 */
	public static final int TICK_SECOND = 50;
	public static final int TICK_SECOND_5 = TICK_SECOND * 5;
	public static final int TICK_SECOND_10 = TICK_SECOND * 10;
	public static final int TICK_SECOND_15 = TICK_SECOND * 15;
	public static final int TICK_SECOND_30 = TICK_SECOND * 30;
	public static final int TICK_MINUTE = TICK_SECOND * 60;
	public static final int TICK_HOUR = TICK_MINUTE * 60;
	public static final int TICK_DAY = TICK_HOUR * 24;
	public static final int TICK_DAYS_3 = TICK_DAY * 3;
	public static final int TICK_DAYS_7 = TICK_DAY * 7;
	public static final int TICK_DAYS_14 = TICK_DAY * 14;
	public static final int TICK_DAYS_21 = TICK_DAY * 3;
	public static final int TICK_DAYS_28 = TICK_DAY * 28;
	public static final int TICK_DAYS_30 = TICK_DAY * 30;
	public static final int TICK_DAYS_31 = TICK_DAY * 31;
	public static final int TICK_YEAR = TICK_DAY * 365;
	
	/**
	 * The current epoch since 1970
	 * 
	 * @return The current epoch
	 */
	public static int epoch()
	{
		return ( int ) ( System.currentTimeMillis() / 1000 );
	}
	
	/**
	 * See {@link #readoutDuration(Number)}
	 */
	public static String readoutDuration( String seconds )
	{
		return readoutDuration( ObjectFunc.castToInt( seconds ) );
	}
	
	/**
	 * Converts the input value into a human readable string, e.g., 0 Day(s) 3 Hour(s) 13 Minutes(s) 42 Second(s).
	 * 
	 * @param seconds
	 *            The duration in seconds
	 * @return Human readable duration string
	 */
	public static String readoutDuration( Number seconds )
	{
		Duration duration = new Duration( seconds );
		PeriodFormatter formatter = new PeriodFormatterBuilder().appendDays().appendSuffix( " Day(s) " ).appendHours().appendSuffix( " Hour(s) " ).appendMinutes().appendSuffix( " Minute(s) " ).appendSeconds().appendSuffix( " Second(s)" ).toFormatter();
		return formatter.print( duration.toPeriod() );
	}
	
	/*
	 * Timing Methods
	 */
	
	/**
	 * Provides reference of context to start time.<br>
	 * We use a WeakHashMap to prevent a memory leak, in case {@link #finish()} is never called and/or context was reclaimed by GC.
	 */
	private static final Map<Object, Long> timings = new WeakHashMap<Object, Long>();
	
	/**
	 * Starts the counter for the referenced context
	 * 
	 * @param context
	 *            The context to reference our start time with.
	 */
	public static void start( Object context )
	{
		timings.put( context, System.currentTimeMillis() );
	}
	
	/**
	 * Finds the total number of milliseconds it took.
	 * Be sure to still call {@link #finish(Object)} as this method is only used for checkpoints.
	 * 
	 * @param context
	 *            The context to reference the starting time.
	 * @return
	 *         The time in milliseconds it took between calling {@link #start(Object)} and this method.<br>
	 *         Returns {@code -1} if we have no record of ever starting.
	 */
	public static long mark( Object context )
	{
		Long start = timings.get( context );
		
		if ( start == null )
			return -1;
		
		return System.currentTimeMillis() - start;
	}
	
	/**
	 * Finds the total number of milliseconds it took and removes the context.
	 * 
	 * @param context
	 *            The context to reference the starting time.
	 * @return
	 *         The time in milliseconds it took between calling {@link #start(Object)} and this method.<br>
	 *         Returns {@code -1} if we have no record of ever starting.
	 */
	public static long finish( Object context )
	{
		Long start = timings.remove( context );
		
		if ( start == null )
			return -1;
		
		return System.currentTimeMillis() - start;
	}
}
