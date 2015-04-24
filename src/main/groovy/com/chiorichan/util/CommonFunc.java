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


public class CommonFunc
{
	/**
	 * @return Epoch based on the current Timezone
	 */
	public static int getEpoch()
	{
		return ( int ) ( System.currentTimeMillis() / 1000 );
	}
	
	public static boolean isValidMD5( String s )
	{
		return s.matches( "[a-fA-F0-9]{32}" );
	}
	
	public static boolean isRoot()
	{
		return System.getProperty( "user.name" ).equalsIgnoreCase( "root" );
	}
}
