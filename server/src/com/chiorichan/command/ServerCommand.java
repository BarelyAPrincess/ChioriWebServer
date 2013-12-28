package com.chiorichan.command;

public class ServerCommand
{
	/** The command string. */
	public final String command;
	public final CommandSender sender;
	
	public ServerCommand(String par1Str, CommandSender par2ICommandSender)
	{
		this.command = par1Str;
		this.sender = par2ICommandSender;
	}
}
