package com.chiorichan.plugin.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.command.Command;
import com.chiorichan.command.PluginCommand;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.plugin.PluginBase;
import com.chiorichan.plugin.PluginDescriptionFile;
import com.chiorichan.plugin.PluginLoader;
import com.chiorichan.plugin.PluginLogger;

/**
 * Represents a Java plugin
 */
public abstract class JavaPlugin extends PluginBase
{
	private boolean isEnabled = false;
	private PluginLoader loader = null;
	private Loader server = null;
	private File file = null;
	private PluginDescriptionFile description = null;
	private File dataFolder = null;
	private ClassLoader classLoader = null;
	private boolean naggable = true;
	private FileConfiguration newConfig = null;
	private File configFile = null;
	private PluginLogger logger = null;
	
	public JavaPlugin()
	{
		final ClassLoader classLoader = this.getClass().getClassLoader();
		if ( !( classLoader instanceof PluginClassLoader ) )
		{
			throw new IllegalStateException( "JavaPlugin requires " + PluginClassLoader.class.getName() );
		}
		( (PluginClassLoader) classLoader ).initialize( this );
	}
	
	protected JavaPlugin(final JavaPluginLoader loader, final PluginDescriptionFile description, final File dataFolder, final File file)
	{
		final ClassLoader classLoader = this.getClass().getClassLoader();
		if ( classLoader instanceof PluginClassLoader )
		{
			throw new IllegalStateException( "Cannot use initialization constructor at runtime" );
		}
		init( loader, server, description, dataFolder, file, classLoader );
	}
	
	/**
	 * Returns the folder that the plugin data's files are located in. The
	 * folder may not yet exist.
	 * 
	 * @return The folder.
	 */
	public final File getDataFolder()
	{
		return dataFolder;
	}
	
	/**
	 * Gets the associated PluginLoader responsible for this plugin
	 * 
	 * @return PluginLoader that controls this plugin
	 */
	public final PluginLoader getPluginLoader()
	{
		return loader;
	}
	
	/**
	 * Returns the Server instance currently running this plugin
	 * 
	 * @return Server running this plugin
	 */
	public final Loader getServer()
	{
		return server;
	}
	
	/**
	 * Returns a value indicating whether or not this plugin is currently
	 * enabled
	 * 
	 * @return true if this plugin is enabled, otherwise false
	 */
	public final boolean isEnabled()
	{
		return isEnabled;
	}
	
	/**
	 * Returns the file which contains this plugin
	 * 
	 * @return File containing this plugin
	 */
	protected File getFile()
	{
		return file;
	}
	
	/**
	 * Returns the plugin.yaml file containing the details for this plugin
	 * 
	 * @return Contents of the plugin.yaml file
	 */
	public final PluginDescriptionFile getDescription()
	{
		return description;
	}
	
	public FileConfiguration getConfig()
	{
		if ( newConfig == null )
		{
			reloadConfig();
		}
		return newConfig;
	}
	
	public void reloadConfig()
	{
		newConfig = YamlConfiguration.loadConfiguration( configFile );
		
		InputStream defConfigStream = getResource( "config.yml" );
		if ( defConfigStream != null )
		{
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration( defConfigStream );
			
			newConfig.setDefaults( defConfig );
		}
	}
	
	public void saveConfig()
	{
		try
		{
			getConfig().save( configFile );
		}
		catch ( IOException ex )
		{
			logger.log( Level.SEVERE, "Could not save config to " + configFile, ex );
		}
	}
	
	public void saveDefaultConfig()
	{
		if ( !configFile.exists() )
		{
			saveResource( "config.yml", false );
		}
	}
	
