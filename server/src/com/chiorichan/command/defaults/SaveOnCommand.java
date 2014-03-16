package com.chiorichan.command.defaults;

import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.framework.Site;

public class SaveOnCommand extends VanillaCommand
{
	public SaveOnCommand()
	{
		super( "save-on" );
		this.description = "Enables server autosaving";
		this.usageMessage = "/save-on";
		this.setPermission( "chiori.command.save.enable" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		for ( Site site : Loader.getPersistenceManager().getSiteManager().getSites() )
		{
			site.setAutoSave( true );
		}
		
		Command.broadcastCommandMessage( sender, "Enabled level saving.." );
		return true;
	}
}