/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.updater;

public class PermissionDeniedException extends DownloadException
{
	private static final long serialVersionUID = 2L;
	
	public PermissionDeniedException( String message, Throwable cause )
	{
		super( message, cause );
	}
	
	public PermissionDeniedException( Throwable cause )
	{
		this( null, cause );
	}
	
	public PermissionDeniedException( String message )
	{
		this( message, null );
	}
	
	public PermissionDeniedException()
	{
		this( null, null );
	}
}
