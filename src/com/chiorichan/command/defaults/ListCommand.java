package com.chiorichan.command.defaults;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Main;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;
import com.google.common.collect.ImmutableList;

public class ListCommand extends VanillaCommand
{
	public ListCommand()
	{
		super( "list" );
		this.description = "Lists all online users";
		this.usageMessage = "/list";
		this.setPermission( "bukkit.command.list" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		StringBuilder online = new StringBuilder();
		
		User[] users = Main.getInstance().getOnlineUsers();
		
		for ( User user : users )
		{
			// If a user is hidden from the sender don't show them in the list
			if ( sender instanceof User && !( (User) sender ).canSee( user ) )
				continue;
			
			if ( online.length() > 0 )
			{
				online.append( ", " );
			}
			
			online.append( user.getDisplayName() );
		}
		
		sender.sendMessage( "There are " + users.length + "/" + Main.getInstance().getMaxUsers() + " users online:\n" + online.toString() );
		
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
