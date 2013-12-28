package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

public class SayCommand extends VanillaCommand
{
	public SayCommand()
	{
		super( "say" );
		this.description = "Broadcasts the given message as the console";
		this.usageMessage = "/say <message>";
		this.setPermission( "chiori.command.say" );
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
		
		if ( sender instanceof User )
		{
			Loader.getLogger().info( "[" + sender.getName() + "] " + message );
		}
		
		Loader.getInstance().broadcastMessage( ChatColor.LIGHT_PURPLE + "[Server] " + message );
		
		return true;
	}
}
