/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http.session;

public class SessionException extends Exception
{
	public SessionException( String string )
	{
		super( string );
	}
	
	public SessionException( Exception e )
	{
		super( e );
	}
	
	private static final long serialVersionUID = -1665918782123029882L;
}
