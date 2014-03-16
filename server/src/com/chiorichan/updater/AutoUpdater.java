package com.chiorichan.updater;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.chiorichan.Loader;
import com.chiorichan.util.Versioning;

public class AutoUpdater
{
	public static final String WARN_CONSOLE = "warn-console";
	public static final String WARN_OPERATORS = "warn-ops";
	
	private static AutoUpdater instance = null;
	private final ChioriDLUpdaterService service;
	private final List<String> onUpdate = new ArrayList<String>();
	private final List<String> onBroken = new ArrayList<String>();
	private final Logger log;
	private final String channel;
	private boolean enabled;
	private BuildArtifact current = null;
	private BuildArtifact latest = null;
	private boolean suggestChannels = true;
	
	public AutoUpdater(ChioriDLUpdaterService service, Logger log, String channel)
	{
		instance = this;
		this.service = service;
		this.log = log;
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
		this.enabled = isEnabled;
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
	
	public void check( final String currentSlug )
	{
		String logMsg = "Running auto update checker";
		
		if ( !isEnabled() )
		{
			return;
		}
		/*
		 * if ( Versioning.getBuildNumber() == "0" )
		 * {
		 * Loader.getLogger().info( logMsg + ".....DISABLED, RUNNING IN DEV MODE!" );
		 * return;
		 * }
		 */
		Loader.getLogger().info( logMsg + ".....Please Wait." );
		
		new Thread()
		{
			@Override
			public void run()
			{
				current = service.getArtifact( currentSlug, "information about this Chiori Web Server version; perhaps you are running a custom one?" );
				latest = service.getArtifact( "lastStableBuild", "latest artifact information" );
				
				if ( isUpdateAvailable() )
				{
					if ( ( current.isBroken() ) && ( onBroken.contains( WARN_CONSOLE ) ) )
					{
						log.severe( "----- Chiori Auto Updater -----" );
						log.severe( "Your version of Chiori Web Server is known to be broken. It is strongly advised that you update to a more recent version ASAP." );
						log.severe( "Known issues with your version:" );
						
						for ( String line : current.getBrokenReason().split( "\n" ) )
						{
							log.severe( "> " + line );
						}
						
						log.severe( "Newer version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
						log.severe( "Details: " + latest.getHtmlUrl() );
						log.severe( "Download: " + latest.getFile() );
						log.severe( "----- ------------------- -----" );
					}
					else if ( onUpdate.contains( WARN_CONSOLE ) )
					{
						log.warning( "----- Chiori Auto Updater -----" );
						log.warning( "Your version of Chiori Web Server is out of date. Version " + latest.getVersion() + " (build #" + latest.getBuildNumber() + ") was released on " + latest.getCreated() + "." );
						log.warning( "Details: " + latest.getHtmlUrl() );
						log.warning( "Download: " + latest.getFile() );
						log.warning( "----- ------------------- -----" );
					}
				}
				else if ( ( current != null ) && ( current.isBroken() ) && ( onBroken.contains( WARN_CONSOLE ) ) )
				{
					log.severe( "----- Chiori Auto Updater -----" );
					log.severe( "Your version of Chiori Web Server is known to be broken. It is strongly advised that you update to a more recent version ASAP." );
					log.severe( "Known issues with your version:" );
					
					for ( String line : current.getBrokenReason().split( "\n" ) )
					{
						log.severe( "> " + line );
					}
					
					log.severe( "Unfortunately, there is not yet a newer version suitable for your server. We would advise you wait an hour or two, or try out a dev build." );
					log.severe( "----- ------------------- -----" );
				}/*
				else if ( ( current != null ) && ( shouldSuggestChannels() ) )
				{
					ArtifactDetails.ChannelDetails prefChan = service.getChannel( channel, "preferred channel details" );
					
					if ( ( prefChan != null ) && ( current.getChannel().getPriority() < prefChan.getPriority() ) )
					{
						log.info( "----- Chiori Auto Updater -----" );
						log.info( "It appears that you're running a " + current.getChannel().getName() + ", when you've specified in chiori.yml that you prefer to run " + prefChan.getName() + "s." );
						log.info( "If you would like to be kept informed about new " + current.getChannel().getName() + " releases, it is recommended that you change 'preferred-channel' in your chiori.yml to '" + current.getChannel().getSlug() + "'." );
						log.info( "With that set, you will be told whenever a new version is available for download, so that you can always keep up to date and secure with the latest fixes." );
						log.info( "If you would like to disable this warning, simply set 'suggest-channels' to false in chiori.yml." );
						log.info( "----- ------------------- -----" );
					}
				}*/
			}
		}.start();
	}
	
	protected static ChioriDLUpdaterService getService()
	{
		return ( instance == null ) ? null : instance.service;
	}
}
