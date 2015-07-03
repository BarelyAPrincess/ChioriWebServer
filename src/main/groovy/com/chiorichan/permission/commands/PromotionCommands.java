/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission.commands;

import java.util.Map;

import com.chiorichan.ConsoleColor;
import com.chiorichan.permission.PermissibleEntity;
import com.chiorichan.permission.PermissibleGroup;
import com.chiorichan.permission.PermissionManager;
import com.chiorichan.permission.lang.RankingException;
import com.chiorichan.terminal.TerminalEntity;
import com.chiorichan.terminal.commands.advanced.CommandHandler;

public class PromotionCommands extends PermissionsCommand
{
	@CommandHandler( name = "pex", syntax = "demote <user> [ladder]", description = "Demotes <user> to previous group or [ladder]", isPrimary = true )
	public void demoteUser( TerminalEntity sender, Map<String, String> args )
	{
		String userName = autoCompleteAccount( args.get( "user" ) );
		PermissibleEntity user = PermissionManager.INSTANCE.getEntity( userName );
		
		if ( user == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Specified user \"" + args.get( "user" ) + "\" not found!" );
			return;
		}
		
		String ladder = "default";
		
		if ( args.containsKey( "ladder" ) )
			ladder = args.get( "ladder" );
		
		PermissibleEntity demoter = sender.getEntity();
		String demoterName = sender.getDisplayName();
		
		// TODO Get reference based on connection method, e.g., Telnet Query
		if ( demoter == null || !demoter.checkPermission( "permissions.user.demote." + ladder ).isTrue() )
		{
			sender.sendMessage( ConsoleColor.RED + "You don't have enough permissions to demote on this ladder" );
			return;
		}
		
		try
		{
			PermissibleGroup targetGroup = user.demote( demoter, args.get( "ladder" ) );
			
			informEntity( user.getId(), "You have been demoted on " + targetGroup.getRankLadder() + " ladder to " + targetGroup.getId() + " group" );
			sender.sendMessage( "User " + user.getId() + " demoted to " + targetGroup.getId() + " group" );
			PermissionManager.getLogger().info( "User " + user.getId() + " has been demoted to " + targetGroup.getId() + " group on " + targetGroup.getRankLadder() + " ladder by " + demoterName );
		}
		catch ( RankingException e )
		{
			sender.sendMessage( ConsoleColor.RED + "Demotion error: " + e.getMessage() );
			PermissionManager.getLogger().severe( "Ranking Error (" + demoterName + " demotes " + e.getTarget().getId() + "): " + e.getMessage() );
		}
	}
	
	@CommandHandler( name = "demote", syntax = "<user>", description = "Demotes <user> to previous group", isPrimary = true, permission = "permissions.user.rank.demote" )
	public void demoteUserAlias( TerminalEntity sender, Map<String, String> args )
	{
		demoteUser( sender, args );
	}
	
	@CommandHandler( name = "pex", syntax = "promote <user> [ladder]", description = "Promotes <user> to next group on [ladder]", isPrimary = true )
	public void promoteUser( TerminalEntity sender, Map<String, String> args )
	{
		String userName = autoCompleteAccount( args.get( "user" ) );
		PermissibleEntity user = PermissionManager.INSTANCE.getEntity( userName );
		
		if ( user == null )
		{
			sender.sendMessage( "Specified user \"" + args.get( "user" ) + "\" not found!" );
			return;
		}
		
		String ladder = "default";
		
		if ( args.containsKey( "ladder" ) )
			ladder = args.get( "ladder" );
		
		PermissibleEntity promoter = sender.getEntity();
		String promoterName = sender.getDisplayName();
		
		// TODO Get reference based on connection method, e.g., Telnet Query
		if ( promoter == null || !promoter.checkPermission( "permissions.user.demote." + ladder ).isTrue() )
		{
			sender.sendMessage( ConsoleColor.RED + "You don't have enough permissions to demote on this ladder" );
			return;
		}
		
		try
		{
			PermissibleGroup targetGroup = user.promote( promoter, ladder );
			
			informEntity( user.getId(), "You have been promoted on " + targetGroup.getRankLadder() + " ladder to " + targetGroup.getId() + " group" );
			sender.sendMessage( "User " + user.getId() + " promoted to " + targetGroup.getId() + " group" );
			PermissionManager.getLogger().info( "User " + user.getId() + " has been promoted to " + targetGroup.getId() + " group on " + targetGroup.getRankLadder() + " ladder by " + promoterName );
		}
		catch ( RankingException e )
		{
			sender.sendMessage( ConsoleColor.RED + "Promotion error: " + e.getMessage() );
			PermissionManager.getLogger().severe( "Ranking Error (" + promoterName + " > " + e.getTarget().getId() + "): " + e.getMessage() );
		}
	}
	
	@CommandHandler( name = "promote", syntax = "<user>", description = "Promotes <user> to next group", isPrimary = true, permission = "permissions.user.rank.promote" )
	public void promoteUserAlias( TerminalEntity sender, Map<String, String> args )
	{
		promoteUser( sender, args );
	}
	
	@CommandHandler( name = "pex", syntax = "group <group> rank [rank] [ladder]", description = "Get or set <group> [rank] [ladder]", isPrimary = true, permission = "permissions.groups.rank.<group>" )
	public void rankGroup( TerminalEntity sender, Map<String, String> args )
	{
		String groupName = this.autoCompleteGroupName( args.get( "group" ) );
		
		PermissibleGroup group = PermissionManager.INSTANCE.getGroup( groupName );
		
		if ( group == null )
		{
			sender.sendMessage( ConsoleColor.RED + "Group \"" + groupName + "\" not found" );
			return;
		}
		
		if ( args.get( "rank" ) != null )
		{
			String newRank = args.get( "rank" ).trim();
			
			try
			{
				group.setRank( Integer.parseInt( newRank ) );
			}
			catch ( NumberFormatException e )
			{
				sender.sendMessage( "Wrong rank. Make sure it's number." );
			}
			
			if ( args.containsKey( "ladder" ) )
				group.setRankLadder( args.get( "ladder" ) );
		}
		
		int rank = group.getRank();
		
		if ( rank > 0 )
			sender.sendMessage( "Group " + group.getId() + " rank is " + rank + " (ladder = " + group.getRankLadder() + ")" );
		else
			sender.sendMessage( "Group " + group.getId() + " is unranked" );
	}
}
