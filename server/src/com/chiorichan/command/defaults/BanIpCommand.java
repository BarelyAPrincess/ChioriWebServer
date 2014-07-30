package com.chiorichan.command.defaults;

import java.util.regex.Pattern;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.account.bases.Account;
import com.chiorichan.account.bases.Sentient;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;

public class BanIpCommand extends VanillaCommand
{
	public static final Pattern ipValidity = Pattern.compile( "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$" );
	
	public BanIpCommand()
	{
		super( "ban-ip" );
		this.description = "Prevents the specified IP address from using this server";
		this.usageMessage = "/ban-ip <address|User> [reason ...]";
		this.setPermission( "chiori.command.ban.ip" );
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
		
		// TODO: Ban Reason support
		if ( ipValidity.matcher( args[0] ).matches() )
		{
			processIPBan( args[0], sender );
		}
		else
		{
			Account acct = Loader.getAccountsManager().getAccount( args[0] );
			
			if ( acct == null )
			{
				sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
				return false;
			}
			
			if ( acct.getAddress() != null )
				processIPBan( acct.getAddress(), sender );
			else
				sender.sendMessage( ChatColor.RED + "Sorry, there seems to be no IP Address associated with that account. Can only ban by IP if they connect using a TCP/IP connection." );
		}
		
		return true;
	}
	
	private void processIPBan( String ip, SentientHandler sender )
	{
		Loader.getAccountsManager().banIp( ip );
		Command.broadcastCommandMessage( sender, "Banned IP Address " + ip );
	}
}
