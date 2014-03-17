package com.chiorichan.command.defaults;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.command.CommandSender;
import com.chiorichan.command.ConsoleCommandSender;
import com.chiorichan.updater.BuildArtifact;
import com.chiorichan.updater.Download;
import com.chiorichan.updater.DownloadListener;
import com.google.common.base.Strings;

public class UpdateCommand extends ChioriCommand
{
	public UpdateCommand()
	{
		super( "update" );
		
		this.description = "Gets the version of this server including any plugins in use";
		this.usageMessage = "/update [latest]";
		this.setPermission( "chiori.command.update" );
	}
	
	@Override
	public boolean execute( CommandSender sender, String currentAlias, String[] args )
	{
		if ( !Loader.getInstance().getAutoUpdater().isEnabled() )
		{
			sender.sendMessage( ChatColor.RED + "I'm sorry but updates are disabled on this server per configs!" );
			return true;
		}
		
		if ( Loader.getConfig().getBoolean( "auto-updater.console-only" ) && !( sender instanceof ConsoleCommandSender ) )
		{
			sender.sendMessage( ChatColor.RED + "I'm sorry but updates can only be performed from the console!" );
			return true;
		}
		
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.AQUA + "Please wait as we check for updates..." );
			Loader.getInstance().getAutoUpdater().check( sender );
		}
		else
		{
			if ( args[0].equalsIgnoreCase( "latest" ) )
			{
				try
				{
					if ( args.length > 1 && args[1].equalsIgnoreCase( "force" ) )
					{
						BuildArtifact latest = Loader.getInstance().getAutoUpdater().getLatest();
						
						if ( latest == null )
							sender.sendMessage( ChatColor.RED + "Please review the latest version without \"force\" arg before updating." );
						else
						{
							sender.sendMessage( ChatColor.YELLOW + "The server is now going into standby mode... Please wait as we download the latest version of Chiori Web Server..." );
							
							File currentJar = new File( URLDecoder.decode( Loader.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8" ) );
							File updatedJar = new File( "update.jar" );
							
							Download download = new Download( new URL( latest.getFile() ), updatedJar.getName(), updatedJar.getPath() );
							download.setListener( new DownloadProgressDisplay( sender ) );
							download.run();
							
							/*
							 * ProcessBuilder processBuilder = new ProcessBuilder();
							 * ArrayList<String> commands = new ArrayList<String>();
							 * if ( !codeSource.getName().endsWith( ".exe" ) )
							 * {
							 * if ( OperatingSystem.getOperatingSystem().equals( OperatingSystem.WINDOWS ) )
							 * {
							 * commands.add( "javaw" );
							 * }
							 * else
							 * {
							 * commands.add( "java" );
							 * }
							 * commands.add( "-Xmx256m" );
							 * commands.add( "-cp" );
							 * commands.add( temp.getAbsolutePath() );
							 * commands.add( Mover.class.getName() );
							 * }
							 * else
							 * {
							 * commands.add( temp.getAbsolutePath() );
							 * commands.add( "-Mover" );
							 * }
							 * commands.add( codeSource.getAbsolutePath() );
							 * commands.addAll( Arrays.asList( args ) );
							 * processBuilder.command( commands );
							 * try
							 * {
							 * processBuilder.start();
							 * }
							 * catch ( Exception e )
							 * {
							 * e.printStackTrace();
							 * }
							 * System.exit( 0 );
							 */
						}
					}
					else
					{
						sender.sendMessage( ChatColor.AQUA + "Please wait as we poll the Jenkins Build Server..." );
						Loader.getInstance().getAutoUpdater().forceUpdate( sender );
					}
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		
		return true;
	}
	
	private static class DownloadProgressDisplay implements DownloadListener
	{
		private final CommandSender sender;
		
		DownloadProgressDisplay(CommandSender _sender)
		{
			sender = _sender;
		}
		
		@Override
		public void stateChanged( String text, float progress )
		{
			sender.sendMessage( ChatColor.YELLOW + "Please wait as we complete your download." );
			sender.sendMessage( ChatColor.DARK_AQUA + "+----------------------------------------------------------------------------------------------------+" );
			sender.sendMessage( ChatColor.DARK_AQUA + "|" + Strings.repeat( "X", Math.round( progress ) ) + Strings.repeat( " ", Math.round( 100 - progress ) ) + "|" );
			sender.sendMessage( ChatColor.DARK_AQUA + "+----------------------------------------------------------------------------------------------------+" );
		}
	}
}
