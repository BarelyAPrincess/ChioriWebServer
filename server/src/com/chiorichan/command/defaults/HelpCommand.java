package com.chiorichan.command.defaults;

import java.util.Arrays;

import com.chiorichan.ChatColor;
import com.chiorichan.account.bases.SentientHandler;

public class HelpCommand extends VanillaCommand
{
	public HelpCommand()
	{
		super( "help" );
		this.description = "Prints out the help map";
		this.usageMessage = "help";
		this.setPermission( "chiori.command.help" );
		this.setAliases( Arrays.asList( "?" ) );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		sender.sendMessage( ChatColor.RED + "Not Implemented!" );
		
		return true;
	}
}
