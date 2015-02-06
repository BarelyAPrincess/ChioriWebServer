/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.chiorichan.RunLevel;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.google.common.collect.ImmutableList;

/**
 * This type is the runtime-container for the information in the plugin.yaml. All plugins must have a respective
 * plugin.yaml. For plugins written in java using the standard plugin loader, this file must be in the root of the jar
 * file.
 * <p>
 * When Bukkit loads a plugin, it needs to know some basic information about it. It reads this information from a YAML file, 'plugin.yaml'. This file consists of a set of attributes, each defined on a new line and with no indentation.
 * <p>
 * Every (almost* every) method corresponds with a specific entry in the plugin.yaml. These are the <b>required</b> entries for every plugin.yaml:
 * <ul>
 * <li>{@link #getName()} - <code>name</code>
 * <li>{@link #getVersion()} - <code>version</code>
 * <li>{@link #getMain()} - <code>main</code>
 * </ul>
 * <p>
 * Failing to include any of these items will throw an exception and cause the server to ignore your plugin.
 * <p>
 * This is a list of the possible yaml keys, with specific details included in the respective method documentations:
 * <table border=1>
 * <tr>
 * <th>Node</th>
 * <th>Method</th>
 * <th>Summary</th>
 * </tr>
 * <tr>
 * <td><code>name</code></td>
 * <td>{@link #getName()}</td>
 * <td>The unique name of plugin</td>
 * </tr>
 * <tr>
 * <td><code>version</code></td>
 * <td>{@link #getVersion()}</td>
 * <td>A plugin revision identifier</td>
 * </tr>
 * <tr>
 * <td><code>main</code></td>
 * <td>{@link #getMain()}</td>
 * <td>The plugin's initial class file</td>
 * </tr>
 * <tr>
 * <td><code>author</code><br>
 * <code>authors</code></td>
 * <td>{@link #getAuthors()}</td>
 * <td>The plugin contributors</td>
 * </tr>
 * <tr>
 * <td><code>description</code></td>
 * <td>{@link #getDescription()}</td>
 * <td>Human readable plugin summary</td>
 * </tr>
 * <tr>
 * <td><code>website</code></td>
 * <td>{@link #getWebsite()}</td>
 * <td>The URL to the plugin's site</td>
 * </tr>
 * <tr>
 * <td><code>prefix</code></td>
 * <td>{@link #getPrefix()}</td>
 * <td>The token to prefix plugin log entries</td>
 * </tr>
 * <tr>
 * <td><code>load</code></td>
 * <td>{@link #getLoad()}</td>
 * <td>The phase of server-startup this plugin will load during</td>
 * </tr>
 * <tr>
 * <td><code>depend</code></td>
 * <td>{@link #getDepend()}</td>
 * <td>Other required plugins</td>
 * </tr>
 * <tr>
 * <td><code>softdepend</code></td>
 * <td>{@link #getSoftDepend()}</td>
 * <td>Other plugins that add functionality</td>
 * </tr>
 * <tr>
 * <td><code>loadbefore</code></td>
 * <td>{@link #getLoadBefore()}</td>
 * <td>The inverse softdepend</td>
 * </tr>
 * </table>
 * <p>
 * A plugin.yaml example:<blockquote>
 * 
 * <pre>
 * name: Inferno
 * version: 1.4.1
 * description: This plugin is so 31337. You can set yourself on fire.
 * # We could place every author in the authors list, but chose not to for illustrative purposes
 * # Also, having an author distinguishes that person as the project lead, and ensures their
 * # name is displayed first
 * author: CaptainInflamo
 * authors: [Cogito, verrier, EvilSeph]
 * website: http://www.curse.com/server-mods/minecraft/myplugin
 * 
 * main: com.captaininflamo.bukkit.inferno.Inferno
 * database: false
 * depend: [NewFire, FlameWire]
 * 
 * commands:
 *   flagrate:
 *     description: Set yourself on fire.
 *     aliases: [combust_me, combustMe]
 *     permission: inferno.flagrate
 *     usage: Syntax error! Simply type /&lt;command&gt; to ignite yourself.
 *   burningdeaths:
 *     description: List how many times you have died by fire.
 *     aliases: [burning_deaths, burningDeaths]
 *     permission: inferno.burningdeaths
 *     usage: |
 *       /&lt;command&gt; [player]
 *       Example: /&lt;command&gt; - see how many times you have burned to death
 *       Example: /&lt;command&gt; CaptainIce - see how many times CaptainIce has burned to death
 * 
 * permissions:
 *   inferno.*:
 *     description: Gives access to all Inferno commands
 *     children:
 *       inferno.flagrate: true
 *       inferno.burningdeaths: true
 *       inferno.burningdeaths.others: true
 *   inferno.flagrate:
 *     description: Allows you to ignite yourself
 *     default: true
 *   inferno.burningdeaths:
 *     description: Allows you to see how many times you have burned to death
 *     default: true
 *   inferno.burningdeaths.others:
 *     description: Allows you to see how many times others have burned to death
 *     default: op
 *     children:
 *       inferno.burningdeaths: true
 * </pre>
 * 
 * </blockquote>
 */
