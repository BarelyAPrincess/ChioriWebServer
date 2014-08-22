package com.chiorichan.plugin.groovy;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.chiorichan.Loader;
import com.chiorichan.plugin.InvalidPluginException;
import com.chiorichan.plugin.PluginDescriptionFile;

/**
 * A ClassLoader for plugins, to allow shared classes across multiple plugins
 */
final class GroovyPluginClassLoader extends URLClassLoader
{
	private final GroovyPluginLoader loader;
	private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	private final PluginDescriptionFile description;
	private final File dataFolder;
	private final File file;
	final GroovyPlugin plugin;
	private GroovyPlugin pluginInit;
	private IllegalStateException pluginState;
	
	GroovyPluginClassLoader(final GroovyPluginLoader loader, final ClassLoader parent, final PluginDescriptionFile description, final File dataFolder, final File file) throws InvalidPluginException, MalformedURLException
	{
		super( new URL[] { file.toURI().toURL() }, parent );
		Validate.notNull( loader, "Loader cannot be null" );
		
		this.loader = loader;
		this.description = description;
		this.dataFolder = dataFolder;
		this.file = file;
		
		try
		{
			Class<?> jarClass;
			try
			{
				jarClass = Class.forName( description.getMain(), true, this );
			}
			catch ( ClassNotFoundException ex )
			{
				throw new InvalidPluginException( "Cannot find mane class `" + description.getMain() + "'", ex );
			}
			
			Class<? extends GroovyPlugin> pluginClass;
			try
			{
				pluginClass = jarClass.asSubclass( GroovyPlugin.class );
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidPluginException( "main class `" + description.getMain() + "' does not extend GroovyPlugin", ex );
			}
			
			plugin = pluginClass.newInstance();
		}
		catch ( IllegalAccessException ex )
		{
			throw new InvalidPluginException( "No public constructor", ex );
		}
		catch ( InstantiationException ex )
		{
			throw new InvalidPluginException( "Abnormal plugin type", ex );
		}
	}
	
	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException
	{
		return findClass( name, true );
	}
	
	Class<?> findClass( String name, boolean checkGlobal ) throws ClassNotFoundException
	{
		// TODO: Uncomment this oneday!
		/*if ( name.startsWith( "com.chiorichan." ) )
		{
			throw new ClassNotFoundException( name );
		}*/
		Class<?> result = classes.get( name );
		
		if ( result == null )
		{
			if ( checkGlobal )
			{
				result = loader.getClassByName( name );
			}
			
			if ( result == null )
			{
				result = super.findClass( name );
				
				if ( result != null )
				{
					loader.setClass( name, result );
				}
			}
			
			classes.put( name, result );
		}
		
		return result;
	}
	
	Set<String> getClasses()
	{
		return classes.keySet();
	}
	
	synchronized void initialize( GroovyPlugin javaPlugin )
	{
		Validate.notNull( javaPlugin, "Initializing plugin cannot be null" );
		Validate.isTrue( javaPlugin.getClass().getClassLoader() == this, "Cannot initialize plugin outside of this class loader" );
		if ( this.plugin != null || this.pluginInit != null )
		{
			throw new IllegalArgumentException( "Plugin already initialized!", pluginState );
		}
		
		pluginState = new IllegalStateException( "Initial initialization" );
		this.pluginInit = javaPlugin;
		
		javaPlugin.init( loader, Loader.getInstance(), description, dataFolder, file, this );
	}
}
