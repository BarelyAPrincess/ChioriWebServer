/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.console;

/**
 * Used to receive user input from console
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public interface Interviewer
{
	boolean handleInput( String command );
	
	String getPrompt();
}
