package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class ReloadCommand extends ChioriCommand
{
	public ReloadCommand(String name)
	{
		super( name );
		this.description = "Reloads the server configuration and plugins";
		this.usageMessage = "/reload";
		this.setPermission( "chiori.command.reload" );
		this.setAliases( Arrays.asList( "rl" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		
		Loader.getInstance().reload();
		Command.broadcastCommandMessage( sender, ChatColor.GREEN + "Reload complete." );
		
		return true;
	}
}
