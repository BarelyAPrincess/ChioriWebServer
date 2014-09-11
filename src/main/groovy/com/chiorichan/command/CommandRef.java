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

public class CommandRef
{
	public final String command;
	public final SentientHandler sender;
	
	public CommandRef(SentientHandler _sender, String _command)
	{
		this.sender = _sender;
		this.command = _command;
	}
}
