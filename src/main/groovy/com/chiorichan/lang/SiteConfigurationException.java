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

public class SiteConfigurationException extends SiteException
{
	public SiteConfigurationException( Exception e )
	{
		super( e );
	}

	public SiteConfigurationException( String reason )
	{
		super( reason );
	}

	public SiteConfigurationException( String reason, Exception e )
	{
		super( reason, e );
	}
}
