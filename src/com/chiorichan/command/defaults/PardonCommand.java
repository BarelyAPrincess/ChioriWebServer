package com.chiorichan.command.defaults;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.ImmutableList;

public class PardonCommand extends VanillaCommand
{
	public PardonCommand()
	{
		super( "pardon" );
		this.description = "Allows the specified user to use this server";
		this.usageMessage = "/pardon <user>";
		this.setPermission( "bukkit.command.unban.user" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length != 1 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		Loader.getInstance().getOfflineUser( args[0] ).setBanned( false );
		Command.broadcastCommandMessage( sender, "Pardoned " + args[0] );
		return true;
	}
	
	@Override
	public List<String> tabComplete( CommandSender sender, String alias, String[] args ) throws IllegalArgumentException
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( args, "Arguments cannot be null" );
		Validate.notNull( alias, "Alias cannot be null" );
		
		if ( args.length == 1 )
		{
			List<String> completions = new ArrayList<String>();
			for ( User user : Loader.getInstance().getBannedUsers() )
			{
				String name = user.getName();
				if ( StringUtil.startsWithIgnoreCase( name, args[0] ) )
				{
					completions.add( name );
				}
			}
			return completions;
		}
		return ImmutableList.of();
	}
}
