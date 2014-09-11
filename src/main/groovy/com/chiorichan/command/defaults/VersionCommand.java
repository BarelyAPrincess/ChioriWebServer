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

import java.util.Arrays;
import java.util.List;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.plugin.Plugin;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.util.Versioning;

public class VersionCommand extends ChioriCommand
{
	public VersionCommand(String name)
	{
		super( name );
		
		this.description = "Gets the version of this server including any plugins in use";
		this.usageMessage = "/version [plugin name]";
		this.setPermission( "chiori.command.version" );
		this.setAliases( Arrays.asList( "ver", "about" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length == 0 )
		{
			sender.sendMessage( "This server is running " + Versioning.getProduct() + " version " + Versioning.getVersion() );
			sender.sendMessage( Versioning.getCopyright() );
		}
		else
		{
			StringBuilder name = new StringBuilder();
			
			for ( String arg : args )
			{
				if ( name.length() > 0 )
				{
					name.append( ' ' );
				}
				
				name.append( arg );
			}
			
			String pluginName = name.toString();
			Plugin exactPlugin = Loader.getPluginManager().getPlugin( pluginName );
			if ( exactPlugin != null )
			{
				describeToSender( exactPlugin, sender );
				return true;
			}
			
			boolean found = false;
			pluginName = pluginName.toLowerCase();
			for ( Plugin plugin : Loader.getPluginManager().getPlugins() )
			{
				if ( plugin.getName().toLowerCase().contains( pluginName ) )
				{
					describeToSender( plugin, sender );
					found = true;
				}
			}
			
			if ( !found )
			{
				sender.sendMessage( "This server is not running any plugin by that name." );
				sender.sendMessage( "Use /plugins to get a list of plugins." );
			}
		}
		return true;
	}
	
	private void describeToSender( Plugin plugin, SentientHandler sender )
	{
		PluginDescriptionFile desc = plugin.getDescription();
		sender.sendMessage( ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + ChatColor.GREEN + desc.getVersion() );
		
		if ( desc.getDescription() != null )
		{
			sender.sendMessage( desc.getDescription() );
		}
		
		if ( desc.getWebsite() != null )
		{
			sender.sendMessage( "Website: " + ChatColor.GREEN + desc.getWebsite() );
		}
		
		if ( !desc.getAuthors().isEmpty() )
		{
			if ( desc.getAuthors().size() == 1 )
			{
				sender.sendMessage( "Author: " + getAuthors( desc ) );
			}
			else
			{
				sender.sendMessage( "Authors: " + getAuthors( desc ) );
			}
		}
	}
	
	private String getAuthors( final PluginDescriptionFile desc )
	{
		StringBuilder result = new StringBuilder();
		List<String> authors = desc.getAuthors();
		
		for ( int i = 0; i < authors.size(); i++ )
		{
			if ( result.length() > 0 )
			{
				result.append( ChatColor.WHITE );
				
				if ( i < authors.size() - 1 )
				{
					result.append( ", " );
				}
				else
				{
					result.append( " and " );
				}
			}
			
			result.append( ChatColor.GREEN );
			result.append( authors.get( i ) );
		}
		
		return result.toString();
	}
}
