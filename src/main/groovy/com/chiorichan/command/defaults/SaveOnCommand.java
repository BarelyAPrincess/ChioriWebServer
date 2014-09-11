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

import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;
import com.chiorichan.framework.Site;

public class SaveOnCommand extends VanillaCommand
{
	public SaveOnCommand()
	{
		super( "save-on" );
		this.description = "Enables server autosaving";
		this.usageMessage = "/save-on";
		this.setPermission( "chiori.command.save.enable" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		for ( Site site : Loader.getSiteManager().getSites() )
		{
			site.setAutoSave( true );
		}
		
		Command.broadcastCommandMessage( sender, "Enabled auto saving..." );
		return true;
	}
}
