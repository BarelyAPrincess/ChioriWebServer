package com.chiorichan.command;

import com.chiorichan.account.bases.SentientHandler;

/**
 * Represents a command that delegates to one or more other commands
 */
public class MultipleCommandAlias extends Command
{
	private Command[] commands;
	
	public MultipleCommandAlias(String name, Command[] commands)
	{
		super( name );
		this.commands = commands;
	}
	
	public Command[] getCommands()
	{
		return commands;
	}
	
	@Override
	public boolean execute( SentientHandler sender, String commandLabel, String[] args )
	{
		boolean result = false;
		
		for ( Command command : commands )
		{
			result |= command.execute( sender, commandLabel, args );
		}
		
		return result;
	}
}
