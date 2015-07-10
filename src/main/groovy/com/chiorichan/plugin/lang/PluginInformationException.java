/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.plugin.lang;

/**
 * Thrown when attempting to load an invalid PluginDescriptionFile
 */
public class PluginInformationException extends PluginException
{
	private static final long serialVersionUID = 5721389122281775896L;
	
	/**
	 * Constructs a new InvalidDescriptionException
	 */
	public PluginInformationException()
	{
		super( "Invalid plugin.yaml" );
	}
	
	/**
	 * Constructs a new InvalidDescriptionException with the given message
	 * 
	 * @param message
	 *            Brief message explaining the cause of the exception
	 */
	public PluginInformationException( final String message )
	{
		super( message );
	}
	
	/**
	 * Constructs a new InvalidDescriptionException based on the given Exception
	 * 
	 * @param cause
	 *            Exception that triggered this Exception
	 */
	public PluginInformationException( final Throwable cause )
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
	public PluginInformationException( final Throwable cause, final String message )
	{
		super( message, cause );
	}
}
