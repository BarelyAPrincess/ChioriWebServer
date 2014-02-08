package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;

public class PardonCommand extends VanillaCommand
{
	public PardonCommand()
	{
		super( "pardon" );
		this.description = "Allows the specified user to use this server";
		this.usageMessage = "/pardon <user>";
		this.setPermission( "chiori.command.unban.user" );
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
}
