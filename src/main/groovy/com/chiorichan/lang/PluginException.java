/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

/**
 * Covers all exceptions that could throw during plugin loads, unloads or etc.
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class PluginException extends Exception
{
	private static final long serialVersionUID = -985004348649679626L;
	
	public PluginException()
	{
		
	}
	
	public PluginException( String message )
	{
		super( message );
	}
	
	public PluginException( String message, Throwable cause )
	{
		super( message, cause );
	}
	
	public PluginException( Throwable cause )
	{
		super( cause );
	}
}
