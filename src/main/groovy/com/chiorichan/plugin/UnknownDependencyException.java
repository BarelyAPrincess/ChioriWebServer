/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;

/**
 * Thrown when attempting to load an invalid Plugin file
 */
public class UnknownDependencyException extends RuntimeException
{
	
	private static final long serialVersionUID = 5721389371901775895L;
	
	/**
	 * Constructs a new UnknownDependencyException based on the given Exception
	 * 
	 * @param throwable
	 *            Exception that triggered this Exception
	 */
	public UnknownDependencyException( final Throwable throwable )
	{
		super( throwable );
	}
	
	/**
	 * Constructs a new UnknownDependencyException with the given message
	 * 
	 * @param message
	 *            Brief message explaining the cause of the exception
	 */
	public UnknownDependencyException( final String message )
	{
		super( message );
	}
	
	/**
	 * Constructs a new UnknownDependencyException based on the given Exception
	 * 
	 * @param message
	 *            Brief message explaining the cause of the exception
	 * @param throwable
	 *            Exception that triggered this Exception
	 */
	public UnknownDependencyException( final Throwable throwable, final String message )
	{
		super( message, throwable );
	}
	
	/**
	 * Constructs a new UnknownDependencyException
	 */
	public UnknownDependencyException()
	{
		
	}
}
