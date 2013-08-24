package com.chiorichan.help;

import com.chiorichan.ChatColor;
import com.chiorichan.command.Command;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.command.MultipleCommandAlias;

/**
 * This is a help topic implementation for {@link MultipleCommandAlias} commands.
 */
public class MultipleCommandAliasHelpTopic extends HelpTopic
{
	
	private final MultipleCommandAlias alias;
	
	public MultipleCommandAliasHelpTopic(MultipleCommandAlias alias)
	{
		this.alias = alias;
		
		name = "/" + alias.getLabel();
		
		// Build short text
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < alias.getCommands().length; i++ )
		{
			if ( i != 0 )
			{
				sb.append( ChatColor.GOLD + " > " + ChatColor.WHITE );
			}
			sb.append( "/" );
			sb.append( alias.getCommands()[i].getLabel() );
		}
		shortText = sb.toString();
		
		// Build full text
		fullText = ChatColor.GOLD + "Alias for: " + ChatColor.WHITE + getShortText();
	}
	
	public boolean canSee( CommandSender sender )
	{
		if ( amendedPermission == null )
		{
			if ( sender instanceof ConsoleCommandSender )
			{
				return true;
			}
			
			for ( Command command : alias.getCommands() )
			{
				if ( !command.testPermissionSilent( sender ) )
				{
					return false;
				}
			}
			
			return true;
		}
		else
		{
			return sender.hasPermission( amendedPermission );
		}
	}
}
