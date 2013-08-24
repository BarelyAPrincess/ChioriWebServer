package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Main;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

public class TellCommand extends VanillaCommand
{
	public TellCommand()
	{
		super( "tell" );
		this.description = "Sends a private message to the given user";
		this.usageMessage = "/tell <user> <message>";
		this.setPermission( "bukkit.command.tell" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length < 2 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		User user = Main.getInstance().getUserExact( args[0] );
		
		// If a user is hidden from the sender pretend they are offline
		if ( user == null || ( sender instanceof User && !( (User) sender ).canSee( user ) ) )
		{
			sender.sendMessage( "There's no user by that name online." );
		}
		else
		{
			StringBuilder message = new StringBuilder();
			
			for ( int i = 1; i < args.length; i++ )
			{
				if ( i > 1 )
					message.append( " " );
				message.append( args[i] );
			}
			
			String result = ChatColor.GRAY + sender.getName() + " whispers " + message;
			
			sender.sendMessage( "[" + sender.getName() + "->" + user.getName() + "] " + message );
			user.sendMessage( result );
		}
		
		return true;
	}
	
	@Override
	public boolean matches( String input )
	{
		return input.equalsIgnoreCase( "tell" ) || input.equalsIgnoreCase( "w" ) || input.equalsIgnoreCase( "msg" );
	}
}
