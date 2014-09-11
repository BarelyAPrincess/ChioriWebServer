/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.command.defaults;

import java.util.List;

import com.chiorichan.command.Command;

public abstract class ChioriCommand extends Command
{
	protected ChioriCommand(String name)
	{
		super( name );
	}
	
	protected ChioriCommand(String name, String description, String usageMessage, List<String> aliases)
	{
		super( name, description, usageMessage, aliases );
	}
}
