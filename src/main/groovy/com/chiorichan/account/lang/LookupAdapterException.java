/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.account.lang;

public class LookupAdapterException extends Exception
{
	private static final long serialVersionUID = 4484558143431369641L;
	
	public LookupAdapterException()
	{
		
	}
	
	public LookupAdapterException( String msg )
	{
		super( msg );
	}
	
	public LookupAdapterException( String msg, Exception e )
	{
		super( msg, e );
	}
}
