package com.chiorichan.command.defaults;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.ImmutableList;

public class WhitelistCommand extends VanillaCommand
{
	private static final List<String> WHITELIST_SUBCOMMANDS = ImmutableList.of( "add", "remove", "on", "off", "list", "reload" );
	
	public WhitelistCommand()
	{
		super( "whitelist" );
		this.description = "Manages the list of Users allowed to use this server";
		this.usageMessage = "/whitelist (add|remove) <User>\n/whitelist (on|off|list|reload)";
		this.setPermission( "bukkit.command.whitelist.reload;bukkit.command.whitelist.enable;bukkit.command.whitelist.disable;bukkit.command.whitelist.list;bukkit.command.whitelist.add;bukkit.command.whitelist.remove" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length == 1 )
		{
			if ( args[0].equalsIgnoreCase( "reload" ) )
			{
				if ( badPerm( sender, "reload" ) )
					return true;
				
				Loader.getInstance().reloadWhitelist();
				Command.broadcastCommandMessage( sender, "Reloaded white-list from file" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "on" ) )
			{
				if ( badPerm( sender, "enable" ) )
					return true;
				
				Loader.getInstance().setWhitelist( true );
				Command.broadcastCommandMessage( sender, "Turned on white-listing" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "off" ) )
			{
				if ( badPerm( sender, "disable" ) )
					return true;
				
				Loader.getInstance().setWhitelist( false );
				Command.broadcastCommandMessage( sender, "Turned off white-listing" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "list" ) )
			{
				if ( badPerm( sender, "list" ) )
					return true;
				
				StringBuilder result = new StringBuilder();
				
				for ( User User : Loader.getInstance().getWhitelistedUsers() )
				{
					if ( result.length() > 0 )
					{
						result.append( ", " );
					}
					
					result.append( User.getName() );
				}
				
				sender.sendMessage( "White-listed Users: " + result.toString() );
				return true;
			}
		}
		else if ( args.length == 2 )
		{
			if ( args[0].equalsIgnoreCase( "add" ) )
			{
				if ( badPerm( sender, "add" ) )
					return true;
				
				Loader.getInstance().getOfflineUser( args[1] ).setWhitelisted( true );
				
				Command.broadcastCommandMessage( sender, "Added " + args[1] + " to white-list" );
				return true;
			}
			else if ( args[0].equalsIgnoreCase( "remove" ) )
			{
				if ( badPerm( sender, "remove" ) )
					return true;
				
				Loader.getInstance().getOfflineUser( args[1] ).setWhitelisted( false );
				
				Command.broadcastCommandMessage( sender, "Removed " + args[1] + " from white-list" );
				return true;
			}
		}
		
		sender.sendMessage( ChatColor.RED + "Correct command usage:\n" + usageMessage );
		return false;
	}
	
	private boolean badPerm( CommandSender sender, String perm )
	{
		if ( !sender.hasPermission( "bukkit.command.whitelist." + perm ) )
		{
			sender.sendMessage( ChatColor.RED + "You do not have permission to perform this action." );
			return true;
		}
		
		return false;
	}
	
	@Override
	public List<String> tabComplete( CommandSender sender, String alias, String[] args )
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( args, "Arguments cannot be null" );
		Validate.notNull( alias, "Alias cannot be null" );
		
		if ( args.length == 1 )
		{
			return StringUtil.copyPartialMatches( args[0], WHITELIST_SUBCOMMANDS, new ArrayList<String>( WHITELIST_SUBCOMMANDS.size() ) );
		}
		else if ( args.length == 2 )
		{
			if ( args[0].equalsIgnoreCase( "add" ) )
			{
				List<String> completions = new ArrayList<String>();
				for ( User User : Loader.getInstance().getOfflineUsers() )
				{
					String name = User.getName();
					if ( StringUtil.startsWithIgnoreCase( name, args[1] ) && !User.isWhitelisted() )
					{
						completions.add( name );
					}
				}
				return completions;
			}
			else if ( args[0].equalsIgnoreCase( "remove" ) )
			{
				List<String> completions = new ArrayList<String>();
				for ( User User : Loader.getInstance().getWhitelistedUsers() )
				{
					String name = User.getName();
					if ( StringUtil.startsWithIgnoreCase( name, args[1] ) )
					{
						completions.add( name );
					}
				}
				return completions;
			}
		}
		return ImmutableList.of();
	}
}
