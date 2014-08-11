package com.chiorichan.command.defaults;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;

public class BanListCommand extends VanillaCommand
{
	//private static final List<String> BANLIST_TYPES = ImmutableList.of( "ips", "Users" );
	
	public BanListCommand()
	{
		super( "banlist" );
		this.description = "View all Users banned from this server";
		this.usageMessage = "/banlist [ips|Users]";
		this.setPermission( "chiori.command.ban.list" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		// TODO: ips support
		StringBuilder message = new StringBuilder();
		Account[] banlist = Loader.getAccountsManager().getBannedAccounts().toArray( new Account[0] );
		
		for ( int x = 0; x < banlist.length; x++ )
		{
			if ( x != 0 )
			{
				if ( x == banlist.length - 1 )
				{
					message.append( " and " );
				}
				else
				{
					message.append( ", " );
				}
			}
			message.append( banlist[x].getName() );
		}
		
		sender.sendMessage( "There are " + banlist.length + " total banned Users:" );
		sender.sendMessage( message.toString() );
		return true;
	}
}
