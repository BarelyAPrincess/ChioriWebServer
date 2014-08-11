package com.chiorichan.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.defaults.BanCommand;
import com.chiorichan.command.defaults.BanIpCommand;
import com.chiorichan.command.defaults.BanListCommand;
import com.chiorichan.command.defaults.ColorsCommand;
import com.chiorichan.command.defaults.DeopCommand;
import com.chiorichan.command.defaults.EchoCommand;
import com.chiorichan.command.defaults.HelpCommand;
import com.chiorichan.command.defaults.KickCommand;
import com.chiorichan.command.defaults.ListCommand;
import com.chiorichan.command.defaults.MeCommand;
import com.chiorichan.command.defaults.OpCommand;
import com.chiorichan.command.defaults.PardonCommand;
import com.chiorichan.command.defaults.PardonIpCommand;
import com.chiorichan.command.defaults.PingCommand;
import com.chiorichan.command.defaults.PluginsCommand;
import com.chiorichan.command.defaults.ReloadCommand;
import com.chiorichan.command.defaults.SaveCommand;
import com.chiorichan.command.defaults.SaveOffCommand;
import com.chiorichan.command.defaults.SaveOnCommand;
import com.chiorichan.command.defaults.SayCommand;
import com.chiorichan.command.defaults.SecretCommand;
import com.chiorichan.command.defaults.StopCommand;
import com.chiorichan.command.defaults.SuCommand;
import com.chiorichan.command.defaults.TellCommand;
import com.chiorichan.command.defaults.UpdateCommand;
import com.chiorichan.command.defaults.VanillaCommand;
import com.chiorichan.command.defaults.VersionCommand;
import com.chiorichan.command.defaults.WhitelistCommand;
import com.chiorichan.command.network.ConnectCommand;
import com.chiorichan.command.network.LoginCommand;
import com.chiorichan.command.network.SendCommand;

public class CommandMap
{
	private static final Pattern PATTERN_ON_SPACE = Pattern.compile( " ", Pattern.LITERAL );
	protected final Map<String, Command> knownCommands = new HashMap<String, Command>();
	protected final Set<String> aliases = new HashSet<String>();
	protected static final Set<VanillaCommand> fallbackCommands = new HashSet<VanillaCommand>();
	
	static
	{
		fallbackCommands.add( new ListCommand() );
		fallbackCommands.add( new OpCommand() );
		fallbackCommands.add( new SuCommand() );
		fallbackCommands.add( new ColorsCommand() );
		fallbackCommands.add( new DeopCommand() );
		fallbackCommands.add( new BanIpCommand() );
		fallbackCommands.add( new PardonIpCommand() );
		fallbackCommands.add( new BanCommand() );
		fallbackCommands.add( new PardonCommand() );
		fallbackCommands.add( new KickCommand() );
		fallbackCommands.add( new SayCommand() );
		fallbackCommands.add( new SecretCommand() );
		fallbackCommands.add( new HelpCommand() );
		fallbackCommands.add( new WhitelistCommand() );
		fallbackCommands.add( new TellCommand() );
		fallbackCommands.add( new MeCommand() );
		fallbackCommands.add( new BanListCommand() );
	}
	
	public CommandMap()
	{
		setDefaultCommands();
	}
	
