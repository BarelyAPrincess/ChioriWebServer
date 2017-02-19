/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.lang;

/**
 * Usually thrown for site loading errors
 */
public class SiteException extends ApplicationException
{
	private static final long serialVersionUID = 8856241361601633171L;

	public SiteException( Exception e )
	{
		super( ReportingLevel.E_ERROR, e );
	}

	public SiteException( String reason )
	{
		super( ReportingLevel.E_ERROR, reason );
	}

	public SiteException( String reason, Exception e )
	{
		super( ReportingLevel.E_ERROR, reason, e );
	}

	@Override
	public ReportingLevel handle( ExceptionReport report, ExceptionContext context )
	{
		return null;
	}
}
