package com.chiorichan.command.defaults;

import java.util.logging.Handler;

import com.chiorichan.ChatColor;
import com.chiorichan.ConsoleLogFormatter;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;

public class SecretCommand extends VanillaCommand
{
	public SecretCommand()
	{
		super( "secret" );
		this.description = "Top Secret! This command is to only be used by our TOP SECRET PEOPLE!";
		this.usageMessage = "secret";
		this.setPermission( "chiori.command.secret" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.RED + description );
			return false;
		}
		
		switch ( args[0].toLowerCase() )
		{
			case "logdebugon":
				Handler[] var1 = Loader.getLogger().getLogger().getHandlers();
				
				for ( Handler var2 : var1 )
				{
					if ( var2 != null && var2.getFormatter() instanceof ConsoleLogFormatter )
					{
						( (ConsoleLogFormatter) var2.getFormatter() ).debugMode = true;
						break;
					}
				}
				break;
			case "logdebugoff":
				Handler[] var11 = Loader.getLogger().getLogger().getHandlers();
				
				for ( Handler var2 : var11 )
				{
					if ( var2 != null && var2.getFormatter() instanceof ConsoleLogFormatter )
					{
						( (ConsoleLogFormatter) var2.getFormatter() ).debugMode = false;
						break;
					}
				}
				break;
		}
		
		sender.sendMessage( "The requested secret command has been executed. Let's hope you don't have enemies." );
		
		return true;
	}
}
