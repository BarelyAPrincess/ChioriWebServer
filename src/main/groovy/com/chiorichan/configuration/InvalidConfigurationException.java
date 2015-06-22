/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.configuration;

/**
 * Exception thrown when attempting to load an invalid {@link Configuration}
 */
@SuppressWarnings( "serial" )
public class InvalidConfigurationException extends Exception
{
	/**
	 * Creates a new instance of InvalidConfigurationException without a message or cause.
	 */
	public InvalidConfigurationException()
	{
	}
	
	/**
	 * Constructs an instance of InvalidConfigurationException with the specified message.
	 *
	 * @param msg
	 *            The details of the exception.
	 */
	public InvalidConfigurationException( String msg )
	{
		super( msg );
	}
	
	/**
	 * Constructs an instance of InvalidConfigurationException with the specified cause.
	 *
	 * @param cause
	 *            The cause of the exception.
	 */
	public InvalidConfigurationException( Throwable cause )
	{
		super( cause );
	}
	
	/**
	 * Constructs an instance of InvalidConfigurationException with the specified message and cause.
	 *
	 * @param cause
	 *            The cause of the exception.
	 * @param msg
	 *            The details of the exception.
	 */
	public InvalidConfigurationException( String msg, Throwable cause )
	{
		super( msg, cause );
	}
}
