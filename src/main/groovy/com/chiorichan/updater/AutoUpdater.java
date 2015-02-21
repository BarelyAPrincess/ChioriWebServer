/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.updater;

import java.util.ArrayList;
import java.util.List;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.InteractiveEntity;
import com.chiorichan.event.BuiltinEventCreator;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.account.AccountLoginEvent;
import com.chiorichan.updater.BuildArtifact.ChangeSet.ChangeSetDetails;
import com.chiorichan.util.Versioning;

public class AutoUpdater extends BuiltinEventCreator implements Listener
{
	public static final String WARN_CONSOLE = "warn-console";
	public static final String WARN_OPERATORS = "warn-ops";
	
	private static AutoUpdater instance = null;
	private final ChioriDLUpdaterService service;
	private final List<String> onUpdate = new ArrayList<String>();
	private final List<String> onBroken = new ArrayList<String>();
	private final String channel;
	private boolean enabled;
	private BuildArtifact current = null;
	private BuildArtifact latest = null;
	private boolean suggestChannels = true;
	
	public AutoUpdater( ChioriDLUpdaterService service, String channel )
	{
		instance = this;
		this.service = service;
		this.channel = channel;
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
			Loader.getEventBus().registerEvents( this, this );
		// else
		// Loader.getEventBus().unregisterEvents( this );
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
		// check( , true );
	}
	
	public void check( final InteractiveEntity sender, final boolean automatic )
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
							sender.sendMessage( ConsoleColor.RED + "----- Chiori Auto Updater -----" );
							sender.sendMessage( ConsoleColor.RED + "Your version of " + Versioning.getProduct() + " is known to be broken. It is strongly advised that you update to a more recent version ASAP." );
							sender.sendMessage( ConsoleColor.RED + "Known issues with your version:" );
							
							for ( String line : current.getBrokenReason().split( "\n" ) )
							{
								sender.sendMessage( ConsoleColor.RED + "> " + line );
							}
							
