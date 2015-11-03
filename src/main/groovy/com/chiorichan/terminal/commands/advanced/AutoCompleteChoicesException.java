/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal.commands.advanced;

public class AutoCompleteChoicesException extends RuntimeException
{
	private static final long serialVersionUID = -8163025621439288595L;
	
	protected String argName;
	protected String[] choices;
	
	public AutoCompleteChoicesException( String[] choices, String argName )
	{
		super();
		this.choices = choices;
		this.argName = argName;
	}
	
	public String getArgName()
	{
		return argName;
	}
	
	public String[] getChoices()
	{
		return choices;
	}
}
