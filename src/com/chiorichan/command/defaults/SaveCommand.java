package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.google.common.collect.ImmutableList;

public class SaveCommand extends VanillaCommand
{
	public SaveCommand()
	{
		super( "save-all" );
		this.description = "Saves the server to disk";
		this.usageMessage = "/save-all";
		this.setPermission( "chiori.command.save.perform" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		Command.broadcastCommandMessage( sender, "Forcing save.." );
		
		Loader.getInstance().saveUsers();
		
		//for ( World world : Main.getInstance().getWorlds() )
		//{
//			world.save();
	//	}
		
		Command.broadcastCommandMessage( sender, "Save complete." );
		
		return true;
	}
}
