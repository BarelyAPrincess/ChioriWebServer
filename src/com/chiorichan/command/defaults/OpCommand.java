package com.chiorichan.command.defaults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.chiorichan.util.StringUtil;
import com.google.common.collect.ImmutableList;

public class OpCommand extends VanillaCommand
{
	public OpCommand()
	{
		super( "op" );
		this.description = "Gives the specified user operator status";
		this.usageMessage = "/op <user>";
		this.setPermission( "bukkit.command.op.give" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length != 1 || args[0].length() == 0 )
		{
			sender.sendMessage( ChatColor.RED + "Usage: " + usageMessage );
			return false;
		}
		
		User user = Loader.getInstance().getOfflineUser( args[0] );
		user.setOp( true );
		
		Command.broadcastCommandMessage( sender, "Opped " + args[0] );
		return true;
	}
	
	@Override
	public List<String> tabComplete( CommandSender sender, String alias, String[] args ) throws IllegalArgumentException
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( args, "Arguments cannot be null" );
		Validate.notNull( alias, "Alias cannot be null" );
		
		if ( args.length == 1 )
		{
			if ( !( sender instanceof User ) )
			{
				return ImmutableList.of();
			}
			
			String lastWord = args[0];
			if ( lastWord.length() == 0 )
			{
				return ImmutableList.of();
			}
			
			User senderUser = (User) sender;
			
			ArrayList<String> matchedUsers = new ArrayList<String>();
			for ( User user : sender.getServer().getOnlineUsers() )
			{
				String name = user.getName();
				if ( !senderUser.canSee( user ) || user.isOp() )
				{
					continue;
				}
				if ( StringUtil.startsWithIgnoreCase( name, lastWord ) )
				{
					matchedUsers.add( name );
				}
			}
			
			Collections.sort( matchedUsers, String.CASE_INSENSITIVE_ORDER );
			return matchedUsers;
		}
		return ImmutableList.of();
	}
}
