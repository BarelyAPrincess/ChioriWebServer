package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;

public class TellCommand extends VanillaCommand
{
	public TellCommand()
	{
		super( "tell" );
		this.description = "Sends a private message to the given user";
		this.usageMessage = "/tell <user> <message>";
		this.setPermission( "chiori.command.tell" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		if ( args.length < 2 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		Account user = Loader.getAccountsManager().getAccount( args[0] );
		
		// If a user is hidden from the sender pretend they are offline
		if ( user == null || ( sender instanceof Account && !( (Account) sender ).canSee( user ) ) )
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
			
			String result = ChatColor.GRAY + sender.getSentient().getName() + " whispers " + message;
			
			sender.sendMessage( "[" + sender.getSentient().getName() + "->" + user.getName() + "] " + message );
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
