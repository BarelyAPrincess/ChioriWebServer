package com.chiorichan.command.defaults;

import com.chiorichan.account.bases.SentientHandler;

public class ColorsCommand extends VanillaCommand
{
	public ColorsCommand()
	{
		super( "colors" );
		this.description = "Prints a list of colors that can be used in this console.";
		this.usageMessage = "colors";
		this.setPermission( "chiori.command.colors" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		
		sender.sendMessage( "&l&d&oTo use any of these just type & (amperstamp) folowed by the color/format code." );
		sender.sendMessage( "" );
		sender.sendMessage( "&00 - Black &11 - Dark Blue &22 - Dark Green &33 - Dark Aqua &44 - Dark Red &55 - Dark Purple &66 - Gold &77 - Gray &88 - Dark Gray &99 - Indigo" );
		sender.sendMessage( "&aa - Green &bb - Aqua &cc - Red &dd - Pink &ee - Yellow &ff - White &r&mm - Strike Through&r &nn - Underlined&r &ll - Bold&r &kk - Random&r &oo - Italic" );
		sender.sendMessage( "" );
		sender.sendMessage( "&l&4&oJust keep in mind that some of these color/format codes are not supported by all terminals. If your have any problems check your terminal type." );
		
		return true;
	}
}
