/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

/**
 * Thrown for almost all Session Exceptions
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class SessionException extends Exception
{
	private static final long serialVersionUID = -1665918782123029882L;
	
	public SessionException( String msg )
	{
		super( msg );
	}
	
	public SessionException( Exception cause )
	{
		super( cause );
	}
	
	public SessionException( String msg, Throwable cause )
	{
		super( msg, cause );
	}
}
