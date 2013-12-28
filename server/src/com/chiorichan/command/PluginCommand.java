package com.chiorichan.command;

import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.plugin.Plugin;

/**
 * Represents a {@link Command} belonging to a plugin
 */
public final class PluginCommand extends Command implements PluginIdentifiableCommand
{
	private final Plugin owningPlugin;
	private CommandExecutor executor;
	
	protected PluginCommand(String name, Plugin owner)
	{
		super( name );
		this.executor = owner;
		this.owningPlugin = owner;
		this.usageMessage = "";
	}
	
	/**
	 * Executes the command, returning its success
	 * 
	 * @param sender
	 *           Source object which is executing this command
	 * @param commandLabel
	 *           The alias of the command used
	 * @param args
	 *           All arguments passed to the command, split via ' '
	 * @return true if the command was successful, otherwise false
	 */
	@Override
	public boolean execute( CommandSender sender, String commandLabel, String[] args )
	{
		boolean success = false;
		
		if ( !owningPlugin.isEnabled() )
		{
			return false;
		}
		
		if ( !testPermission( sender ) )
		{
			return true;
		}
		
		try
		{
			success = executor.onCommand( sender, this, commandLabel, args );
		}
		catch ( Throwable ex )
		{
			throw new CommandException( "Unhandled exception executing command '" + commandLabel + "' in plugin " + owningPlugin.getDescription().getFullName(), ex );
		}
		
		if ( !success && usageMessage.length() > 0 )
		{
			for ( String line : usageMessage.replace( "<command>", commandLabel ).split( "\n" ) )
			{
				sender.sendMessage( line );
			}
		}
		
		return success;
	}
	
	/**
	 * Sets the {@link CommandExecutor} to run when parsing this command
	 * 
	 * @param executor
	 *           New executor to run
	 */
	public void setExecutor( CommandExecutor executor )
	{
		this.executor = executor == null ? owningPlugin : executor;
	}
	
	/**
	 * Gets the {@link CommandExecutor} associated with this command
	 * 
	 * @return CommandExecutor object linked to this command
	 */
	public CommandExecutor getExecutor()
	{
		return executor;
	}
	
	/**
	 * Gets the owner of this PluginCommand
	 * 
	 * @return Plugin that owns this command
	 */
	public Plugin getPlugin()
	{
		return owningPlugin;
	}
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder( super.toString() );
		stringBuilder.deleteCharAt( stringBuilder.length() - 1 );
		stringBuilder.append( ", " ).append( owningPlugin.getDescription().getFullName() ).append( ')' );
		return stringBuilder.toString();
	}
}
