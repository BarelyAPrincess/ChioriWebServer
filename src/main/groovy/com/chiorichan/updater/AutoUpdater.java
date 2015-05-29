/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.updater;

import java.util.ArrayList;
import java.util.List;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Account;
import com.chiorichan.account.AccountType;
import com.chiorichan.account.event.AccountPreLoginEvent;
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.scheduler.ScheduleManager;
import com.chiorichan.scheduler.TaskCreator;
import com.chiorichan.updater.BuildArtifact.ChangeSet.ChangeSetDetails;
import com.chiorichan.util.Versioning;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class AutoUpdater extends BuiltinEventCreator implements Listener, TaskCreator
{
	public static final String WARN_CONSOLE = "warn-console";
	public static final String WARN_OPERATORS = "warn-ops";
	
	private static AutoUpdater instance = null;
	private final DownloadUpdaterService service;
	private final List<String> onUpdate = new ArrayList<String>();
	private final List<String> onBroken = new ArrayList<String>();
	private final String channel;
	private boolean enabled;
	private BuildArtifact current = null;
	private BuildArtifact latest = null;
	private boolean suggestChannels = true;
	
	public AutoUpdater( DownloadUpdaterService service, String channel )
	{
		instance = this;
		this.service = service;
		this.channel = channel;
		
		/*
		 * This schedules the Auto Updater with the Scheduler to run every 30 minutes (by default).
		 */
		ScheduleManager.INSTANCE.scheduleAsyncRepeatingTask( this, new Runnable()
		{
			@Override
			public void run()
			{
				check();
			}
		}, 0L, Loader.getConfig().getInt( "auto-updater.check-interval", 30 ) * 3000 ); // 3000 ticks = 1 minute
	}
	
	public String getChannel()
	{
		return channel;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled( boolean isEnabled )
	{
		enabled = isEnabled;
		
		if ( enabled )
			EventBus.INSTANCE.registerEvents( this, this );
		// else
		// EventBus.INSTANCE.unregisterEvents( this );
	}
	
	public boolean shouldSuggestChannels()
	{
		return suggestChannels;
	}
	
	public void setSuggestChannels( boolean suggestChannels )
	{
		this.suggestChannels = suggestChannels;
	}
	
	public List<String> getOnBroken()
	{
		return onBroken;
	}
	
	public List<String> getOnUpdate()
	{
		return onUpdate;
	}
	
	public boolean isUpdateAvailable()
	{
		if ( ( latest == null ) || ( current == null ) || ( !isEnabled() ) )
		{
			return false;
		}
		else
		{
			return latest.timestamp > current.timestamp;
		}
	}
	
	public BuildArtifact getCurrent()
	{
		return current;
	}
	
	public BuildArtifact getLatest()
	{
		return latest;
	}
	
	public void check()
	{
		// Makes an anonymous update check. Unless sys.updater.allow is granted to the noLogin account, the update will fail silently.
		check( AccountType.ACCOUNT_NONE, true );
	}
	
	public void check( final Account sender, final boolean automatic )
	{
		final String currentSlug = Versioning.getBuildNumber();
		
		if ( !isEnabled() || Versioning.getBuildNumber().equals( "0" ) )
		{
			return;
		}
		
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
						if ( ( current.isBroken() ) && ( onBroken.contains( WARN_CONSOLE ) ) )
						{
							sender.send( ConsoleColor.RED + "----- Chiori Auto Updater -----" );
							sender.send( ConsoleColor.RED + "Your version of " + Versioning.getProduct() + " is known to be broken. It is strongly advised that you update to a more recent version ASAP." );
							sender.send( ConsoleColor.RED + "Known issues with your version:" );
							
							for ( String line : current.getBrokenReason().split( "\n" ) )
							{
								sender.send( ConsoleColor.RED + "> " + line );
							}
							
							sender.send( ConsoleColor.RED + "Newer version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
							sender.send( ConsoleColor.RED + "Details: " + latest.getHtmlUrl() );
							sender.send( ConsoleColor.RED + "Download: " + latest.getJar() );
							sender.send( ConsoleColor.RED + "----- ------------------- -----" );
						}
						else if ( onUpdate.contains( WARN_CONSOLE ) )
						{
							sender.send( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
							sender.send( ConsoleColor.YELLOW + "Your version of " + Versioning.getProduct() + " is out of date. Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
							sender.send( ConsoleColor.YELLOW + "Details: " + latest.getHtmlUrl() );
							sender.send( ConsoleColor.YELLOW + "Download: " + latest.getJar() );
							sender.send( ConsoleColor.YELLOW + "----- ------------------- -----" );
						}
					}
					else if ( ( current != null ) && ( current.isBroken() ) && ( onBroken.contains( WARN_CONSOLE ) ) )
					{
						sender.send( ConsoleColor.RED + "----- Chiori Auto Updater -----" );
						sender.send( ConsoleColor.RED + "Your version of " + Versioning.getProduct() + " is known to be broken. It is strongly advised that you update to a more recent (or older) version ASAP." );
						sender.send( ConsoleColor.RED + "Known issues with your version:" );
						
						for ( String line : current.getBrokenReason().split( "\n" ) )
						{
							sender.send( ConsoleColor.RED + "> " + line );
						}
						
						sender.send( ConsoleColor.RED + "Unfortunately, there is not yet a newer version suitable for your server. We would advise you wait an hour or two, or try out a dev build." );
						sender.send( ConsoleColor.RED + "----- ------------------- -----" );
					}
					else if ( current == null && latest != null )
					{
						sender.send( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
						sender.send( ConsoleColor.YELLOW + "It appears that we could not find any information regarding your current build of Chiori Web Server. This could either be due to your" );
						sender.send( ConsoleColor.YELLOW + "version being so out of date that our Build Server has no information or you self compiled this build, in which case you should have disabled" );
						sender.send( ConsoleColor.YELLOW + "the auto updates. For the sake of fair warning below is our latest release. Please run \"update latest\" if you like us to auto update." );
						sender.send( ConsoleColor.YELLOW + "" );
						sender.send( ConsoleColor.YELLOW + "Latest Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
						sender.send( ConsoleColor.YELLOW + "Details: " + latest.getHtmlUrl() );
						sender.send( ConsoleColor.YELLOW + "Download: " + latest.getJar() );
						sender.send( ConsoleColor.YELLOW + "----- ------------------- -----" );
					}
					
					/*
					 * else if ( ( current != null ) && ( shouldSuggestChannels() ) )
					 * {
					 * ArtifactDetails.ChannelDetails prefChan = service.getChannel( channel, "preferred channel details" );
					 * if ( ( prefChan != null ) && ( current.getChannel().getPriority() < prefChan.getPriority() ) )
					 * {
					 * sender.send( ChatColor.AQUA + "----- Chiori Auto Updater -----" );
					 * sender.send( ChatColor.AQUA + "It appears that you're running a " + current.getChannel().getName() + ", when you've specified in chiori.yml that you prefer to run " + prefChan.getName() + "s." );
					 * sender.send( ChatColor.AQUA + "If you would like to be kept informed about new " + current.getChannel().getName() +
					 * " releases, it is recommended that you change 'preferred-channel' in your chiori.yml to '" +
					 * current.getChannel().getSlug() + "'." );
					 * sender.send( ChatColor.AQUA + "With that set, you will be told whenever a new version is available for download, so that you can always keep up to date and secure with the latest fixes." );
					 * sender.send( ChatColor.AQUA + "If you would like to disable this warning, simply set 'suggest-channels' to false in chiori.yml." );
					 * sender.send( ChatColor.AQUA + "----- ------------------- -----" );
					 * }
					 * }
					 */
					else
					// if ( !( sender instanceof ConsoleCommandSender ) )
					{
						if ( !automatic )
						{
							sender.send( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
							
							if ( current == null && latest == null )
								sender.send( ConsoleColor.YELLOW + "There seems to have been a problem checking for updates!" );
							else
								sender.send( ConsoleColor.YELLOW + "You are already running the latest version of " + Versioning.getProduct() + "!" );
							
							sender.send( ConsoleColor.YELLOW + "----- ------------------- -----" );
						}
					}
				}
				catch ( Throwable t )
				{
					t.printStackTrace();
				}
			}
		}.start();
	}
	
	public void forceUpdate( final Account sender )
	{
		new Thread()
		{
			@Override
			public void run()
			{
				current = ( Versioning.getBuildNumber().equals( "0" ) ) ? null : service.getArtifact( Versioning.getBuildNumber(), "information about this " + Versioning.getProduct() + " version; perhaps you are running a custom one?" );
				latest = service.getArtifact( "lastStableBuild", "latest artifact information" );
				
				if ( latest == null )
				{
					sender.send( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
					sender.send( ConsoleColor.YELLOW + "There seems to have been a problem checking for updates!" );
					sender.send( ConsoleColor.YELLOW + "----- ------------------- -----" );
				}
				else
				{
					sender.send( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
					
					if ( current != null )
						sender.send( ConsoleColor.YELLOW + "Your Version " + current.getVersion() + " (build #" + current.getBuildNumber() + ") was released on " + current.getCreated() + "." );
					
					sender.send( ConsoleColor.YELLOW + "Latest Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
					sender.send( ConsoleColor.YELLOW + "Details: " + latest.getHtmlUrl() );
					sender.send( ConsoleColor.YELLOW + "Download: " + latest.getJar() );
					sender.send( "" );
					
					for ( ChangeSetDetails l : latest.getChanges() )
						for ( String ll : l.toString().split( "\n" ) )
							sender.send( ConsoleColor.AQUA + "[CHANGES] " + ConsoleColor.WHITE + ll );
					
					sender.send( "" );
					sender.send( ConsoleColor.YELLOW + "If you would like " + Versioning.getProduct() + " to update to the latest version run \"update latest force\"" );
					sender.send( ConsoleColor.RED + "WARNING: Chiori Auto Updater currently can't auto update any installed plugins." );
					sender.send( ConsoleColor.RED + "You can obtain updated offical plugins from the Details URL above or you will need to contact the original developer." );
					sender.send( ConsoleColor.RED + "Quite frankly, If there has been no changes to the Plugin API (See Change Log) then even outdated plugins should still work." );
				}
			}
		}.start();
	}
	
	protected static DownloadUpdaterService getService()
	{
		return ( instance == null ) ? null : instance.service;
	}
	
	@Override
	public String getName()
	{
		return "Auto Updater";
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onAccountLoginEvent( AccountPreLoginEvent event )
	{
		if ( ( Loader.getAutoUpdater().isEnabled() ) && ( Loader.getAutoUpdater().getCurrent() != null ) && ( event.getAccount().instance().checkPermission( Loader.BROADCAST_CHANNEL_ADMINISTRATIVE ).isTrue() ) )
			if ( ( Loader.getAutoUpdater().getCurrent().isBroken() ) && ( getOnBroken().contains( AutoUpdater.WARN_OPERATORS ) ) )
				event.getAccount().send( ConsoleColor.DARK_RED + "The version of " + Versioning.getProduct() + " that this server is running is known to be broken. Please consider updating to the latest version at jenkins.chiorichan.com." );
			else if ( ( Loader.getAutoUpdater().isUpdateAvailable() ) && ( getOnUpdate().contains( AutoUpdater.WARN_OPERATORS ) ) )
				event.getAccount().send( ConsoleColor.DARK_PURPLE + "The version of " + Versioning.getProduct() + " that this server is running is out of date. Please consider updating to the latest version at jenkins.chiorichan.com." );
	}
}
