/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.session;

import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.ExceptionContext;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.ReportingLevel;

/**
 * Thrown for almost all Session Exceptions
 */
public class SessionException extends ApplicationException
{
	private static final long serialVersionUID = -1665918782123029882L;

	public SessionException( Exception cause )
	{
		super( ReportingLevel.E_ERROR, cause );
	}

	public SessionException( String msg )
	{
		super( ReportingLevel.E_ERROR, msg );
	}

	public SessionException( String msg, Throwable cause )
	{
		super( ReportingLevel.E_ERROR, msg, cause );
	}

	@Override
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		return false;
	}
}
