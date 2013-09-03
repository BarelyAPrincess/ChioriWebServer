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

public class DeopCommand extends VanillaCommand
{
	public DeopCommand()
	{
		super( "deop" );
		this.description = "Takes the specified User's operator status";
		this.usageMessage = "/deop <User>";
		this.setPermission( "chiori.command.op.take" );
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
		user.setOp( false );
		
		if ( user instanceof User )
		{
			user.sendMessage( ChatColor.YELLOW + "You are no longer op!" );
		}
		
		Command.broadcastCommandMessage( sender, "De-opped " + args[0] );
		return true;
	}
}
