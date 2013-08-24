package com.chiorichan.help;

import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;

/**
 * This is a help topic implementation for general topics registered in the help.yml file.
 */
public class CustomHelpTopic extends HelpTopic
{
	private final String permissionNode;
	
	public CustomHelpTopic(String name, String shortText, String fullText, String permissionNode)
	{
		this.permissionNode = permissionNode;
		this.name = name;
		this.shortText = shortText;
		this.fullText = shortText + "\n" + fullText;
	}
	
	public boolean canSee( CommandSender sender )
	{
		if ( sender instanceof ConsoleCommandSender )
		{
			return true;
		}
		
		if ( !permissionNode.equals( "" ) )
		{
			return sender.hasPermission( permissionNode );
		}
		else
		{
			return true;
		}
	}
}
