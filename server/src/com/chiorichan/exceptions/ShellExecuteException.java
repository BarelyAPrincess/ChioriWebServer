/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, Atom Node LLC. All Right Reserved.
 */
package com.chiorichan.exceptions;

/**
 *
 * @author Chiori Greene
 */
public class ShellExecuteException extends Exception
{
	public ShellExecuteException()
	{
		super();
	}

	public ShellExecuteException( String message )
	{
		super( message );
	}

	public ShellExecuteException( String message, Throwable cause )
	{
		super( message, cause );
	}

	public ShellExecuteException( Throwable cause )
	{
		super( cause );
	}

	protected ShellExecuteException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace )
	{
		super( message, cause, enableSuppression, writableStackTrace );
	}
}
