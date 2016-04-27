/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		return false;
	}
}
