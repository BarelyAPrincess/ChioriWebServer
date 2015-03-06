/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
abstract class BuiltinCommand extends Command
{
	public BuiltinCommand( String name )
	{
		super( name );
	}
	
	public BuiltinCommand( String name, String permission )
	{
		super( name, permission );
	}
}