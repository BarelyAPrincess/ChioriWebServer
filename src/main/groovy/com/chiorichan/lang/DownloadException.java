/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
