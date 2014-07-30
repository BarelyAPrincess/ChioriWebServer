package com.chiorichan.command.defaults;

import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.bus.ConsoleBus;

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
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		
		sender.sendMessage( "Pong " + ConsoleBus.currentTick );
		
		return true;
	}
}
