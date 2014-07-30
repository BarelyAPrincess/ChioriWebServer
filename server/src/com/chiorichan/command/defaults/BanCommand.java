package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class BanCommand extends VanillaCommand
{
	public BanCommand()
	{
		super( "ban" );
		this.description = "Prevents the specified User from using this server";
		this.usageMessage = "/ban <User> [reason ...]";
		this.setPermission( "chiori.command.ban.User" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		// TODO: Ban Reason support
		Account acct = Loader.getAccountsManager().getAccount( args[0] );
		
		Loader.getAccountsManager().banId( acct.getAccountId() );
		
		if ( acct != null )
		{
			acct.kick( "Banned by admin." );
		}
		
		Command.broadcastCommandMessage( sender, "Banned User " + args[0] );
		return true;
	}
}
