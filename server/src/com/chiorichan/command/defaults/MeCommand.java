package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;

public class MeCommand extends VanillaCommand
{
	public MeCommand()
	{
		super( "me" );
		this.description = "Performs the specified action in chat";
		this.usageMessage = "/me <action>";
		this.setPermission( "chiori.command.me" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		if ( args.length < 1 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		StringBuilder message = new StringBuilder();
		message.append( sender.getSentient().getName() );
		
		for ( String arg : args )
		{
			message.append( " " );
			message.append( arg );
		}
		
		Loader.getPluginManager().broadcastMessage( "* " + message.toString() );
		
		return true;
	}
}