public class PluginDescriptionFile
{
	private static final Yaml yaml = new Yaml( new SafeConstructor() );
	private String name = null;
	private String main = null;
	private String classLoaderOf = null;
	private List<String> depend = null;
	private List<String> softDepend = null;
	private List<String> loadBefore = null;
	private String version = null;
	private String description = null;
	private List<String> authors = null;
	private String website = null;
	private String prefix = null;
	private RunLevel order = RunLevel.INITIALIZED;
	
	public PluginDescriptionFile( final File file ) throws InvalidDescriptionException, FileNotFoundException
	{
		loadMap( asMap( yaml.load( new FileInputStream( file ) ) ) );
	}
	
	public PluginDescriptionFile( final InputStream stream ) throws InvalidDescriptionException
	{
		loadMap( asMap( yaml.load( stream ) ) );
	}
	
	public PluginDescriptionFile( final YamlConfiguration descYaml ) throws InvalidDescriptionException
	{
		loadMap( asMap( descYaml ) );
	}
	
	/**
	 * Loads a PluginDescriptionFile from the specified reader
	 * 
	 * @param reader
	 *            The reader
	 * @throws InvalidDescriptionException
	 *             If the PluginDescriptionFile is invalid
	 */
	public PluginDescriptionFile( final Reader reader ) throws InvalidDescriptionException
	{
		loadMap( asMap( yaml.load( reader ) ) );
	}
	
	/**
	 * Creates a new PluginDescriptionFile with the given detailed
	 * 
	 * @param pluginName
	 *            Name of this plugin
	 * @param pluginVersion
	 *            Version of this plugin
	 * @param mainClass
	 *            Full location of the main class of this plugin
	 */
	public PluginDescriptionFile( final String pluginName, final String pluginVersion, final String mainClass )
	{
		name = pluginName;
		version = pluginVersion;
		main = mainClass;
	}
	
	/**
	 * Gives the name of the plugin. This name is a unique identifier for plugins.
	 * <ul>
	 * <li>Must consist of all alphanumeric characters, underscores, hyphon, and period (a-z,A-Z,0-9, _.-). Any other character will cause the plugin.yaml to fail loading.
	 * <li>Used to determine the name of the plugin's data folder. Data folders are placed in the ./plugins/ directory by default, but this behavior should not be relied on.
	 * <li>It is good practice to name your jar the same as this, for example 'MyPlugin.jar'.
	 * <li>Case sensitive.
	 * <li>The is the token referenced in {@link #getDepend()}, {@link #getSoftDepend()}, and {@link #getLoadBefore()}.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>name</code>.
	 * <p>
	 * Example:<blockquote>
	 * 
	 * <pre>
	 * name: MyPlugin
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return the name of the plugin
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Gives the version of the plugin.
	 * <ul>
	 * <li>Version is an arbitrary string, however the most common format is MajorRelease.MinorRelease.Build (eg: 1.4.1).
	 * <li>Typically you will increment this every time you release a new feature or bug fix.
	 * <li>Displayed when a user types <code>/version PluginName</code>
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>version</code>.
	 * <p>
	 * Example:<blockquote>
	 * 
	 * <pre>
	 * version: 1.4.1
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return the version of the plugin
	 */
	public String getVersion()
	{
		return version;
	}
	
	/**
	 * Gives the fully qualified name of the main class for a plugin. The format should follow the {@link ClassLoader#loadClass(String)} syntax to successfully be resolved at runtime. For most plugins, this is the
	 * class that extends
	 * <ul>
	 * <li>This must contain the full namespace including the class file itself.
	 * <li>If your namespace is <code>com.chiorichan.plugin</code>, and your class file is called <code>MyPlugin</code> then this must be <code>com.chiorichan.plugin.MyPlugin</code>
	 * <li>No plugin can use <code>org.bukkit.</code> as a base package for <b>any class</b>, including the main class.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>main</code>.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * main: org.bukkit.plugin.MyPlugin
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return the fully qualified main class for the plugin
	 */
	public String getMain()
	{
		return main;
	}
	
