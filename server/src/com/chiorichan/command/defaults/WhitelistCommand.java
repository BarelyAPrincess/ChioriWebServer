package com.chiorichan.command.defaults;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

public class WhitelistCommand extends VanillaCommand
{
	//private static final List<String> WHITELIST_SUBCOMMANDS = ImmutableList.of( "add", "remove", "on", "off", "list", "reload" );
	
	public WhitelistCommand()
	{
		super( "whitelist" );
		this.description = "Manages the list of Users allowed to use this server";
		this.usageMessage = "/whitelist (add|remove) <User>\n/whitelist (on|off|list|reload)";
		this.setPermission( "chiori.command.whitelist.reload;chiori.command.whitelist.enable;chiori.command.whitelist.disable;chiori.command.whitelist.list;chiori.command.whitelist.add;chiori.command.whitelist.remove" );
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
}
