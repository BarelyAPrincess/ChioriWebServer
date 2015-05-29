/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.util;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;


public class CommonFunc
{
	/**
	 * Gets Epoch
	 * 
	 * @return The precise epoch
	 */
	public static int getEpoch()
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
}