	/**
	 * Gives a human-friendly description of the functionality the plugin provides.
	 * <ul>
	 * <li>The description can have multiple lines.
	 * <li>Displayed when a user types <code>/version PluginName</code>
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>description</code>.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * description: This plugin is so 31337. You can set yourself on fire.
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return description of this plugin, or null if not specified
	 */
	public String getDescription()
	{
		return description;
	}
	
	/**
	 * Gives the phase of server startup that the plugin should be loaded.
	 * <ul>
	 * <li>Possible values are in {@link RunLevel}.
	 * <li>Defaults to {@link RunLevel#POSTSERVER}.
	 * <li>Certain caveats apply to each phase.
	 * <li>When different, {@link #getDepend()}, {@link #getSoftDepend()}, and {@link #getLoadBefore()} become relative in order loaded per-phase. If a plugin loads at <code>STARTUP</code>, but a dependency loads at <code>POSTWORLD</code>,
	 * the dependency will not be loaded before the plugin is loaded.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>load</code>.
	 * <p>
	 * Example:<blockquote>
	 * 
	 * <pre>
	 * load: STARTUP
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return the phase when the plugin should be loaded
	 */
	public RunLevel getLoad()
	{
		return order;
	}
	
	/**
	 * Gives the list of authors for the plugin.
	 * <ul>
	 * <li>Gives credit to the developer.
	 * <li>Used in some server error messages to provide helpful feedback on who to contact when an error occurs.
	 * <li>A bukkit.org forum handle or email address is recommended.
	 * <li>Is displayed when a user types <code>/version PluginName</code>
	 * <li><code>authors</code> must be in <a href="http://en.wikipedia.org/wiki/YAML#Lists">YAML list format</a>.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this has two entries, <code>author</code> and <code>authors</code>.
	 * <p>
	 * Single author example: <blockquote>
	 * 
	 * <pre>
	 * author: CaptainInflamo
	 * </pre>
	 * 
	 * </blockquote> Multiple author example: <blockquote>
	 * 
	 * <pre>
	 * authors: [Cogito, verrier, EvilSeph]
	 * </pre>
	 * 
	 * </blockquote> When both are specified, author will be the first entry in the list, so this example: <blockquote>
	 * 
	 * <pre>
	 * author: Grum
	 * authors:
	 * - feildmaster
	 * - amaranth
	 * </pre>
	 * 
	 * </blockquote> Is equivilant to this example: <blockquote>
	 * 
	 * <pre>authors: [Grum, feildmaster, aramanth]
	 * 
	 * <pre>
	 * </blockquote>
	 * 
	 * @return an immutable list of the plugin's authors
	 */
	public List<String> getAuthors()
	{
		return authors;
	}
	
	/**
	 * Gives the plugin's or plugin's author's website.
	 * <ul>
	 * <li>A link to the Curse page that includes documentation and downloads is highly recommended.
	 * <li>Displayed when a user types <code>/version PluginName</code>
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>website</code>.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * website: http://www.curse.com/server-mods/minecraft/myplugin
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return description of this plugin, or null if not specified
	 */
	public String getWebsite()
	{
		return website;
	}
	
	/**
	 * Gives a list of other plugins that the plugin requires.
	 * <ul>
	 * <li>Use the value in the {@link #getName()} of the target plugin to specify the dependency.
	 * <li>If any plugin listed here is not found, your plugin will fail to load at startup.
	 * <li>If multiple plugins list each other in <code>depend</code>, creating a network with no individual plugin does not list another plugin in the <a href=https://en.wikipedia.org/wiki/Circular_dependency>network</a>, all plugins in
	 * that network will fail.
	 * <li><code>depend</code> must be in must be in <a href="http://en.wikipedia.org/wiki/YAML#Lists">YAML list format</a>.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>depend</code>.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * depend:
	 * - OnePlugin
	 * - AnotherPlugin
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return immutable list of the plugin's dependencies
	 */
	public List<String> getDepend()
	{
		return depend;
	}
	
