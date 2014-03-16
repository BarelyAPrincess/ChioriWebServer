package com.chiorichan.command.defaults;

import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.framework.Site;

public class SaveOffCommand extends VanillaCommand
{
	public SaveOffCommand()
	{
		super( "save-off" );
		this.description = "Disables server autosaving";
		this.usageMessage = "/save-off";
		this.setPermission( "chiori.command.save.disable" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		for ( Site site : Loader.getPersistenceManager().getSiteManager().getSites() )
		{
			site.setAutoSave( false );
		}
		
		Command.broadcastCommandMessage( sender, "Disabled level saving.." );
		return true;
	}
}