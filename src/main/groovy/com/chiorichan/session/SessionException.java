/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
