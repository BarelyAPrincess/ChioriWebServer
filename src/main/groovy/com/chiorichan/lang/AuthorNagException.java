/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

@SuppressWarnings( "serial" )
public class AuthorNagException extends RuntimeException
{
	private final String message;
	
	/**
	 * Constructs a new AuthorNagException based on the given Exception
	 * 
	 * @param message
	 *            Brief message explaining the cause of the exception
	 */
	public AuthorNagException( final String message )
	{
		this.message = message;
	}
	
	@Override
	public String getMessage()
	{
		return message;
	}
}
