package com.chiorichan.command.defaults;

import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.user.User;

public class ListCommand extends VanillaCommand
{
	public ListCommand()
	{
		super( "list" );
		this.description = "Lists all online users";
		this.usageMessage = "/list";
		this.setPermission( "chiori.command.list" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !testPermission( sender ) )
			return true;
		
		StringBuilder online = new StringBuilder();
		
		User[] users = Loader.getInstance().getOnlineUsers();
		
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
		
		sender.sendMessage( "There are " + users.length + "/" + Loader.getInstance().getMaxUsers() + " users online:\n" + online.toString() );
		
		return true;
	}
}
