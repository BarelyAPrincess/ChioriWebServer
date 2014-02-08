package com.chiorichan.command.defaults;

import com.chiorichan.Console;
import com.chiorichan.command.CommandSender;

public class PingCommand extends VanillaCommand
{
	public PingCommand()
	{
		super( "ping" );
		this.description = "Responds to sender with PONG tick";
		this.usageMessage = "/ping";
		this.setPermission( "chiori.command.ping" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		sender.sendMessage( "Pong " + Console.currentTick );
		
		return true;
	}
}
