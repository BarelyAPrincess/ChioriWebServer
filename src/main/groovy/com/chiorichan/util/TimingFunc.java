/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.util;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provides basic code timings
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class TimingFunc
{
	/**
	 * Provides reference of context to start time.<br>
	 * We use a WeakHashMap to prevent a memory leak, in case {@link #finish()} is never called and/or context was reclaimed by GC.
	 */
	static final Map<Object, Long> timings = new WeakHashMap<Object, Long>();
	
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
