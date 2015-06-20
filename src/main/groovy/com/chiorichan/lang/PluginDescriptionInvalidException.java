/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.lang;

/**
 * Thrown when attempting to load an invalid PluginDescriptionFile
 */
public class PluginDescriptionInvalidException extends PluginException
{
	private static final long serialVersionUID = 5721389122281775896L;
	
	/**
	 * Constructs a new InvalidDescriptionException
	 */
	public PluginDescriptionInvalidException()
	{
		super( "Invalid plugin.yaml" );
	}
	
	/**
	 * Constructs a new InvalidDescriptionException with the given message
	 * 
	 * @param message
	 *            Brief message explaining the cause of the exception
	 */
	public PluginDescriptionInvalidException( final String message )
	{
		super( message );
	}
	
	/**
	 * Constructs a new InvalidDescriptionException based on the given Exception
	 * 
	 * @param cause
	 *            Exception that triggered this Exception
	 */
	public PluginDescriptionInvalidException( final Throwable cause )
	{
		super( "Invalid plugin.yaml", cause );
	}
	
	/**
	 * Constructs a new InvalidDescriptionException based on the given Exception
	 * 
	 * @param message
	 *            Brief message explaining the cause of the exception
	 * @param cause
	 *            Exception that triggered this Exception
	 */
	public PluginDescriptionInvalidException( final Throwable cause, final String message )
	{
		super( message, cause );
	}
}
