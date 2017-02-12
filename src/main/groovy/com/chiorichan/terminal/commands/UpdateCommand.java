/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.terminal.commands;

import com.chiorichan.AppConfig;
import com.chiorichan.AppController;
import com.chiorichan.Loader;
import com.chiorichan.account.AccountAttachment;
import com.chiorichan.zutils.ZIO;
import com.chiorichan.zutils.ZHttp;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.terminal.Command;
import com.chiorichan.updater.AutoUpdater;
import com.chiorichan.updater.BuildArtifact;
import com.chiorichan.updater.Download;
import com.chiorichan.updater.DownloadListener;
import com.chiorichan.Versioning;
import joptsimple.internal.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

public class UpdateCommand extends Command
{
	private static class DownloadProgressDisplay implements DownloadListener
	{
		private final AccountAttachment sender;

		DownloadProgressDisplay( AccountAttachment sender )
		{
			this.sender = sender;
			sender.sendMessage( "" );
		}

		@Override
		public void stateChanged( String text, float progress )
		{
			sender.sendMessage( EnumColor.YELLOW + "" + EnumColor.NEGATIVE + text + " -> " + Math.round( progress ) + "% completed! " + EnumColor.DARK_AQUA + "[" + Strings.repeat( '=', Math.round( progress ) ) + Strings.repeat( ' ', Math.round( 100 - progress ) ) + "]\r" );
		}

		@Override
		public void stateDone()
		{
			sender.sendMessage( "\n" );
		}
	}

	public UpdateCommand()
	{
		super( "update", "sys.update" );

		setDescription( "Check for updates and optionally updates to the latest build" );
		setUsage( "/update [latest]" );
	}

	@Override
	public boolean execute( AccountAttachment sender, String command, String[] args )
	{
		if ( !AutoUpdater.instance().isEnabled() )
		{
			sender.sendMessage( EnumColor.RED + "I'm sorry but updates are disabled per configs!" );
			return true;
		}

		if ( AppConfig.get().getBoolean( "auto-updater.console-only" ) && !( sender instanceof AccountAttachment ) )
		{
			sender.sendMessage( EnumColor.RED + "I'm sorry but updates can only be performed from the console!" );
			return true;
		}

		if ( !testPermission( sender ) )
			return true;

		if ( args.length > 0 )
			if ( args[0].equalsIgnoreCase( "latest" ) )
				try
				{
					if ( args.length > 1 && args[1].equalsIgnoreCase( "force" ) )
					{
						BuildArtifact latest = AutoUpdater.instance().getLatest();

						if ( latest == null )
							sender.sendMessage( EnumColor.RED + "Please review the latest version without \"force\" arg before updating." );
						else
						{
							sender.sendMessage( EnumColor.YELLOW + "Please wait as we download the latest version of Chiori Web Server..." );

							// TODO Add support for class files

							File currentJar = new File( URLDecoder.decode( Loader.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8" ) );
							File updatedJar = new File( "update.jar" );

							Download download = new Download( new URL( latest.getJar() ), updatedJar.getName(), updatedJar.getPath() );
							download.setListener( new DownloadProgressDisplay( sender ) );
							download.run();

							String origMD5 = new String( ZHttp.readUrl( latest.getMD5File() ) ).trim();

							if ( origMD5 != null && !origMD5.isEmpty() )
							{
								String descMD5 = DigestUtils.md5Hex( new FileInputStream( updatedJar.getPath() ) ).trim();

								if ( descMD5.equals( origMD5 ) )
									sender.sendMessage( EnumColor.AQUA + "SUCCESS: The downloaded jar and control MD5Checksum matched, '" + origMD5 + "'" );
								else
								{
									sender.sendMessage( EnumColor.RED + "ERROR: The server said the downloaded jar should have a MD5Checksum of '" + origMD5 + "' but it had the MD5Checksum '" + descMD5 + "', UPDATE ABORTED!!!" );
									FileUtils.deleteQuietly( updatedJar );
									return true;
								}
							}

							currentJar.delete();
							FileInputStream fis = null;
							FileOutputStream fos = null;
							try
							{
								fis = new FileInputStream( updatedJar );
								fos = new FileOutputStream( currentJar );
								ZIO.copy( fis, fos );
							}
							catch ( IOException e )
							{
								e.printStackTrace();
							}
							finally
							{
								ZIO.closeQuietly( fis );
								ZIO.closeQuietly( fos );
							}

							updatedJar.setExecutable( true, true );

							String newMD5 = DigestUtils.md5Hex( new FileInputStream( currentJar.getPath() ) ).trim();

							if ( origMD5 != null && !origMD5.isEmpty() && newMD5.equals( origMD5 ) )
							{
								sender.sendMessage( EnumColor.AQUA + "----- Chiori Auto Updater -----" );
								sender.sendMessage( EnumColor.AQUA + "SUCCESS: The downloaded jar was successfully installed in the place of your old one." );
								sender.sendMessage( EnumColor.AQUA + "You will need to restart " + Versioning.getProduct() + " for the changes to take effect." );
								sender.sendMessage( EnumColor.AQUA + "Please type 'stop' and press enter to make this happen, otherwise you may encounter unexpected problems!" );
								sender.sendMessage( EnumColor.AQUA + "----- ------------------- -----" );
							}
							else
							{
								sender.sendMessage( EnumColor.YELLOW + "----- Chiori Auto Updater -----" );
								sender.sendMessage( EnumColor.RED + "SEVERE: There was a problem installing the downloaded jar in the place of your old one." );
								sender.sendMessage( EnumColor.RED + "Sorry about that because most likely your current jar is now corrupt" );
								sender.sendMessage( EnumColor.RED + "Try downloading this version yourself from: " + latest.getJar() );
								sender.sendMessage( EnumColor.RED + "Details: " + latest.getHtmlUrl() );
								sender.sendMessage( EnumColor.YELLOW + "----- ------------------- -----" );
							}

							// Disable updater until next boot.
							AutoUpdater.instance().setEnabled( false );

							AppController.restartApplication( "The update was successfully downloaded, restarting to apply it." );
						}
					}
					else
					{
						sender.sendMessage( EnumColor.AQUA + "Please wait as we poll the Jenkins Build Server..." );
						AutoUpdater.instance().forceUpdate( sender );
					}
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			else
				args = new String[0];

		if ( args.length == 0 )
		{
			sender.sendMessage( EnumColor.AQUA + "Please wait as we check for updates..." );
			AutoUpdater.instance().check( sender, false );
		}

		return true;
	}
}
