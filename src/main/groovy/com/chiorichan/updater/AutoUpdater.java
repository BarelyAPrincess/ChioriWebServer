/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.updater;

import java.io.File;
import java.io.IOException;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.chiorichan.AppConfig;
import com.chiorichan.AppController;
import com.chiorichan.AppLoader;
import com.chiorichan.ApplicationTerminal;
import com.chiorichan.ServerFileWatcher;
import com.chiorichan.ServerFileWatcher.EventCallback;
import com.chiorichan.account.AccountAttachment;
import com.chiorichan.account.event.AccountSuccessfulLoginEvent;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.lang.ApplicationException;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.logger.Log;
import com.chiorichan.logger.LogSource;
import com.chiorichan.messaging.MessageReceiver;
import com.chiorichan.services.AppManager;
import com.chiorichan.services.ServiceManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.updater.BuildArtifact.ChangeSet.ChangeSetDetails;
import com.chiorichan.util.SecureFunc;
import com.chiorichan.util.Versioning;

public class AutoUpdater implements ServiceManager, Listener, TaskRegistrar, LogSource
{
	public static final String WARN_CONSOLE = "warn-console";
	public static final String WARN_OPERATORS = "warn-ops";

	private static AutoUpdater instance = null;

	public static Log getLogger()
	{
		return Log.get( instance );
	}

	protected static DownloadUpdaterService getService()
	{
		return instance == null ? null : instance.service;
	}

	public static AutoUpdater instance()
	{
		return AppManager.manager( AutoUpdater.class ).instance();
	}

	private final DownloadUpdaterService service;
	private final List<String> onUpdate = new ArrayList<String>();
	private final List<String> onBroken = new ArrayList<String>();
	private final String channel;
	private boolean enabled;
	private BuildArtifact current = null;

	private BuildArtifact latest = null;

	private boolean suggestChannels = true;

	private String serverJarMD5 = null;

