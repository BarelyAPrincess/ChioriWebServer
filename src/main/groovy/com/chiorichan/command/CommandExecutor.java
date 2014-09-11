/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.command;

import com.chiorichan.account.bases.SentientHandler;

/**
 * Represents a class which contains a single method for executing commands
 */
public interface CommandExecutor
{
	
	/**
	 * Executes the given command, returning its success
	 * 
	 * @param sender
	 *           Source of the command
	 * @param command
	 *           Command which was executed
	 * @param label
	 *           Alias of the command which was used
	 * @param args
	 *           Passed command arguments
	 * @return true if a valid command, otherwise false
	 */
	public boolean onCommand( SentientHandler sender, Command command, String label, String[] args );
}
