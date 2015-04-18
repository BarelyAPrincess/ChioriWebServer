/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

/**
 * Usually thrown for site loading errors 
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class SiteException extends Exception
{
	private static final long serialVersionUID = 8856241361601633171L;
	
	public SiteException( String reason )
	{
		super( reason );
	}
	
	public SiteException( Exception e )
	{
		super( e );
	}
	
	public SiteException( String reason, Exception e )
	{
		super( reason, e );
	}
}
