package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

public class OpCommand extends VanillaCommand
{
	public OpCommand()
	{
		super( "op" );
		this.description = "Gives the specified user operator status";
		this.usageMessage = "/op <user>";
		this.setPermission( "chiori.command.op.give" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length != 1 || args[0].length() == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		User user = Loader.getInstance().getOfflineUser( args[0] );
		
		if ( args[0].equals( "[console]" ) )
		{
			Loader.getConsole().setOp( true );
			Command.broadcastCommandMessage( sender, "Opped the Console User" );
			return true;
		}
		
		if ( user != null )
		{
			user.setOp( true );
			Command.broadcastCommandMessage( sender, "Opped " + args[0] );
		}
		else
		{
			Command.broadcastCommandMessage( sender, "There was a problem oping " + args[0] );
		}
		
		return true;
	}
}
