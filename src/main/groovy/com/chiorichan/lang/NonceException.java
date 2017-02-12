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

public class NonceException extends ApplicationException
{
	public NonceException( String msg )
	{
		super( ReportingLevel.E_ERROR, msg );
	}

	@Override
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		return false;
	}
}
