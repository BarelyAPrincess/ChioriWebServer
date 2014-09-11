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

public class SaveCommand extends VanillaCommand
{
	public SaveCommand()
	{
		super( "save-all" );
		this.description = "Saves the server to disk";
		this.usageMessage = "/save-all";
		this.setPermission( "chiori.command.save.perform" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		Command.broadcastCommandMessage( sender, "Forcing save.." );
		
		Loader.getAccountsManager().saveAccounts();
		
		//for ( World world : Main.getInstance().getWorlds() )
		//{
//			world.save();
	//	}
		
		Command.broadcastCommandMessage( sender, "Save complete." );
		
		return true;
	}
}
