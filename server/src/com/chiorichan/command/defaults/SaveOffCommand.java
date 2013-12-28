package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.google.common.collect.ImmutableList;

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
		
		//for ( World world : Main.getWorlds() )
		//{
			//world.setAutoSave( false );
		//}
		
		Command.broadcastCommandMessage( sender, "Disabled level saving.." );
		return true;
	}
}
