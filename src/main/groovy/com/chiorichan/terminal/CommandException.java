/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal;

/**
 * Thrown when an unhandled exception occurs during the execution of a Command
 */
@SuppressWarnings( "serial" )
public class CommandException extends RuntimeException
{
	/**
	 * Creates a new instance of <code>CommandException</code> without detail message.
	 */
	public CommandException()
	{
	}
	
	/**
	 * Constructs an instance of <code>CommandException</code> with the specified detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public CommandException( String msg )
	{
		super( msg );
	}
	
	public CommandException( String msg, Throwable cause )
	{
		super( msg, cause );
	}
}
