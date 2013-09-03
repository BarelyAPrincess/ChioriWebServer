package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.google.common.collect.ImmutableList;

public class BanCommand extends VanillaCommand
{
	public BanCommand()
	{
		super( "ban" );
		this.description = "Prevents the specified User from using this server";
		this.usageMessage = "/ban <User> [reason ...]";
		this.setPermission( "chiori.command.ban.User" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		// TODO: Ban Reason support
		Loader.getInstance().getOfflineUser( args[0] ).setBanned( true );
		
		User User = Loader.getInstance().getUser( args[0] );
		if ( User != null )
		{
			User.kick( "Banned by admin." );
		}
		
		Command.broadcastCommandMessage( sender, "Banned User " + args[0] );
		return true;
	}
}
