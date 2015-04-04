/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.lang;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class PluginUnconfiguredException extends RuntimeException
{
	private static final long serialVersionUID = 4789128239905660393L;
	
	public PluginUnconfiguredException( String message )
	{
		super( message );
	}
}