package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

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
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		Command.broadcastCommandMessage( sender, "Stopping the server.." );
		
		String reason = this.createString( args, 0 );
		if ( !reason.isEmpty() )
		{
			for ( User User : Loader.getInstance().getOnlineUsers() )
			{
				User.kick( reason );
			}
		}
		
		Loader.stop();
		
		return true;
	}
}
