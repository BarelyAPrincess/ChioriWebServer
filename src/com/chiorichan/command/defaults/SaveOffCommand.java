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
		this.setPermission( "Main.command.save.disable" );
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
	
	@Override
	public List<String> tabComplete( CommandSender sender, String alias, String[] args ) throws IllegalArgumentException
	{
		Validate.notNull( sender, "Sender cannot be null" );
		Validate.notNull( args, "Arguments cannot be null" );
		Validate.notNull( alias, "Alias cannot be null" );
		
		return ImmutableList.of();
	}
}