							sender.sendMessage( ConsoleColor.RED + "Newer version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
							sender.sendMessage( ConsoleColor.RED + "Details: " + latest.getHtmlUrl() );
							sender.sendMessage( ConsoleColor.RED + "Download: " + latest.getJar() );
							sender.sendMessage( ConsoleColor.RED + "----- ------------------- -----" );
						}
						else if ( onUpdate.contains( WARN_CONSOLE ) )
						{
							sender.sendMessage( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
							sender.sendMessage( ConsoleColor.YELLOW + "Your version of " + Versioning.getProduct() + " is out of date. Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
							sender.sendMessage( ConsoleColor.YELLOW + "Details: " + latest.getHtmlUrl() );
							sender.sendMessage( ConsoleColor.YELLOW + "Download: " + latest.getJar() );
							sender.sendMessage( ConsoleColor.YELLOW + "----- ------------------- -----" );
						}
					}
					else if ( ( current != null ) && ( current.isBroken() ) && ( onBroken.contains( WARN_CONSOLE ) ) )
					{
						sender.sendMessage( ConsoleColor.RED + "----- Chiori Auto Updater -----" );
						sender.sendMessage( ConsoleColor.RED + "Your version of " + Versioning.getProduct() + " is known to be broken. It is strongly advised that you update to a more recent (or older) version ASAP." );
						sender.sendMessage( ConsoleColor.RED + "Known issues with your version:" );
						
						for ( String line : current.getBrokenReason().split( "\n" ) )
						{
							sender.sendMessage( ConsoleColor.RED + "> " + line );
						}
						
						sender.sendMessage( ConsoleColor.RED + "Unfortunately, there is not yet a newer version suitable for your server. We would advise you wait an hour or two, or try out a dev build." );
						sender.sendMessage( ConsoleColor.RED + "----- ------------------- -----" );
					}
					else if ( current == null && latest != null )
					{
						sender.sendMessage( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
						sender.sendMessage( ConsoleColor.YELLOW + "It appears that we could not find any information regarding your current build of Chiori Web Server. This could either be due to your" );
						sender.sendMessage( ConsoleColor.YELLOW + "version being so out of date that our Build Server has no information or you self compiled this build, in which case you should have disabled" );
						sender.sendMessage( ConsoleColor.YELLOW + "the auto updates. For the sake of fair warning below is our latest release. Please run \"update latest\" if you like us to auto update." );
						sender.sendMessage( ConsoleColor.YELLOW + "" );
						sender.sendMessage( ConsoleColor.YELLOW + "Latest Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
						sender.sendMessage( ConsoleColor.YELLOW + "Details: " + latest.getHtmlUrl() );
						sender.sendMessage( ConsoleColor.YELLOW + "Download: " + latest.getJar() );
						sender.sendMessage( ConsoleColor.YELLOW + "----- ------------------- -----" );
					}
					
					/*
					 * else if ( ( current != null ) && ( shouldSuggestChannels() ) )
					 * {
					 * ArtifactDetails.ChannelDetails prefChan = service.getChannel( channel, "preferred channel details" );
					 * if ( ( prefChan != null ) && ( current.getChannel().getPriority() < prefChan.getPriority() ) )
					 * {
					 * sender.sendMessage( ChatColor.AQUA + "----- Chiori Auto Updater -----" );
					 * sender.sendMessage( ChatColor.AQUA + "It appears that you're running a " + current.getChannel().getName() + ", when you've specified in chiori.yml that you prefer to run " + prefChan.getName() + "s." );
					 * sender.sendMessage( ChatColor.AQUA + "If you would like to be kept informed about new " + current.getChannel().getName() +
					 * " releases, it is recommended that you change 'preferred-channel' in your chiori.yml to '" +
					 * current.getChannel().getSlug() + "'." );
					 * sender.sendMessage( ChatColor.AQUA + "With that set, you will be told whenever a new version is available for download, so that you can always keep up to date and secure with the latest fixes." );
					 * sender.sendMessage( ChatColor.AQUA + "If you would like to disable this warning, simply set 'suggest-channels' to false in chiori.yml." );
					 * sender.sendMessage( ChatColor.AQUA + "----- ------------------- -----" );
					 * }
					 * }
					 */
					else
					// if ( !( sender instanceof ConsoleCommandSender ) )
					{
						if ( !automatic )
						{
							sender.sendMessage( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
							
							if ( current == null && latest == null )
								sender.sendMessage( ConsoleColor.YELLOW + "There seems to have been a problem checking for updates!" );
							else
								sender.sendMessage( ConsoleColor.YELLOW + "You are already running the latest version of " + Versioning.getProduct() + "!" );
							
							sender.sendMessage( ConsoleColor.YELLOW + "----- ------------------- -----" );
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
	
	public void forceUpdate( final InteractiveEntity sender )
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
					sender.sendMessage( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
					sender.sendMessage( ConsoleColor.YELLOW + "There seems to have been a problem checking for updates!" );
					sender.sendMessage( ConsoleColor.YELLOW + "----- ------------------- -----" );
				}
				else
				{
					sender.sendMessage( ConsoleColor.YELLOW + "----- Chiori Auto Updater -----" );
					
					if ( current != null )
						sender.sendMessage( ConsoleColor.YELLOW + "Your Version " + current.getVersion() + " (build #" + current.getBuildNumber() + ") was released on " + current.getCreated() + "." );
					
					sender.sendMessage( ConsoleColor.YELLOW + "Latest Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
					sender.sendMessage( ConsoleColor.YELLOW + "Details: " + latest.getHtmlUrl() );
					sender.sendMessage( ConsoleColor.YELLOW + "Download: " + latest.getJar() );
					sender.sendMessage( "" );
					
					for ( ChangeSetDetails l : latest.getChanges() )
						for ( String ll : l.toString().split( "\n" ) )
							sender.sendMessage( ConsoleColor.AQUA + "[CHANGES] " + ConsoleColor.WHITE + ll );
					
					sender.sendMessage( "" );
					sender.sendMessage( ConsoleColor.YELLOW + "If you would like " + Versioning.getProduct() + " to update to the latest version run \"update latest force\"" );
					sender.sendMessage( ConsoleColor.RED + "WARNING: Chiori Auto Updater currently can't auto update any installed plugins." );
					sender.sendMessage( ConsoleColor.RED + "You can obtain updated offical plugins from the Details URL above or you will need to contact the original developer." );
					sender.sendMessage( ConsoleColor.RED + "Quite frankly, If there has been no changes to the Plugin API (See Change Log) then even outdated plugins should still work." );
				}
			}
		}.start();
	}
	
	protected static ChioriDLUpdaterService getService()
	{
		return ( instance == null ) ? null : instance.service;
	}
	
	@Override
	public String getName()
	{
		return "Auto Updater";
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onAccountLoginEvent( AccountLoginEvent event )
	{
		if ( ( Loader.getAutoUpdater().isEnabled() ) && ( Loader.getAutoUpdater().getCurrent() != null ) && ( event.getHandler().checkPermission( Loader.BROADCAST_CHANNEL_ADMINISTRATIVE ).isTrue() ) )
			if ( ( Loader.getAutoUpdater().getCurrent().isBroken() ) && ( getOnBroken().contains( AutoUpdater.WARN_OPERATORS ) ) )
				event.getHandler().sendMessage( ConsoleColor.DARK_RED + "The version of " + Versioning.getProduct() + " that this server is running is known to be broken. Please consider updating to the latest version at jenkins.chiorichan.com." );
			else if ( ( Loader.getAutoUpdater().isUpdateAvailable() ) && ( getOnUpdate().contains( AutoUpdater.WARN_OPERATORS ) ) )
				event.getHandler().sendMessage( ConsoleColor.DARK_PURPLE + "The version of " + Versioning.getProduct() + " that this server is running is out of date. Please consider updating to the latest version at jenkins.chiorichan.com." );
	}
}