	public AutoUpdater( DownloadUpdaterService service, String channel )
	{
		// TODO Make it so the updater can update jars and class files

		instance = this;
		this.service = service;
		this.channel = channel;

		/*
		 * This schedules the Auto Updater with the Scheduler to run every 30 minutes (by default).
		 */
		TaskManager.instance().scheduleAsyncRepeatingTask( this, 0L, AppConfig.get().getInt( "auto-updater.check-interval", 30 ) * Ticks.MINUTE, new Runnable()
		{
			@Override
			public void run()
			{
				check();
			}
		} );

		try
		{
			AppConfig.get();
			serverJarMD5 = AppConfig.getApplicationJar().exists() && AppConfig.getApplicationJar().isFile() ? SecureFunc.md5( FileUtils.readFileToByteArray( AppConfig.getApplicationJar() ) ) : null;

			if ( serverJarMD5 != null )
				ServerFileWatcher.instance().register( AppConfig.get().getDirectory(), new EventCallback()
				{
					@Override
					public void call( Kind<?> kind, File file, boolean isDirectory )
					{
						getLogger().debug( String.format( "%s: %s", kind.name(), file ) );

						AppConfig.get();
						AppConfig.get();
						if ( AppConfig.getApplicationJar().exists() && AppConfig.getApplicationJar().isFile() )
							if ( file.getAbsolutePath().equals( AppConfig.getApplicationJar().getAbsolutePath() ) && AppConfig.get().getBoolean( "auto-updater.auto-restart", true ) )
								if ( AppLoader.isWatchdogRunning() )
								{
									String newServerJarMD5 = null;
									try
									{
										newServerJarMD5 = SecureFunc.md5( FileUtils.readFileToByteArray( AppConfig.getApplicationJar() ) );
									}
									catch ( IOException e )
									{
										e.printStackTrace();
									}

									if ( serverJarMD5 == null || !serverJarMD5.equals( newServerJarMD5 ) )
										AppController.restartApplication( "We detected modification to the server jar, the server will now restart to apply changes." );
								}
								else
									getLogger().warning( "We detected a change to the server jar, but the Watchdog process is not running." );

						if ( file.getAbsolutePath().equals( AppConfig.get().file() ) )
						{
							getLogger().info( "We detected a change in the server configuration file, reloading!" );
							AppConfig.get().reload();
						}
					}
				} );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public void check()
	{
		check( ApplicationTerminal.terminal(), true );
	}

	public void check( final AccountAttachment sender, final boolean automatic )
	{
		final String currentSlug = Versioning.getBuildNumber();

		if ( !isEnabled() || "0".equals( currentSlug ) )
			return;

		if ( !sender.getEntity().checkPermission( "sys.update" ).isTrue() && !sender.getEntity().isOp() )
			return;

		new Thread()
		{
			@Override
			public void run()
			{
				current = service.getArtifact( currentSlug, "information about this version; perhaps you are running a custom one?" );
				latest = service.getArtifact( "lastStableBuild", "latest artifact information" );

				try
				{
					if ( isUpdateAvailable() )
					{
						if ( current.isBroken() && onBroken.contains( WARN_CONSOLE ) )
						{
							sender.sendMessage( EnumColor.RED + "----- Chiori Auto Updater -----" );
							sender.sendMessage( EnumColor.RED + "Your version of " + Versioning.getProduct() + " is known to be broken. It is strongly advised that you update to a more recent version ASAP." );
							sender.sendMessage( EnumColor.RED + "Known issues with your version:" );

							for ( String line : current.getBrokenReason().split( "\n" ) )
								sender.sendMessage( EnumColor.RED + "> " + line );

							sender.sendMessage( EnumColor.RED + "Newer version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
							sender.sendMessage( EnumColor.RED + "Details: " + latest.getHtmlUrl() );
							sender.sendMessage( EnumColor.RED + "Download: " + latest.getJar() );
							sender.sendMessage( EnumColor.RED + "----- ------------------- -----" );
						}
						else if ( onUpdate.contains( WARN_CONSOLE ) )
						{
							sender.sendMessage( EnumColor.YELLOW + "----- Chiori Auto Updater -----" );
							sender.sendMessage( EnumColor.YELLOW + "Your version of " + Versioning.getProduct() + " is out of date. Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
							sender.sendMessage( EnumColor.YELLOW + "Details: " + latest.getHtmlUrl() );
							sender.sendMessage( EnumColor.YELLOW + "Download: " + latest.getJar() );
							sender.sendMessage( EnumColor.YELLOW + "----- ------------------- -----" );
						}
					}
					else if ( current != null && current.isBroken() && onBroken.contains( WARN_CONSOLE ) )
					{
						sender.sendMessage( EnumColor.RED + "----- Chiori Auto Updater -----" );
						sender.sendMessage( EnumColor.RED + "Your version of " + Versioning.getProduct() + " is known to be broken. It is strongly advised that you update to a more recent (or older) version ASAP." );
						sender.sendMessage( EnumColor.RED + "Known issues with your version:" );

						for ( String line : current.getBrokenReason().split( "\n" ) )
							sender.sendMessage( EnumColor.RED + "> " + line );

						sender.sendMessage( EnumColor.RED + "Unfortunately, there is not yet a newer version suitable for your server. We would advise you wait an hour or two, or try out a dev build." );
						sender.sendMessage( EnumColor.RED + "----- ------------------- -----" );
					}
					else if ( current == null && latest != null )
					{
						sender.sendMessage( EnumColor.YELLOW + "----- Chiori Auto Updater -----" );
						sender.sendMessage( EnumColor.YELLOW + "It appears that we could not find any information regarding your current build of Chiori Web Server. This could either be due to your" );
						sender.sendMessage( EnumColor.YELLOW + "version being so out of date that our Build Server has no information or you self compiled this build, in which case you should have disabled" );
						sender.sendMessage( EnumColor.YELLOW + "the auto updates. For the sake of fair warning below is our latest release. Please run \"update latest\" if you like us to auto update." );
						sender.sendMessage( EnumColor.YELLOW + "" );
						sender.sendMessage( EnumColor.YELLOW + "Latest Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
						sender.sendMessage( EnumColor.YELLOW + "Details: " + latest.getHtmlUrl() );
						sender.sendMessage( EnumColor.YELLOW + "Download: " + latest.getJar() );
						sender.sendMessage( EnumColor.YELLOW + "----- ------------------- -----" );
					}
					else if ( !automatic )
					{
						sender.sendMessage( EnumColor.YELLOW + "----- Chiori Auto Updater -----" );

						if ( current == null && latest == null )
							sender.sendMessage( EnumColor.YELLOW + "There seems to have been a problem checking for updates!" );
						else
							sender.sendMessage( EnumColor.YELLOW + "You are already running the latest version of " + Versioning.getProduct() + "!" );

						sender.sendMessage( EnumColor.YELLOW + "----- ------------------- -----" );
					}
				}
				catch ( Throwable t )
				{
					t.printStackTrace();
				}
			}
		}.start();
	}

	public void forceUpdate( final AccountAttachment sender )
	{
		new Thread()
		{
			@Override
			public void run()
			{
				current = Versioning.getBuildNumber().equals( "0" ) ? null : service.getArtifact( Versioning.getBuildNumber(), "information about this " + Versioning.getProduct() + " version; perhaps you are running a custom one?" );
				latest = service.getArtifact( "lastStableBuild", "latest artifact information" );

				if ( latest == null )
				{
					sender.sendMessage( EnumColor.YELLOW + "----- Chiori Auto Updater -----" );
					sender.sendMessage( EnumColor.YELLOW + "There seems to have been a problem checking for updates!" );
					sender.sendMessage( EnumColor.YELLOW + "----- ------------------- -----" );
				}
				else
				{
					sender.sendMessage( EnumColor.YELLOW + "----- Chiori Auto Updater -----" );

					if ( current != null )
						sender.sendMessage( EnumColor.YELLOW + "Your Version " + current.getVersion() + " (build #" + current.getBuildNumber() + ") was released on " + current.getCreated() + "." );

					sender.sendMessage( EnumColor.YELLOW + "Latest Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
					sender.sendMessage( EnumColor.YELLOW + "Details: " + latest.getHtmlUrl() );
					sender.sendMessage( EnumColor.YELLOW + "Download: " + latest.getJar() );
					sender.sendMessage( "" );

					for ( ChangeSetDetails l : latest.getChanges() )
						for ( String ll : l.toString().split( "\n" ) )
							sender.sendMessage( EnumColor.AQUA + "[CHANGES] " + EnumColor.WHITE + ll );

					sender.sendMessage( "" );
					sender.sendMessage( EnumColor.YELLOW + "If you would like " + Versioning.getProduct() + " to update to the latest version run \"update latest force\"" );
					sender.sendMessage( EnumColor.RED + "WARNING: Chiori Auto Updater currently can't auto update any installed plugins." );
					sender.sendMessage( EnumColor.RED + "You can obtain updated offical plugins from the Details URL above or you will need to contact the original developer." );
					sender.sendMessage( EnumColor.RED + "Quite frankly, If there has been no changes to the Plugin API (See Change Log) then even outdated plugins should still work." );
				}
			}
		}.start();
	}

	public String getChannel()
	{
		return channel;
	}

	public BuildArtifact getCurrent()
	{
		return current;
	}

	public BuildArtifact getLatest()
	{
		return latest;
	}

	@Override
	public String getLoggerId()
	{
		return "Updater";
	}

	@Override
	public String getName()
	{
		return "Auto Updater";
	}

	public List<String> getOnBroken()
	{
		return onBroken;
	}

	public List<String> getOnUpdate()
	{
		return onUpdate;
	}

	@Override
	public void init() throws ApplicationException
	{

	}

	@Override
	public boolean isEnabled()
	{
		return enabled;
	}

	public boolean isUpdateAvailable()
	{
		if ( latest == null || current == null || !isEnabled() )
			return false;
		else
			return latest.timestamp > current.timestamp;
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onAccountLoginEvent( AccountSuccessfulLoginEvent event )
	{
		if ( event.getAccountPermissible() instanceof MessageReceiver )
		{
			MessageReceiver receiver = ( MessageReceiver ) event.getAccountPermissible();
			if ( isEnabled() && getCurrent() != null && event.getAccount().getEntity().checkPermission( AppController.BROADCAST_CHANNEL_ADMINISTRATIVE ).isTrue() )
				if ( getCurrent().isBroken() && getOnBroken().contains( AutoUpdater.WARN_OPERATORS ) )
					receiver.sendMessage( EnumColor.DARK_RED + "The version of " + Versioning.getProduct() + " that this server is running is known to be broken. Please consider updating to the latest version available from http://jenkins.chiorichan.com/." );
				else if ( isUpdateAvailable() && getOnUpdate().contains( AutoUpdater.WARN_OPERATORS ) )
					receiver.sendMessage( EnumColor.DARK_PURPLE + "The version of " + Versioning.getProduct() + " that this server is running is out of date. Please consider updating to the latest version available from http://jenkins.chiorichan.com/." );
		}
	}

	public void setEnabled( boolean isEnabled )
	{
		enabled = isEnabled;
		if ( enabled )
			EventBus.instance().registerEvents( this, AppLoader.instances().get( 0 ) );
		else
			EventBus.instance().unregisterEvents( this );
	}

	public void setSuggestChannels( boolean suggestChannels )
	{
		this.suggestChannels = suggestChannels;
	}

	public boolean shouldSuggestChannels()
	{
		return suggestChannels;
	}
}
