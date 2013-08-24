package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.ChatColor;
import com.chiorichan.Main;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;

public class ReloadCommand extends ChioriCommand
{
	public ReloadCommand(String name)
	{
		super( name );
		this.description = "Reloads the server configuration and plugins";
		this.usageMessage = "/reload";
		this.setPermission( "Main.command.reload" );
		this.setAliases( Arrays.asList( "rl" ) );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		Main.getInstance().reload();
		Command.broadcastCommandMessage( sender, ChatColor.GREEN + "Reload complete." );
		
		return true;
	}
}
