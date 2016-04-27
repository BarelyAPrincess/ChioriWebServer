/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;

import joptsimple.OptionParser;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.chiorichan.datastore.DatastoreManager;
import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.RunLevel;
import com.chiorichan.lang.StartupAbortException;
import com.chiorichan.lang.StartupException;
import com.chiorichan.logger.Log;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.services.AppManager;
import com.chiorichan.session.SessionManager;
import com.chiorichan.site.SiteManager;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.DownloadUpdaterService;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.Versioning;

public class Loader extends AppLoader
{
	private static File webroot = new File( "" );

	public static String getShutdownMessage()
	{
		return AppController.config().getString( "settings.shutdown-message" );
	}

	public static File getWebRoot()
	{
		return webroot;
	}

	public static void main( String... args )
	{
		init( Loader.class, args );
	}

	protected static void populateOptionParser( OptionParser parser )
	{
		parser.acceptsAll( Arrays.asList( "nobanner" ), "Disables the banner" );
		parser.acceptsAll( Arrays.asList( "web-ip" ), "Host for Web to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
		parser.acceptsAll( Arrays.asList( "web-port" ), "Port for Web to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
		parser.acceptsAll( Arrays.asList( "tcp-ip" ), "Host for Web to listen on" ).withRequiredArg().ofType( String.class ).describedAs( "Hostname or IP" );
		parser.acceptsAll( Arrays.asList( "tcp-port" ), "Port for Web to listen on" ).withRequiredArg().ofType( Integer.class ).describedAs( "Port" );
		parser.acceptsAll( Arrays.asList( "install" ), "Runs the server just long enough to create the required configuration files, then terminates." );
		parser.acceptsAll( Arrays.asList( "web-disable" ), "Disable the internal Web Server" );
		parser.acceptsAll( Arrays.asList( "tcp-disable" ), "Disable the internal TCP Server" );
		parser.acceptsAll( Arrays.asList( "webroot-dir" ), "Specify webroot directory" ).withRequiredArg().ofType( String.class );
	}

	private String clientId = "0";

	public Loader() throws StartupException
	{
		boolean firstRun = false;

		if ( !options().has( "nobanner" ) )
			ApplicationTerminal.terminal().showBanner();

		if ( Versioning.isAdminUser() )
			Log.get().warning( "We have detected that you are running " + Versioning.getProduct() + " with the system administrator/root, this is highly discouraged as it my compromise security and/or mess with file permissions." );

		if ( Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L )
			Log.get().warning( "It is recommended you dedicate more ram to this application, launch it with \"java -Xmx1024M -Xms1024M -jar " + AppConfig.getApplicationJar().getName() + "\"" );

		try
		{
			File contentTypes = new File( "ContentTypes.properties" );

			if ( !contentTypes.exists() )
				FileUtils.writeStringToFile( contentTypes, "# Chiori-chan's Web Server Content-Types File which overrides the default internal ones.\n# Syntax: 'ext: mime/type'" );
		}
		catch ( IOException e )
		{
			Log.get().warning( "There was an exception thrown trying to create the 'ContentTypes.properties' file.", e );
		}

		try
		{
			File shellOverrides = new File( "InterpreterOverrides.properties" );

			if ( !shellOverrides.exists() )
				FileUtils.writeStringToFile( shellOverrides, "# Chiori-chan's Web Server Interpreter Overrides File which overrides the default internal ones.\n# You don't have to add a string if the key and value are the same, hence Convension!\n# Syntax: 'fileExt: shellHandler'" );
		}
		catch ( IOException e )
		{
			Log.get().warning( "There was an exception thrown trying to create the 'InterpreterOverrides.properties' file.", e );
		}

		boolean install = false;

		if ( options().has( "install" ) )
		{
			install = true;

			if ( config().file().exists() )
			{
				// Warn the user that they may be overriding an existing installation
				Log.get().info( "                     " + EnumColor.RED + "" + EnumColor.NEGATIVE + "WARNING!!! WARNING!!! WARNING!!!" );
				Log.get().info( EnumColor.RED + "" + EnumColor.NEGATIVE + "--------------------------------------------------------------------------------" );
				Log.get().info( EnumColor.RED + "" + EnumColor.NEGATIVE + "| You've supplied the --install argument which instructs the server to factory |" );
				Log.get().info( EnumColor.RED + "" + EnumColor.NEGATIVE + "| reset all files and configuration required to run Chiori-chan's Web Server,  |" );
				Log.get().info( EnumColor.RED + "" + EnumColor.NEGATIVE + "| This includes database and plugin configuration. This can not be undone!     |" );
				Log.get().info( EnumColor.RED + "" + EnumColor.NEGATIVE + "--------------------------------------------------------------------------------" );
				String key = ApplicationTerminal.terminal().prompt( EnumColor.RED + "" + EnumColor.NEGATIVE + "Are you sure you wish to continue? Press 'Y' for Yes, 'N' for No or 'C' to Continue Normally.", "Y", "N", "C" );

				if ( key.equals( "N" ) )
				{
					Log.get().info( "The server will now stop, please wait..." );
					throw new StartupAbortException();
				}

				if ( key.equals( "C" ) )
					install = false;
			}
		}

		if ( !config().file().exists() )
			firstRun = true;

		if ( firstRun || install )
			try
			{
				// Delete Existing Configuration
				if ( config().file().exists() )
					config().file().delete();

				// Save Factory Configuration
				FileFunc.putResource( "com/chiorichan/config.yaml", config().file() );
			}
			catch ( IOException e )
			{
				Log.get().severe( "It would appear we had problem installing " + Versioning.getProduct() + " " + Versioning.getVersion() + " for the first time, see exception for details.", e );
			}

		clientId = config().getString( "server.installationUID", clientId );

		if ( clientId == null || clientId.isEmpty() || clientId.equalsIgnoreCase( "null" ) )
		{
			clientId = UUID.randomUUID().toString();
			config().set( "server.installationUID", clientId );
		}

		webroot = AppController.config().getDirectory( "webroot", "webroot", true );

		if ( install )
		{
			// TODO Add more files to be deleted on factory reset
			FileUtils.deleteQuietly( webroot );
			FileUtils.deleteQuietly( config().getDirectoryCache() );
			FileUtils.deleteQuietly( config().getDirectoryLogs() );
			FileUtils.deleteQuietly( config().getDirectoryUpdates() );
			FileUtils.deleteQuietly( config().getDirectoryPlugins() );
		}

		if ( firstRun || install )
			try
			{
				// Check and Extract WebUI Interface

				String fwZip = "com/chiorichan/framework.archive";
				String zipMD5 = FileFunc.resourceToString( fwZip + ".md5" );

				if ( zipMD5 == null )
				{
					InputStream is = getClass().getClassLoader().getResourceAsStream( fwZip );

					if ( is == null )
						throw new IOException();
					zipMD5 = DigestUtils.md5Hex( is );
				}

				File fwRoot = new File( webroot, "default" );
				File curMD5 = new File( fwRoot, "version.md5" );
				if ( firstRun || !curMD5.exists() || !zipMD5.equals( FileUtils.readFileToString( curMD5 ) ) )
				{
					Log.get().info( "Extracting the Web UI to the Framework Webroot... Please wait..." );
					FileUtils.deleteDirectory( fwRoot );
					FileFunc.extractZipResource( fwZip, fwRoot );
					FileUtils.write( new File( fwRoot, "version.md5" ), zipMD5 );
					Log.get().info( "Finished with no errors!!" );
				}

				// Create 'start.sh' Script for Unix-like Systems
				File startSh = new File( "start.sh" );
				if ( Versioning.isUnixLikeOS() && !startSh.exists() )
				{
					String startString = "#!/bin/bash\necho \"Starting " + Versioning.getProduct() + " " + Versioning.getVersion() + " [ hit CTRL-C to stop ]\"\njava -Xmx2G -Xms2G -jar " + new File( Loader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() ).getAbsolutePath();
					FileUtils.writeStringToFile( startSh, startString );
					startSh.setExecutable( true, true );
				}

				// Create 'debugstart.sh' Script for Unix-like Systems and if we are in development mode
				startSh = new File( "debug.sh" );
				if ( Versioning.isDevelopment() && Versioning.isUnixLikeOS() && !startSh.exists() )
				{
					String startString = "#!/bin/bash\necho \"Starting " + Versioning.getProduct() + " " + Versioning.getVersion() + " in debug mode [ hit CTRL-C to stop ]\"\njava -Xmx2G -Xms2G -server -XX:+DisableExplicitGC -Xdebug -Xrunjdwp:transport=dt_socket,address=8686,server=y,suspend=n -jar " + new File( Loader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() ).getAbsolutePath();
					FileUtils.writeStringToFile( startSh, startString );
					startSh.setExecutable( true, true );
				}
			}
			catch ( IOException | URISyntaxException e )
			{
				Log.get().severe( "It would appear we had problem installing " + Versioning.getProduct() + " for the first time, see exception for details.", e );
			}

		if ( install )
		{
			// Miscellaneous files and folders that need deletion to comply with a factory reset
			FileUtils.deleteQuietly( new File( "server.db" ) );
			FileUtils.deleteQuietly( new File( "permissions.yaml" ) );
			FileUtils.deleteQuietly( new File( "sites" ) );
			FileUtils.deleteQuietly( new File( "sessions" ) );
			FileUtils.deleteQuietly( new File( "accounts" ) );
			FileUtils.deleteQuietly( new File( "ContentTypes.properties" ) );
			FileUtils.deleteQuietly( new File( "InterpreterOverrides.properties" ) );

			// Delete Plugin Configuration
			for ( File f : new File( "plugins" ).listFiles() )
				if ( !f.getName().toLowerCase().endsWith( "jar" ) )
					FileUtils.deleteQuietly( f );

			Log.get().info( "Installation was successful, please wait..." );
			throw new StartupAbortException();
		}

		if ( firstRun )
		{
			Log.get().highlight( "                          ATTENTION! ATTENTION! ATTENTION!" );
			Log.get().highlight( "--------------------------------------------------------------------------------------" );
			Log.get().highlight( "| It appears that this is your first time running Chiori-chan's Web Server.          |" );
			Log.get().highlight( "| All the needed files have been created and extracted from the server jar file.     |" );
			Log.get().highlight( "| We highly recommended that you stop the server, review config(), and restart.      |" );
			Log.get().highlight( "| You can find documentation and guides on our Github at:                            |" );
			Log.get().highlight( "|                   https://github.com/ChioriGreene/ChioriWebServer                  |" );
			Log.get().highlight( "--------------------------------------------------------------------------------------" );
			String key = ApplicationTerminal.terminal().prompt( "Would you like to stop and review config()? Press 'Y' for Yes or 'N' for No.", "Y", "N" );

			if ( key.equals( "Y" ) )
			{
				Log.get().info( "The server will now stop, please wait..." );
				throw new StartupAbortException();
			}
		}

		if ( Versioning.isUnixLikeOS() )
		{
			SignalHandler handler = new SignalHandler()
			{
				@Override
				public void handle( Signal arg0 )
				{
					AppController.stopApplication( "Received SIGTERM - Terminate" );
				}
			};

			Signal.handle( new Signal( "TERM" ), handler );
			Signal.handle( new Signal( "INT" ), handler );
		}

		if ( !config().getBoolean( "server.disableTracking" ) && !Versioning.isDevelopment() )
			NetworkFunc.sendTracking( "startServer", "start", Versioning.getVersion() + " (Build #" + Versioning.getBuildNumber() + ")" );
	}

	public AppConfig config()
	{
		return AppController.config();
	}

	public boolean hasWhitelist()
	{
		return config().getBoolean( "white-list", false );
	}

	@Override
	public void onRunlevelChange( RunLevel level ) throws ApplicationException
	{
		switch ( level )
		{
			case SHUTDOWN:
			{
				if ( AppManager.manager( SessionManager.class ).isInitalized() )
				{
					Log.get().info( "Shutting Down Session Manager..." );
					SessionManager.instance().shutdown();
				}

				Log.get().info( "Shutting Down Network Manager..." );
				NetworkManager.shutdown();

				if ( AppManager.manager( SiteManager.class ).isInitalized() )
				{
					Log.get().info( "Shutting Down Site Manager..." );
					SiteManager.instance().unloadSites();
				}
				break;
			}
			case INITIALIZATION:
				break;
			case INITIALIZED:
				break;
			case POSTSTARTUP:
			{
				Log.get().info( "Initalizing the Datastore Subsystem..." );
				AppManager.manager( DatastoreManager.class ).init();

				Log.get().info( "Initalizing the Database Subsystem..." );
				AppController.config().initDatabase();

				Log.get().info( "Initalizing the Site Subsystem..." );
				AppManager.manager( SiteManager.class ).init();

				Log.get().info( "Initalizing the Session Subsystem..." );
				AppManager.manager( SessionManager.class ).init();

				Log.get().info( "Initalizing the File Watcher Subsystem..." );
				AppManager.manager( ServerFileWatcher.class ).init();

				break;
			}
			case RELOAD:
			{
				// TODO: Reload seems to be broken. This needs some serious reworking.

				Log.get().info( "Reinitalizing the Session Manager..." );
				SessionManager.instance().reload();

				Log.get().info( "Reinitalizing the Site Manager..." );
				SiteManager.instance().reload();
				break;
			}
			case RUNNING:
			{
				AppManager.manager( AutoUpdater.class ).init( new DownloadUpdaterService( AppController.config().getString( "auto-updater.host" ) ), AppController.config().getString( "auto-updater.preferred-channel" ) );
				AutoUpdater updater = AutoUpdater.instance();

				updater.setEnabled( AppController.config().getBoolean( "auto-updater.enabled" ) );
				updater.setSuggestChannels( AppController.config().getBoolean( "auto-updater.suggest-channels" ) );
				updater.getOnBroken().addAll( AppController.config().getStringList( "auto-updater.on-broken" ) );
				updater.getOnUpdate().addAll( AppController.config().getStringList( "auto-updater.on-update" ) );

				updater.check();
				break;
			}
			case STARTUP:
			{
				if ( !options().has( "tcp-disable" ) && AppController.config().getBoolean( "server.enableTcpServer", true ) )
					NetworkManager.startTcpServer();
				else
					Log.get().warning( "The integrated tcp server has been disabled per the configuration. Change server.enableTcpServer to true to reenable it." );

				if ( !options().has( "web-disable" ) && AppController.config().getBoolean( "server.enableWebServer", true ) )
				{
					NetworkManager.startHttpServer();
					NetworkManager.startHttpsServer();
				}
				else
					Log.get().warning( "The integrated web server has been disabled per the configuration. Change server.enableWebServer to true to reenable it." );

				if ( !options().has( "query-disable" ) && AppController.config().getBoolean( "server.queryEnabled", true ) )
					NetworkManager.startQueryServer();

				break;
			}
			case CRASHED:
				break;
			case DISPOSED:
				break;
			default:
				break;
		}
	}

	@Override
	public String toString()
	{
		return Versioning.getProduct() + " " + Versioning.getVersion();
	}
}
