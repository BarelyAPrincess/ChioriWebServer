package com.chiorichan.command.defaults;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;
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
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender.getSentient() ) )
			return true;
		
		for ( Site site : Loader.getSiteManager().getSites() )
		{
			site.setAutoSave( false );
		}
		
		Command.broadcastCommandMessage( sender, "Disabled auto saving..." );
		return true;
	}
}
