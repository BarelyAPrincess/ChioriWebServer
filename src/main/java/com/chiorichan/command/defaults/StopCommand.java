package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class StopCommand extends VanillaCommand
{
	public StopCommand()
	{
		super( "stop" );
		this.description = "Stops the server with optional reason";
		this.usageMessage = "/stop [reason]";
		this.setPermission( "chiori.command.stop" );
		
		this.setAliases( Arrays.asList( "exit", "quit" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		Command.broadcastCommandMessage( sender, "Stopping the server..." );
		
		Loader.gracefullyShutdownServer( this.createString( args, 0 ) );
		
		return true;
	}
}
