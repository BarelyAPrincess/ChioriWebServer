package com.chiorichan.command.defaults;

import java.util.List;

import com.chiorichan.command.Command;

public abstract class ChioriCommand extends Command
{
	protected ChioriCommand(String name)
	{
		super( name );
	}
	
	protected ChioriCommand(String name, String description, String usageMessage, List<String> aliases)
	{
		super( name, description, usageMessage, aliases );
	}
}
