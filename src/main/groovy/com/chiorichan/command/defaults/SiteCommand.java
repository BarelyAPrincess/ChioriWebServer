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

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;
import com.chiorichan.framework.Site;

public class SiteCommand extends VanillaCommand
{
	// private static final List<String> WHITELIST_SUBCOMMANDS = ImmutableList.of( "add", "remove", "on", "off", "list", "reload" );
	
	public SiteCommand()
	{
		super( "site" );
		this.description = "Manages the sites that are registered on this server.";
		this.usageMessage = "site (add|remove) <siteId> <type>\nsite (reload|list)";
		this.setPermission( "chiori.command.site.reload;chiori.command.site.list;chiori.command.site.add;chiori.command.site.remove" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length == 1 )
		{
			if ( args[0].equalsIgnoreCase( "reload" ) )
			{
				if ( badPerm( sender, "reload" ) )
					return true;
				
				Loader.getSiteManager().reload();
				Command.broadcastCommandMessage( sender, "Reloaded sites from file and database" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "list" ) )
			{
				if ( badPerm( sender, "list" ) )
					return true;
				
				StringBuilder result = new StringBuilder();
				for ( Site s : Loader.getSiteManager().getSites() )
				{
					if ( result.length() > 0 )
						result.append( ", " );
					
					result.append( "(" + s.getSiteId() + "/" + s.getDomain() + ") " + s.getTitle() );
				}
				
				sender.sendMessage( "Sites Loaded: " + result.toString() );
				return true;
			}
		}
		else if ( args.length == 2 )
		{
			if ( args[0].equalsIgnoreCase( "remove" ) )
			{
				if ( badPerm( sender, "remove" ) )
					return true;
				
				sender.sendMessage( ChatColor.AQUA + Loader.getSiteManager().remove( args[1] ) );
				
				return true;
			}
		}
		else if ( args.length == 3 )
		{
			if ( args[0].equalsIgnoreCase( "add" ) )
			{
				if ( badPerm( sender, "add" ) )
					return true;
				
				sender.sendMessage( ChatColor.AQUA + Loader.getSiteManager().add( args[1], args[2] ) );
				
				return true;
			}
		}
		
		sender.sendMessage( ChatColor.RED + "Correct command usage:\n" + usageMessage );
		return false;
	}
	
	private boolean badPerm( SentientHandler sender, String perm )
	{
		if ( !sender.getSentient().hasPermission( "chiori.command.site." + perm ) )
		{
			sender.sendMessage( ChatColor.RED + "You do not have permission to perform this action." );
			return true;
		}
		
		return false;
	}
}
