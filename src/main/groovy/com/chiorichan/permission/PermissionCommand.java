/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import com.chiorichan.console.commands.AdvancedCommand;
import com.chiorichan.permission.commands.GroupCommands;
import com.chiorichan.permission.commands.PromotionCommands;
import com.chiorichan.permission.commands.ReferenceCommands;
import com.chiorichan.permission.commands.UserCommands;
import com.chiorichan.permission.commands.UtilityCommands;

public class PermissionCommand extends AdvancedCommand
{
	public PermissionCommand()
	{
		super( "pex" );
		setAliases( "perms" );
		
		register( new GroupCommands() );
		register( new PromotionCommands() );
		register( new UserCommands() );
		register( new UtilityCommands() );
		register( new ReferenceCommands() );
	}
}