	/**
	 * Gives a list of other plugins that the plugin requires for full functionality. The {@link PluginManager} will make
	 * best effort to treat all entries here as if they were a {@link #getDepend() dependency}, but will never fail
	 * because of one of these entries.
	 * <ul>
	 * <li>Use the value in the {@link #getName()} of the target plugin to specify the dependency.
	 * <li>When an unresolvable plugin is listed, it will be ignored and does not affect load order.
	 * <li>When a circular dependency occurs (a network of plugins depending or soft-dependending each other), it will arbitrarily choose a plugin that can be resolved when ignoring soft-dependencies.
	 * <li><code>softdepend</code> must be in <a href="http://en.wikipedia.org/wiki/YAML#Lists">YAML list format</a>.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>softdepend</code>.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * softdepend: [OnePlugin, AnotherPlugin]
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return immutable list of the plugin's preferred dependencies
	 */
	public List<String> getSoftDepend()
	{
		return softDepend;
	}
	
	/**
	 * Gets the list of plugins that should consider this plugin a soft-dependency.
	 * <ul>
	 * <li>Use the value in the {@link #getName()} of the target plugin to specify the dependency.
	 * <li>The plugin should load before any other plugins listed here.
	 * <li>Specifying another plugin here is strictly equivalent to having the specified plugin's {@link #getSoftDepend()} include {@link #getName() this plugin}.
	 * <li><code>loadbefore</code> must be in <a href="http://en.wikipedia.org/wiki/YAML#Lists">YAML list format</a>.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>loadbefore</code>.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * loadbefore:
	 * - OnePlugin
	 * - AnotherPlugin
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return immutable list of plugins that should consider this plugin a soft-dependency
	 */
	public List<String> getLoadBefore()
	{
		return loadBefore;
	}
	
	/**
	 * Gives the token to prefix plugin-specific logging messages with.
	 * <ul>
	 * <li>This includes all messages using Plugin#getLogger()
	 * <li>If not specified, the server uses the plugin's {@link #getName() name}.
	 * <li>This should clearly indicate what plugin is being logged.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>prefix</code>.
	 * <p>
	 * Example:<blockquote>
	 * 
	 * <pre>
	 * prefix: ex-why-zee
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @return the prefixed logging token, or null if not specified
	 */
	public String getPrefix()
	{
		return prefix;
	}
	
	/**
	 * Returns the name of a plugin, including the version. This method is provided for convenience; it uses the {@link #getName()} and {@link #getVersion()} entries.
	 * 
	 * @return a descriptive name of the plugin and respective version
	 */
	public String getFullName()
	{
		return name + " v" + version;
	}
	
	public String getClassLoaderOf()
	{
		return classLoaderOf;
	}
	
	/**
	 * Saves this PluginDescriptionFile to the given writer
	 * 
	 * @param writer
	 *            Writer to output this file to
	 */
	public void save( Writer writer )
	{
		yaml.dump( saveMap(), writer );
	}
	
