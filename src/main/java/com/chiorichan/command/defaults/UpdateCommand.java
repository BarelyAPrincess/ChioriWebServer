package com.chiorichan.command.defaults;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.ThreadCommandReader;
import com.chiorichan.account.bases.SentientHandler;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.updater.BuildArtifact;
import com.chiorichan.updater.Download;
import com.chiorichan.updater.DownloadListener;
import com.chiorichan.util.Versioning;
import com.google.common.base.Strings;

public class UpdateCommand extends ChioriCommand
{
	public UpdateCommand()
	{
		super( "update" );
		
		this.description = "Gets the version of this server including any plugins in use";
		this.usageMessage = "update [latest]";
		this.setPermission( "chiori.command.update" );
	}
	
	@Override
	public boolean execute( SentientHandler sender, String currentAlias, String[] args )
	{
		if ( !Loader.getAutoUpdater().isEnabled() )
		{
			sender.sendMessage( ChatColor.RED + "I'm sorry but updates are disabled per configs!" );
			return true;
		}
		
		if ( Loader.getConfig().getBoolean( "auto-updater.console-only" ) && !( sender instanceof ThreadCommandReader ) )
		{
			sender.sendMessage( ChatColor.RED + "I'm sorry but updates can only be performed from the console!" );
			return true;
		}
		
		if ( !testPermission( sender ) )
			return true;
		
		if ( args.length > 0 )
		{
			if ( args[0].equalsIgnoreCase( "latest" ) )
			{
				try
				{
					if ( args.length > 1 && args[1].equalsIgnoreCase( "force" ) )
					{
						BuildArtifact latest = Loader.getAutoUpdater().getLatest();
						
						if ( latest == null )
							sender.sendMessage( ChatColor.RED + "Please review the latest version without \"force\" arg before updating." );
						else
						{
							sender.sendMessage( ChatColor.YELLOW + "Please wait as we download the latest version of Chiori Web Server..." );
							
							File currentJar = new File( URLDecoder.decode( Loader.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8" ) );
							File updatedJar = new File( "update.jar" );
							File backupJar = new File( currentJar.getAbsolutePath() + ".bak" );
							
							Download download = new Download( new URL( latest.getJar() ), updatedJar.getName(), updatedJar.getPath() );
							download.setListener( new DownloadProgressDisplay( sender ) );
							download.run();
							
							String origMD5 = new String( WebUtils.readUrl( latest.getMD5File() ) ).trim();
							
							if ( origMD5 != null && !origMD5.isEmpty() )
							{
								String descMD5 = DigestUtils.md5Hex( new FileInputStream( updatedJar.getPath() ) ).trim();
								
								if ( descMD5.equals( origMD5 ) )
									sender.sendMessage( ChatColor.AQUA + "SUCCESS: The downloaded jar and control MD5Checksum matched, '" + origMD5 + "'" );
								else
								{
									sender.sendMessage( ChatColor.RED + "ERROR: The server said the downloaded jar should have a MD5Checksum of '" + origMD5 + "' but it had the MD5Checksum '" + descMD5 + "', UPDATE ABORTED!!!" );
									FileUtils.deleteQuietly( updatedJar );
									return true;
								}
							}
							
							try
							{
								backupJar.delete();
								FileUtils.moveFile( currentJar, backupJar );
							}
							catch ( Exception e )
							{
								e.printStackTrace();
							}
							
							FileInputStream fis = null;
							FileOutputStream fos = null;
							try
							{
								fis = new FileInputStream( updatedJar );
								fos = new FileOutputStream( currentJar );
								IOUtils.copy( fis, fos );
							}
							catch ( IOException e )
							{
								e.printStackTrace();
							}
							finally
							{
								IOUtils.closeQuietly( fis );
								IOUtils.closeQuietly( fos );
							}
							
							currentJar.setExecutable( true, true );
							updatedJar.delete();
							
							String newMD5 = DigestUtils.md5Hex( new FileInputStream( currentJar.getPath() ) ).trim();
							
							if ( origMD5 != null && !origMD5.isEmpty() && newMD5.equals( origMD5 ) )
							{
								sender.sendMessage( ChatColor.AQUA + "----- Chiori Auto Updater -----" );
								sender.sendMessage( ChatColor.AQUA + "SUCCESS: The downloaded jar was successfully installed in the place of your old one." );
								sender.sendMessage( ChatColor.AQUA + "If you have a problem with the updated jar, you can find a backup jar file at: " + backupJar.getAbsolutePath() );
								sender.sendMessage( ChatColor.AQUA + "You will need to restart " + Versioning.getProduct() + " for the changes to take effect." );
								sender.sendMessage( ChatColor.AQUA + "Please type 'stop' and press enter to make this happen, otherwise you may encounter unexpected problems!" );
								sender.sendMessage( ChatColor.AQUA + "----- ------------------- -----" );
							}
							else
							{
								currentJar.delete();
								FileUtils.moveFile( backupJar, currentJar );
								
								sender.sendMessage( ChatColor.YELLOW + "----- Chiori Auto Updater -----" );
								sender.sendMessage( ChatColor.RED + "SEVERE: There was a problem installing the downloaded jar in the place of your old one." );
								sender.sendMessage( ChatColor.RED + "Don't worry we restored your original jar file." );
								sender.sendMessage( ChatColor.RED + "Try redownloading this version yourself from: " + latest.getJar() );
								sender.sendMessage( ChatColor.RED + "Details: " + latest.getHtmlUrl() );
								sender.sendMessage( ChatColor.YELLOW + "----- ------------------- -----" );
							}
							
							// Disable updater until next boot.
							Loader.getAutoUpdater().setEnabled( false );
							
							/*
							 * TODO It would be nice if the server could automatically restart the server.
							 * But there has been problems with this sadly.
							 */
							
							/*
							 * ProcessBuilder processBuilder = new ProcessBuilder();
							 * List<String> commands = new ArrayList<String>();
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
							 * commands.add( updatedJar.getAbsolutePath() );
							 * commands.add( UpdateInstaller.class.getName() );
							 * commands.add( currentJar.getAbsolutePath() );
							 * commands.add( "" + Runtime.getRuntime().maxMemory() );
							 * // commands.addAll( Arrays.asList( args ) );
							 * processBuilder.command( commands );
							 * try
							 * {
							 * Process process = processBuilder.start();
							 * process.exitValue();
							 * Loader.getLogger().severe( "The Auto Updater failed to start. You can find the new Server Version at \"update.jar\"" );
							 * }
							 * catch ( IllegalThreadStateException e )
							 * {
							 * Loader.stop( "The server is now going down to apply the latest version." );
							 * }
							 * catch ( Exception e )
							 * {
							 * e.printStackTrace();
							 * }
							 */
						}
					}
					else
					{
						sender.sendMessage( ChatColor.AQUA + "Please wait as we poll the Jenkins Build Server..." );
						Loader.getAutoUpdater().forceUpdate( sender );
					}
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}
			else
			{
				args = new String[0];
			}
		}
		
		if ( args.length == 0 )
		{
			sender.sendMessage( ChatColor.AQUA + "Please wait as we check for updates..." );
			Loader.getAutoUpdater().check( sender, false );
		}
		
		return true;
	}
	
	private static class DownloadProgressDisplay implements DownloadListener
	{
		private final SentientHandler sender;
		
		DownloadProgressDisplay(SentientHandler _sender)
		{
			sender = _sender;
			sender.sendMessage( "" );
		}
		
		@Override
		public void stateChanged( String text, float progress )
		{
			sender.sendMessage( ChatColor.YELLOW + "" + ChatColor.NEGATIVE + text + " -> " + Math.round( progress ) + "% completed! " + ChatColor.DARK_AQUA + "[" + Strings.repeat( "=", Math.round( progress ) ) + Strings.repeat( " ", Math.round( 100 - progress ) ) + "]\r" );
		}
		
		@Override
		public void stateDone()
		{
			sender.sendMessage( "\n" );
		}
	}
}
