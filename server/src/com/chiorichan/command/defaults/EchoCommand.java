package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.command.CommandSender;

public class EchoCommand extends VanillaCommand
{
	public EchoCommand()
	{
		super( "echo" );
		this.description = "Repeats the given message to the user";
		this.usageMessage = "/echo <message>";
		this.setPermission( "chiori.command.echo" );
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
		
		StringBuilder message = new StringBuilder();
		if ( args.length > 0 )
		{
			message.append( args[0] );
			for ( int i = 1; i < args.length; i++ )
			{
				message.append( " " );
				message.append( args[i] );
			}
		}
		
		sender.sendMessage( message.toString() );
		
		return true;
	}
}
