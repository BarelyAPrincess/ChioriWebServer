/*
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.util


/**
 * Provides do... until/while ability to groovy scripts
 * Which as of Jan 2014 was not implemented into groovy
 */
public class Looper
{
	private Closure code
	
	public static Looper go( Closure code )
	{
		new Looper(code:code)
	}
	
	public void until( Closure test )
	{
		code()
		while (!test())
		{
			code()
		}
	}
	
	public void while0( Closure test )
	{
		code()
		while (test())
		{
			code()
		}
	}
}
