/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.lang;

/**
 * This exception is thrown when a permissions backend has issues loading
 */
public class PermissionBackendException extends Exception
{
	private static final long serialVersionUID = -133147199740089646L;
	
	public PermissionBackendException()
	{
	}
	
	public PermissionBackendException( String message )
	{
		super( message );
	}
	
	public PermissionBackendException( String message, Throwable cause )
	{
		super( message, cause );
	}
	
	public PermissionBackendException( Throwable cause )
	{
		super( cause );
	}
	
	public PermissionBackendException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace )
	{
		super( message, cause, enableSuppression, writableStackTrace );
	}
}