	public void saveResource( String resourcePath, boolean replace )
	{
		if ( resourcePath == null || resourcePath.equals( "" ) )
		{
			throw new IllegalArgumentException( "ResourcePath cannot be null or empty" );
		}
		
		resourcePath = resourcePath.replace( '\\', '/' );
		InputStream in = getResource( resourcePath );
		if ( in == null )
		{
			throw new IllegalArgumentException( "The embedded resource '" + resourcePath + "' cannot be found in " + file );
		}
		
		File outFile = new File( dataFolder, resourcePath );
		int lastIndex = resourcePath.lastIndexOf( '/' );
		File outDir = new File( dataFolder, resourcePath.substring( 0, lastIndex >= 0 ? lastIndex : 0 ) );
		
		if ( !outDir.exists() )
		{
			outDir.mkdirs();
		}
		
		try
		{
			if ( !outFile.exists() || replace )
			{
				OutputStream out = new FileOutputStream( outFile );
				byte[] buf = new byte[1024];
				int len;
				while ( ( len = in.read( buf ) ) > 0 )
				{
					out.write( buf, 0, len );
				}
				out.close();
				in.close();
			}
			else
			{
				logger.log( Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists." );
			}
		}
		catch ( IOException ex )
		{
			logger.log( Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex );
		}
	}
	
	public InputStream getResource( String filename )
	{
		if ( filename == null )
		{
			throw new IllegalArgumentException( "Filename cannot be null" );
		}
		
		try
		{
			URL url = getClassLoader().getResource( filename );
			
			if ( url == null )
			{
				return null;
			}
			
			URLConnection connection = url.openConnection();
			connection.setUseCaches( false );
			return connection.getInputStream();
		}
		catch ( IOException ex )
		{
			return null;
		}
	}
	
	/**
	 * Returns the ClassLoader which holds this plugin
	 * 
	 * @return ClassLoader holding this plugin
	 */
	protected final ClassLoader getClassLoader()
	{
		return classLoader;
	}
	
	/**
	 * Sets the enabled state of this plugin
	 * 
	 * @param enabled true if enabled, otherwise false
	 */
	protected final void setEnabled( final boolean enabled )
	{
		if ( isEnabled != enabled )
		{
			isEnabled = enabled;
			
			if ( isEnabled )
			{
				onEnable();
			}
			else
			{
				onDisable();
			}
		}
	}
	
	final void init( PluginLoader loader, Loader server, PluginDescriptionFile description, File dataFolder, File file, ClassLoader classLoader )
	{
		this.loader = loader;
		this.server = server;
		this.file = file;
		this.description = description;
		this.dataFolder = dataFolder;
		this.classLoader = classLoader;
		this.configFile = new File( dataFolder, "config.yml" );
		this.logger = new PluginLogger( this );
	}
	
	/**
	 * Provides a list of all classes that should be persisted in the database
	 * 
	 * @return List of Classes that are Ebeans
	 */
	public List<Class<?>> getDatabaseClasses()
	{
		return new ArrayList<Class<?>>();
	}
	
	protected String replaceDatabaseString( String input )
	{
		input = input.replaceAll( "\\{DIR\\}", dataFolder.getPath().replaceAll( "\\\\", "/" ) + "/" );
		input = input.replaceAll( "\\{NAME\\}", description.getName().replaceAll( "[^\\w_-]", "" ) );
		return input;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean onCommand( SentientHandler sender, Command command, String label, String[] args )
	{
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<String> onTabComplete( SentientHandler sender, Command command, String alias, String[] args )
	{
		return null;
	}
	
	/**
	 * Gets the command with the given name, specific to this plugin. Commands
	 * need to be registered in the {@link PluginDescriptionFile#getCommands()
	 * PluginDescriptionFile} to exist at runtime.
	 * 
	 * @param name name or alias of the command
	 * @return the plugin command if found, otherwise null
	 */
	public PluginCommand getCommand( String name )
	{
		String alias = name.toLowerCase();
		PluginCommand command = Loader.getPluginManager().getPluginCommand( alias );
		
		if ( ( command != null ) && ( command.getPlugin() != this ) )
		{
			command = Loader.getPluginManager().getPluginCommand( description.getName().toLowerCase() + ":" + alias );
		}
		
		if ( ( command != null ) && ( command.getPlugin() == this ) )
		{
			return command;
		}
		else
		{
			return null;
		}
	}
	
	public void onLoad()
	{
	}
	
	public void onDisable()
	{
	}
	
	public void onEnable()
	{
	}
	
	public final boolean isNaggable()
	{
		return naggable;
	}
	
	public final void setNaggable( boolean canNag )
	{
		this.naggable = canNag;
	}
	
	public final Logger getLogger()
	{
		return logger;
	}
	
	@Override
	public String toString()
	{
		return description.getFullName();
	}
	
	/**
	 * This method provides fast access to the plugin that has {@link #getProvidingPlugin(Class) provided} the given plugin class, which is
	 * usually the plugin that implemented it.
	 * <p>
	 * An exception to this would be if plugin's jar that contained the class does not extend the class, where the intended plugin would have resided in a different jar / classloader.
	 * 
	 * @param clazz the class desired
	 * @return the plugin that provides and implements said class
	 * @throws IllegalArgumentException if clazz is null
	 * @throws IllegalArgumentException if clazz does not extend {@link JavaPlugin}
	 * @throws IllegalStateException if clazz was not provided by a plugin,
	 *              for example, if called with <code>JavaPlugin.getPlugin(JavaPlugin.class)</code>
	 * @throws IllegalStateException if called from the static initializer for
	 *              given JavaPlugin
	 * @throws ClassCastException if plugin that provided the class does not
	 *              extend the class
	 */
	public static <T extends JavaPlugin> T getPlugin( Class<T> clazz )
	{
		Validate.notNull( clazz, "Null class cannot have a plugin" );
		if ( !JavaPlugin.class.isAssignableFrom( clazz ) )
		{
			throw new IllegalArgumentException( clazz + " does not extend " + JavaPlugin.class );
		}
		final ClassLoader cl = clazz.getClassLoader();
		if ( !( cl instanceof PluginClassLoader ) )
		{
			throw new IllegalArgumentException( clazz + " is not initialized by " + PluginClassLoader.class );
		}
		JavaPlugin plugin = ( (PluginClassLoader) cl ).plugin;
		if ( plugin == null )
		{
			throw new IllegalStateException( "Cannot get plugin for " + clazz + " from a static initializer" );
		}
		return clazz.cast( plugin );
	}
	
	/**
	 * This method provides fast access to the plugin that has provided the
	 * given class.
	 * 
	 * @throws IllegalArgumentException if the class is not provided by a
	 *              JavaPlugin
	 * @throws IllegalArgumentException if class is null
	 * @throws IllegalStateException if called from the static initializer for
	 *              given JavaPlugin
	 */
	public static JavaPlugin getProvidingPlugin( Class<?> clazz )
	{
		Validate.notNull( clazz, "Null class cannot have a plugin" );
		final ClassLoader cl = clazz.getClassLoader();
		if ( !( cl instanceof PluginClassLoader ) )
		{
			throw new IllegalArgumentException( clazz + " is not provided by " + PluginClassLoader.class );
		}
		JavaPlugin plugin = ( (PluginClassLoader) cl ).plugin;
		if ( plugin == null )
		{
			throw new IllegalStateException( "Cannot get plugin for " + clazz + " from a static initializer" );
		}
		return plugin;
	}
	
	public Loader getInstance()
	{
		return server;
	}
}
