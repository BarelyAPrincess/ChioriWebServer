/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.util


/**
 * Provides do... until/while ability to groovy scripts
 * Which as of Jan 2014 was not implemented into groovy
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class Looper
{
	private final Closure code
	
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
