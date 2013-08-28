package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;

public class MeCommand extends VanillaCommand
{
	public MeCommand()
	{
		super( "me" );
		this.description = "Performs the specified action in chat";
		this.usageMessage = "/me <action>";
		this.setPermission( "bukkit.command.me" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length < 1 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		StringBuilder message = new StringBuilder();
		message.append( sender.getName() );
		
		for ( String arg : args )
		{
			message.append( " " );
			message.append( arg );
		}
		
		Loader.getInstance().broadcastMessage( "* " + message.toString() );
		
		return true;
	}
}
