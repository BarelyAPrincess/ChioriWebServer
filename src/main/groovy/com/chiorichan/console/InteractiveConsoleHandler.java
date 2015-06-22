/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.console;

import com.chiorichan.net.NetworkWrapper;

/**
 * Used to interface InteractiveConsole with it's creator
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public interface InteractiveConsoleHandler
{
	void println( String... msg );
	
	void print( String... msg );
	
	NetworkWrapper getPersistence();
}
