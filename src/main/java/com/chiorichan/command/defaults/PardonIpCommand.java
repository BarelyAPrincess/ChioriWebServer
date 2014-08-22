package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class PardonIpCommand extends VanillaCommand
{
	public PardonIpCommand()
	{
		super( "pardon-ip" );
		this.description = "Allows the specified IP address to use this server";
		this.usageMessage = "/pardon-ip <address>";
		this.setPermission( "chiori.command.unban.ip" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length != 1 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		if ( BanIpCommand.ipValidity.matcher( args[0] ).matches() )
		{
			Loader.getAccountsManager().unbanIp( args[0] );
			
			Command.broadcastCommandMessage( sender, "Pardoned ip " + args[0] );
		}
		else
		{
			sender.sendMessage( "Invalid ip" );
		}
		
		return true;
	}
}
