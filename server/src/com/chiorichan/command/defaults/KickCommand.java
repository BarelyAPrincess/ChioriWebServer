package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.google.common.collect.ImmutableList;

public class KickCommand extends VanillaCommand
{
	public KickCommand()
	{
		super( "kick" );
		this.description = "Removes the specified user from the server";
		this.usageMessage = "/kick <user> [reason ...]";
		this.setPermission( "chiori.command.kick" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length < 1 || args[0].length() == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		User user = Loader.getInstance().getUserExact( args[0] );
		
		if ( user != null )
		{
			String reason = "Kicked by an operator.";
			
			if ( args.length > 1 )
			{
				reason = createString( args, 1 );
			}
			
			user.kick( reason );
			Command.broadcastCommandMessage( sender, "Kicked user " + user.getName() + ". With reason:\n" + reason );
		}
		else
		{
			sender.sendMessage( args[0] + " not found." );
		}
		
		return true;
	}
}