	private void setDefaultCommands()
	{
		register( "chiori", new SaveCommand() );
		register( "chiori", new SaveOnCommand() );
		register( "chiori", new SaveOffCommand() );
		register( "chiori", new StopCommand() );
		register( "chiori", new PingCommand() );
		register( "chiori", new EchoCommand() );
		register( "chiori", new VersionCommand( "version" ) );
		register( "chiori", new UpdateCommand() );
		register( "chiori", new ReloadCommand( "reload" ) );
		register( "chiori", new PluginsCommand( "plugins" ) );
		
		// XXX Client Commands
		register( "chiori", new SendCommand() );
		register( "chiori", new ConnectCommand() );
		register( "chiori", new LoginCommand() );
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void registerAll( String fallbackPrefix, List<Command> commands )
	{
		if ( commands != null )
		{
			for ( Command c : commands )
			{
				register( fallbackPrefix, c );
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean register( String fallbackPrefix, Command command )
	{
		return register( command.getName(), fallbackPrefix, command );
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean register( String label, String fallbackPrefix, Command command )
	{
		boolean registeredPassedLabel = register( label, fallbackPrefix, command, false );
		
		Iterator<String> iterator = command.getAliases().iterator();
		while ( iterator.hasNext() )
		{
			if ( !register( iterator.next(), fallbackPrefix, command, true ) )
			{
				iterator.remove();
			}
		}
		
		// Register to us so further updates of the commands label and aliases are postponed until its reregistered
		command.register( this );
		
		return registeredPassedLabel;
	}
	
	/**
	 * Registers a command with the given name is possible, otherwise uses fallbackPrefix to create a unique name if its
	 * not an alias
	 * 
	 * @param label
	 *             the name of the command, without the '/'-prefix.
	 * @param fallbackPrefix
	 *             a prefix which is prepended to the command with a ':' one or more times to make the command unique
	 * @param command
	 *             the command to register
	 * @return true if command was registered with the passed in label, false otherwise. If isAlias was true a return of
	 *         false indicates no command was registerd If isAlias was false a return of false indicates the
	 *         fallbackPrefix was used one or more times to create a unique name for the command
	 */
	private synchronized boolean register( String label, String fallbackPrefix, Command command, boolean isAlias )
	{
		String lowerLabel = label.trim().toLowerCase();
		
		if ( isAlias && knownCommands.containsKey( lowerLabel ) )
		{
			// Request is for an alias and it conflicts with a existing command or previous alias ignore it
			// Note: This will mean it gets removed from the commands list of active aliases
			return false;
		}
		
		String lowerPrefix = fallbackPrefix.trim().toLowerCase();
		boolean registerdPassedLabel = true;
		
		// If the command exists but is an alias we overwrite it, otherwise we rename it based on the fallbackPrefix
		while ( knownCommands.containsKey( lowerLabel ) && !aliases.contains( lowerLabel ) )
		{
			lowerLabel = lowerPrefix + ":" + lowerLabel;
			registerdPassedLabel = false;
		}
		
		if ( isAlias )
		{
			aliases.add( lowerLabel );
		}
		else
		{
			// Ensure lowerLabel isn't listed as a alias anymore and update the commands registered name
			aliases.remove( lowerLabel );
			command.setLabel( lowerLabel );
		}
		knownCommands.put( lowerLabel, command );
		
		return registerdPassedLabel;
	}
	
	protected Command getFallback( String label )
	{
		for ( VanillaCommand cmd : fallbackCommands )
		{
			if ( cmd.matches( label ) )
			{
				return cmd;
			}
		}
		
		return null;
	}
	
	public Set<VanillaCommand> getFallbackCommands()
	{
		return Collections.unmodifiableSet( fallbackCommands );
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean dispatch( SentientHandler sender, String commandLine ) throws CommandException
	{
		String[] args = PATTERN_ON_SPACE.split( commandLine );
		
		if ( args.length == 0 )
		{
			return false;
		}
		
		String sentCommandLabel = args[0].toLowerCase();
		Command target = getCommand( sentCommandLabel );
		
		if ( target == null )
		{
			return false;
		}
		
		try
		{
			// Note: we don't return the result of target.execute as thats success / failure, we return handled (true) or
			// not handled (false)
			target.execute( sender, sentCommandLabel, Arrays.copyOfRange( args, 1, args.length ) );
		}
		catch ( CommandException ex )
		{
			throw ex;
		}
		catch ( Throwable ex )
		{
			throw new CommandException( "Unhandled exception executing '" + commandLine + "' in " + target, ex );
		}
		
		// return true as command was handled
		return true;
	}
	
	public synchronized void clearCommands()
	{
		for ( Map.Entry<String, Command> entry : knownCommands.entrySet() )
		{
			entry.getValue().unregister( this );
		}
		knownCommands.clear();
		aliases.clear();
		setDefaultCommands();
	}
	
	public Command getCommand( String name )
	{
		Command target = knownCommands.get( name.toLowerCase() );
		if ( target == null )
		{
			target = getFallback( name );
		}
		return target;
	}
	
	public Collection<Command> getCommands()
	{
		return knownCommands.values();
	}
	
	public void registerServerAliases()
	{
		Map<String, String[]> values = Loader.getPluginManager().getCommandAliases();
		
		for ( String alias : values.keySet() )
		{
			String[] targetNames = values.get( alias );
			List<Command> targets = new ArrayList<Command>();
			StringBuilder bad = new StringBuilder();
			
			for ( String name : targetNames )
			{
				Command command = getCommand( name );
				
				if ( command == null )
				{
					if ( bad.length() > 0 )
					{
						bad.append( ", " );
					}
					bad.append( name );
				}
				else
				{
					targets.add( command );
				}
			}
			
			// We register these as commands so they have absolute priority.
			
			if ( targets.size() > 0 )
			{
				knownCommands.put( alias.toLowerCase(), new MultipleCommandAlias( alias.toLowerCase(), targets.toArray( new Command[0] ) ) );
			}
			else
			{
				knownCommands.remove( alias.toLowerCase() );
			}
			
			if ( bad.length() > 0 )
			{
				Loader.getLogger().warning( "The following command(s) could not be aliased under '" + alias + "' because they do not exist: " + bad );
			}
		}
	}
}
