package com.chiorichan.command.defaults;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.ImmutableList;

public class BanListCommand extends VanillaCommand
{
	private static final List<String> BANLIST_TYPES = ImmutableList.of( "ips", "Users" );
	
	public BanListCommand()
	{
		super( "banlist" );
		this.description = "View all Users banned from this server";
		this.usageMessage = "/banlist [ips|Users]";
		this.setPermission( "chiori.command.ban.list" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		// TODO: ips support
		StringBuilder message = new StringBuilder();
		User[] banlist = Loader.getInstance().getBannedUsers().toArray( new User[0] );
		
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
