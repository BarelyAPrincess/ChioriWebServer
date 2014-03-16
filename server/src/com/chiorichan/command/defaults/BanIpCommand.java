package com.chiorichan.command.defaults;

import java.util.regex.Pattern;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

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
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
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
			User User = Loader.getInstance().getUser( args[0] );
			
			if ( User == null )
			{
				sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
				return false;
			}
			
			processIPBan( User.getAddress(), sender );
		}
		
		return true;
	}
	
	private void processIPBan( String ip, CommandSender sender )
	{
		// TODO: Kick on ban
		Loader.getInstance().banIP( ip );
		
		Command.broadcastCommandMessage( sender, "Banned IP Address " + ip );
	}
}