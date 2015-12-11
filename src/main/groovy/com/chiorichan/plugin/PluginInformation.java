/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.RunLevel;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.libraries.MavenReference;
import com.chiorichan.plugin.lang.PluginInformationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This type is the runtime-container for the information in the plugin.yaml. All plugins must have a respective
 * plugin.yaml. For plugins written in java using the standard plugin loader, this file must be in the root of the jar
 * file.
 * <p>
 * When the server loads a plugin, it needs to know some basic information about it. It reads this information from a YAML file, 'plugin.yaml'. This file consists of a set of attributes, each defined on a new line and with no indentation.
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
 * <td><code>libraries</code></td>
 * <td>{@link #getLibraries()}</td>
 * <td>Required java libraries</td>
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
 * name: SuperAwesomePlugin
 * version: 1.0.4
 * description: This plugin does something really awesome to the server
 * author: SomeAuthor
 * authors: [SomeAuthor, God, Jesus]
 * website: http://www.superawesomeplugin.com
 *
 * main: com.superawesomeplugin.plugin.Main
 * depend: [EmailPlugin]
 *
 * commands:
 *   doit:
 *     description: Does that super awesome thing
 *     aliases: [doit2, ihateyou]
 *     permission: com.chiorichan.destruction
 *     usage: Type /&lt;doit&gt; to do that super awesome thing.
 * </pre>
 *
 * </blockquote>
 *
 * XXX Rewrite the description file read process to make it easier to implement
 */
public class PluginInformation
{
	private YamlConfiguration yaml;

	public PluginInformation( final File file ) throws PluginInformationException, FileNotFoundException
	{
		yaml = YamlConfiguration.loadConfiguration( file );
	}

	public PluginInformation( final InputStream stream ) throws PluginInformationException
	{
		yaml = YamlConfiguration.loadConfiguration( stream );
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
	 * @throws PluginInformationException
	 */
	public PluginInformation( final String pluginName, final String pluginVersion, final String mainClass ) throws PluginInformationException
	{
		yaml = new YamlConfiguration();
		setName( pluginName );
		setPluginVersion( pluginVersion );
		setMainClass( mainClass );
	}

	public PluginInformation( final YamlConfiguration yaml ) throws PluginInformationException
	{
		this.yaml = yaml;
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
		if ( yaml.get( "authors" ) != null )
			if ( yaml.isList( "authors" ) )
				return yaml.getStringList( "authors" );
			else
				return ImmutableList.of( yaml.getString( "authors" ) );

		if ( yaml.getString( "author" ) != null )
			return ImmutableList.of( yaml.getString( "author" ) );
		return ImmutableList.of();
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
		return yaml.getStringList( "depend" );
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
		return yaml.getString( "description" );
	}

	/**
	 * Returns the name of a plugin, including the version. This method is provided for convenience; it uses the {@link #getName()} and {@link #getVersion()} entries.
	 *
	 * @return a descriptive name of the plugin and respective version
	 * @throws PluginInformationException
	 */
	public String getFullName()
	{
		return getName() + " v" + getVersion();
	}

	public String getGitHubBaseUrl()
	{
		return yaml.getString( "gitHubBaseUrl" );
	}

	/**
	 * Gives a list of java libraries required by this plugin.
	 * <ul>
	 * <li>Use the maven group:name:version string to specify the library.
	 * <li>If any libraries listed here are not found or can't be parsed, your plugin will fail to load at startup.
	 * <li><code>libraries</code> must be in <a href="http://en.wikipedia.org/wiki/YAML#Lists">YAML list format</a>.
	 * <li>For the time being, libraries are downloaded from the Central Maven Repository. We have plans to implement the ability to specify repositories, maybe even upload your libraries and plugins to our own central download server.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>libraries</code>.
	 * <p>
	 * Example: <blockquote>
	 *
	 * <pre>
	 * libraries:
	 * - com.dropbox.core:dropbox-core-sdk:1.7.7
	 * - org.apache.commons:commons-lang3:3.3.2
	 * </pre>
	 *
	 * </blockquote>
	 *
	 * @return immutable list of the plugin's dependencies
	 * @throws PluginInformationException
	 */
	public List<MavenReference> getLibraries()
	{
		List<MavenReference> refs = Lists.newArrayList();

		for ( String mavenString : yaml.getStringList( "libraries" ) )
			try
			{
				refs.add( new MavenReference( getName(), mavenString ) );
			}
			catch ( IllegalArgumentException e )
			{
				Loader.getLogger().severe( "Could not parse the library '" + mavenString + "' for plugin '" + getName() + "', expected pattern 'group:name:version' unless fixed, it will be ignored." );
				Loader.getLogger().severe( e.getMessage() );
			}

		return refs;
	}

	/**
	 * Gives the phase of server startup that the plugin should be loaded.
	 * <ul>
	 * <li>Possible values are in {@link RunLevel}.
	 * <li>Defaults to {@link RunLevel#INITIALIZED}.
	 * <li>Certain caveats apply to each phase.
	 * <li>When different, {@link #getDepend()}, {@link #getSoftDepend()}, and {@link #getLoadBefore()} become relative in order loaded per-phase. If a plugin loads at <code>STARTUP</code>, but a dependency loads at <code>POSTWORLD</code>,
	 * the dependency will not be loaded before the plugin is loaded.
	 * </ul>
	 * <p>
	 * In the plugin.yaml, this entry is named <code>runlevel</code>.
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
		try
		{
			return RunLevel.valueOf( yaml.getString( "runlevel" ) );
		}
		catch ( IllegalArgumentException | NullPointerException e )
		{
			return RunLevel.INITIALIZED;
		}
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
		return yaml.getStringList( "loadbefore" );
	}

	public String getMain()
	{
		return yaml.getString( "main" );
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
	 * @throws PluginInformationException
	 */
	public String getMainWithException() throws PluginInformationException
	{
		String main = yaml.getString( "main" );

		if ( main == null )
			throw new PluginInformationException( "Main is not defined." );

		if ( main.startsWith( "com.chiori" ) && !main.startsWith( "com.chiorichan.plugin" ) )
			throw new PluginInformationException( "Plugin is forbidden from using the 'com.chiori' namespace due to a conflict of interest." );

		return main;
	}

	public String getName()
	{
		return yaml.getString( "name" );
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
	public String getNameWithException() throws PluginInformationException
	{
		String name = yaml.getString( "name" );

		if ( name == null )
			throw new PluginInformationException( "Plugin name is not defined." );

		if ( !name.matches( "^[A-Za-z0-9 _.-]+$" ) )
			throw new PluginInformationException( "name '" + name + "' contains invalid characters." );

		return name;
	}

	public Map<String, List<String>> getNatives()
	{
		Map<String, List<String>> natives = Maps.newHashMap();

		ConfigurationSection section = yaml.getConfigurationSection( "natives" );
		if ( section != null )
			for ( String key : section.getKeys( false ) )
				if ( section.isList( key ) )
					natives.put( key, section.getStringList( key ) );
				else if ( natives.containsKey( key ) )
					natives.get( key ).add( section.getString( key ) );
				else
					natives.put( key, new ArrayList<String>( Arrays.asList( section.getString( key ) ) ) );

		return natives;

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
		return yaml.getString( "prefix" );
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
		return yaml.getStringList( "softdepend" );
	}

	public String getVersion()
	{
		return yaml.getString( "version" );
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
	 * @throws PluginInformationException
	 */
	public String getVersionWithException() throws PluginInformationException
	{
		String version = yaml.getString( "version" );

		if ( version == null )
			throw new PluginInformationException( "Plugin version is not defined." );

		return version;
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
		return yaml.getString( "website" );
	}

	/**
	 * Saves this PluginInformation to the given file
	 *
	 * @param file
	 *            File to output to
	 * @throws IOException
	 */
	public void save( File file ) throws IOException
	{
		yaml.save( file );
	}

	public String saveToString()
	{
		return yaml.saveToString();
	}

	PluginInformation setMainClass( String main )
	{
		yaml.set( "main", main );
		return this;
	}

	PluginInformation setName( String name ) throws PluginInformationException
	{
		if ( !name.matches( "^[A-Za-z0-9 _.-]+$" ) )
			throw new PluginInformationException( "name '" + name + "' contains invalid characters." );

		yaml.set( "name", name );

		return this;
	}

	PluginInformation setPluginVersion( String version )
	{
		yaml.set( "version", version );
		return this;
	}
}
