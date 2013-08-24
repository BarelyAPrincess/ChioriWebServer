package com.chiorichan.command.defaults;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Main;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.google.common.collect.ImmutableList;

public class BanIpCommand extends VanillaCommand
{
	public static final Pattern ipValidity = Pattern.compile( "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$" );
	
	public BanIpCommand()
	{
		super( "ban-ip" );
		this.description = "Prevents the specified IP address from using this server";
		this.usageMessage = "/ban-ip <address|User> [reason ...]";
		this.setPermission( "bukkit.command.ban.ip" );
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
			User User = Main.getInstance().getUser( args[0] );
			
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
		Main.getInstance().banIP( ip );
		
		Command.broadcastCommandMessage( sender, "Banned IP Address " + ip );
	}
	
	@Override
	public List<String> tabComplete( CommandSender sender, String alias, String[] args ) throws IllegalArgumentException
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( args, "Arguments cannot be null" );
		Validate.notNull( alias, "Alias cannot be null" );
		
		if ( args.length == 1 )
		{
			return super.tabComplete( sender, alias, args );
		}
		return ImmutableList.of();
	}
}
