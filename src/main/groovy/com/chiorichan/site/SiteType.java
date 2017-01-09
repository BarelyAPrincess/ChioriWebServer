/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.site;

public enum SiteType
{
	NOTSET(), SQL(), FILE();
	
	public String toString()
	{
		switch ( this )
		{
			case FILE:
				return "File";
			case SQL:
				return "Sql";
			default:
				return "Not Set";
		}
	}
}
