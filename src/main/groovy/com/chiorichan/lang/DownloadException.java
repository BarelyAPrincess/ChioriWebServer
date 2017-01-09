/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.lang;

import java.io.IOException;

public class DownloadException extends IOException
{
	private static final long serialVersionUID = 2L;
	
	private final Throwable cause;
	private final String message;
	
	public DownloadException( String message, Throwable cause )
	{
		this.cause = cause;
		this.message = message;
	}
	
	public DownloadException( Throwable cause )
	{
		this( null, cause );
	}
	
	public DownloadException( String message )
	{
		this( message, null );
	}
	
	public DownloadException()
	{
		this( null, null );
	}
	
	@Override
	public synchronized Throwable getCause()
	{
		return this.cause;
	}
	
	@Override
	public String getMessage()
	{
		return message;
	}
}
