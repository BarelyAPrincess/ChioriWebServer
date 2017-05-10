/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.lang;


public class DownloadDeniedException extends DownloadException
{
	private static final long serialVersionUID = 2L;
	
	public DownloadDeniedException( String message, Throwable cause )
	{
		super( message, cause );
	}
	
	public DownloadDeniedException( Throwable cause )
	{
		this( null, cause );
	}
	
	public DownloadDeniedException( String message )
	{
		this( message, null );
	}
	
	public DownloadDeniedException()
	{
		this( null, null );
	}
}