	private void loadMap( Map<?, ?> map ) throws InvalidDescriptionException
	{
		try
		{
			name = map.get( "name" ).toString();
			
			if ( !name.matches( "^[A-Za-z0-9 _.-]+$" ) )
			{
				throw new InvalidDescriptionException( "name '" + name + "' contains invalid characters." );
			}
		}
		catch ( NullPointerException ex )
		{
			throw new InvalidDescriptionException( ex, "name is not defined" );
		}
		catch ( ClassCastException ex )
		{
			throw new InvalidDescriptionException( ex, "name is of wrong type" );
		}
		
		try
		{
			version = map.get( "version" ).toString();
		}
		catch ( NullPointerException ex )
		{
			throw new InvalidDescriptionException( ex, "version is not defined" );
		}
		catch ( ClassCastException ex )
		{
			throw new InvalidDescriptionException( ex, "version is of wrong type" );
		}
		
		try
		{
			main = map.get( "main" ).toString();
			if ( main.startsWith( "com.chiori" ) && !main.startsWith( "com.chiorichan.plugin.builtin" ) )
				throw new InvalidDescriptionException( "main may not be within the com.chiori namespace" );
		}
		catch ( NullPointerException ex )
		{
			throw new InvalidDescriptionException( ex, "main is not defined" );
		}
		catch ( ClassCastException ex )
		{
			throw new InvalidDescriptionException( ex, "main is of wrong type" );
		}
		
		if ( map.get( "class-loader-of" ) != null )
		{
			classLoaderOf = map.get( "class-loader-of" ).toString();
		}
		
		if ( map.get( "depend" ) != null )
		{
			ImmutableList.Builder<String> dependBuilder = ImmutableList.<String> builder();
			try
			{
				for ( Object dependency : ( Iterable<?> ) map.get( "depend" ) )
				{
					dependBuilder.add( dependency.toString() );
				}
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidDescriptionException( ex, "depend is of wrong type" );
			}
			catch ( NullPointerException e )
			{
				throw new InvalidDescriptionException( e, "invalid dependency format" );
			}
			depend = dependBuilder.build();
		}
		
		if ( map.get( "softdepend" ) != null )
		{
			ImmutableList.Builder<String> softDependBuilder = ImmutableList.<String> builder();
			try
			{
				for ( Object dependency : ( Iterable<?> ) map.get( "softdepend" ) )
				{
					softDependBuilder.add( dependency.toString() );
				}
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidDescriptionException( ex, "softdepend is of wrong type" );
			}
			catch ( NullPointerException ex )
			{
				throw new InvalidDescriptionException( ex, "invalid soft-dependency format" );
			}
			softDepend = softDependBuilder.build();
		}
		
		if ( map.get( "loadbefore" ) != null )
		{
			ImmutableList.Builder<String> loadBeforeBuilder = ImmutableList.<String> builder();
			try
			{
				for ( Object predependency : ( Iterable<?> ) map.get( "loadbefore" ) )
				{
					loadBeforeBuilder.add( predependency.toString() );
				}
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidDescriptionException( ex, "loadbefore is of wrong type" );
			}
			catch ( NullPointerException ex )
			{
				throw new InvalidDescriptionException( ex, "invalid load-before format" );
			}
			loadBefore = loadBeforeBuilder.build();
		}
		
		if ( map.get( "website" ) != null )
		{
			website = map.get( "website" ).toString();
		}
		
		if ( map.get( "description" ) != null )
		{
			description = map.get( "description" ).toString();
		}
		
		if ( map.get( "load" ) != null )
		{
			try
			{
				order = RunLevel.valueOf( ( ( String ) map.get( "load" ) ).toUpperCase().replaceAll( "\\W", "" ) );
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidDescriptionException( ex, "load is of wrong type" );
			}
			catch ( IllegalArgumentException ex )
			{
				throw new InvalidDescriptionException( ex, "load is not a valid choice" );
			}
		}
		
		if ( map.get( "authors" ) != null )
		{
			ImmutableList.Builder<String> authorsBuilder = ImmutableList.<String> builder();
			if ( map.get( "author" ) != null )
			{
				authorsBuilder.add( map.get( "author" ).toString() );
			}
			try
			{
				for ( Object o : ( Iterable<?> ) map.get( "authors" ) )
				{
					authorsBuilder.add( o.toString() );
				}
			}
			catch ( ClassCastException ex )
			{
				throw new InvalidDescriptionException( ex, "authors are of wrong type" );
			}
			catch ( NullPointerException ex )
			{
				throw new InvalidDescriptionException( ex, "authors are improperly defined" );
			}
			authors = authorsBuilder.build();
		}
		else if ( map.get( "author" ) != null )
		{
			authors = ImmutableList.of( map.get( "author" ).toString() );
		}
		else
		{
			authors = ImmutableList.<String> of();
		}
		
		if ( map.get( "prefix" ) != null )
		{
			prefix = map.get( "prefix" ).toString();
		}
	}
	
	private Map<String, Object> saveMap()
	{
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put( "name", name );
		map.put( "main", main );
		map.put( "version", version );
		map.put( "order", order.toString() );
		
		if ( depend != null )
		{
			map.put( "depend", depend );
		}
		if ( softDepend != null )
		{
			map.put( "softdepend", softDepend );
		}
		if ( website != null )
		{
			map.put( "website", website );
		}
		if ( description != null )
		{
			map.put( "description", description );
		}
		
		if ( authors.size() == 1 )
		{
			map.put( "author", authors.get( 0 ) );
		}
		else if ( authors.size() > 1 )
		{
			map.put( "authors", authors );
		}
		
		if ( classLoaderOf != null )
		{
			map.put( "class-loader-of", classLoaderOf );
		}
		
		if ( prefix != null )
		{
			map.put( "prefix", prefix );
		}
		
		return map;
	}
	
	private Map<?, ?> asMap( Object object ) throws InvalidDescriptionException
	{
		if ( object instanceof Map )
		{
			return ( Map<?, ?> ) object;
		}
		throw new InvalidDescriptionException( object + " is not properly structured." );
	}
}
