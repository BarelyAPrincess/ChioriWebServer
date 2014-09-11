/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;


public class StartupException extends RuntimeException
{
	private static final long serialVersionUID = 13L;
	
	public StartupException(String msg)
	{
		super( msg );
	}

	public StartupException(Throwable e)
	{
		super( e );
	}
	
	public StartupException(String msg, Throwable e)
	{
		super( msg, e );
	}
}
