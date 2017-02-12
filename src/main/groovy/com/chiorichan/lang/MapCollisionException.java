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
 * Thrown when two maps are joined and have conflicting keys
 */
public class MapCollisionException extends ApplicationException
{
	public MapCollisionException()
	{
		super( ReportingLevel.E_IGNORABLE );
	}

	@Override
	public boolean handle( ExceptionReport report, ExceptionContext context )
	{
		return false;
	}
}
