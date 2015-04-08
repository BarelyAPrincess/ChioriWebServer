/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.framework;

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
