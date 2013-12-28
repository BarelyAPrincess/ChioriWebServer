package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.google.common.collect.ImmutableList;

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
		
		//for ( World world : Bukkit.getWorlds() )
		//{
			//world.setAutoSave( true );
		//}
		
		Command.broadcastCommandMessage( sender, "Enabled level saving.." );
		return true;
	}
}
