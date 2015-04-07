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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;

import com.chiorichan.Loader;
import com.chiorichan.util.WebUtils;

public class Download implements Runnable
{
	private static final long TIMEOUT = 30000;
	
	private URL url;
	private long size = -1;
	private long downloaded = 0;
	private String outPath;
	private String name;
	private DownloadListener listener;
	private Result result = Result.FAILURE;
	private File outFile = null;
	private Exception exception = null;
	
	public Download( URL url, String name, String outPath ) throws MalformedURLException
	{
		this.url = url;
		this.outPath = outPath;
		this.name = name;
	}
	
	public float getProgress()
	{
		return ( ( float ) downloaded / size ) * 100;
	}
	
	public Exception getException()
	{
		return exception;
	}
	
	@Override
	public void run()
	{
		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;
		try
		{
			HttpURLConnection conn = WebUtils.openHttpConnection( url );
			int response = conn.getResponseCode();
			int responseFamily = response / 100;
			
			if ( responseFamily == 3 )
			{
				throw new DownloadException( "The server issued a redirect response which the Updater failed to follow." );
			}
			else if ( responseFamily != 2 )
			{
				throw new DownloadException( "The server issued a " + response + " response code." );
			}
			
			InputStream in = getConnectionInputStream( conn );
			
			size = conn.getContentLength();
			outFile = new File( outPath );
			outFile.delete();
			
			rbc = Channels.newChannel( in );
			fos = new FileOutputStream( outFile );
			
			stateChanged();
			
			Thread progress = new MonitorThread( Thread.currentThread(), rbc );
			progress.start();
			
			fos.getChannel().transferFrom( rbc, 0, size > 0 ? size : Integer.MAX_VALUE );
			in.close();
			rbc.close();
			progress.interrupt();
			if ( size > 0 )
			{
				if ( size == outFile.length() )
				{
					result = Result.SUCCESS;
				}
			}
			else
			{
				result = Result.SUCCESS;
			}
			
			stateDone();
		}
		catch ( PermissionDeniedException e )
		{
			exception = e;
			result = Result.PERMISSION_DENIED;
		}
		catch ( DownloadException e )
		{
			exception = e;
			result = Result.FAILURE;
		}
		catch ( Exception e )
		{
			exception = e;
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly( fos );
			IOUtils.closeQuietly( rbc );
		}
		
		if ( exception != null )
			Loader.getLogger().severe( "Download Resulted in an Exception", exception );
	}
	
	protected InputStream getConnectionInputStream( final URLConnection urlconnection ) throws DownloadException
	{
		final AtomicReference<InputStream> is = new AtomicReference<InputStream>();
		
		for ( int j = 0; ( j < 3 ) && ( is.get() == null ); j++ )
		{
			StreamThread stream = new StreamThread( urlconnection, is );
			stream.start();
			int iterationCount = 0;
			while ( ( is.get() == null ) && ( iterationCount++ < 5 ) )
			{
				try
				{
					stream.join( 1000L );
				}
				catch ( InterruptedException ignore )
				{
				}
			}
			
			if ( stream.permDenied.get() )
			{
				throw new PermissionDeniedException( "Permission denied!" );
			}
			
			if ( is.get() != null )
			{
				break;
			}
			try
			{
				stream.interrupt();
				stream.join();
			}
			catch ( InterruptedException ignore )
			{
			}
		}
		
		if ( is.get() == null )
		{
			throw new DownloadException( "Unable to download file from " + urlconnection.getURL() );
		}
		return new BufferedInputStream( is.get() );
	}
	
	private void stateDone()
	{
		if ( listener != null )
		{
			listener.stateChanged( "Download Done!", 100 );
			listener.stateDone();
		}
	}
	
	private void stateChanged()
	{
		if ( listener != null )
			listener.stateChanged( name, getProgress() );
	}
	
	public void setListener( DownloadListener listener )
	{
		this.listener = listener;
	}
	
	public Result getResult()
	{
		return result;
	}
	
	public File getOutFile()
	{
		return outFile;
	}
	
	private static class StreamThread extends Thread
	{
		private final URLConnection urlconnection;
		private final AtomicReference<InputStream> is;
		public final AtomicBoolean permDenied = new AtomicBoolean( false );
		
		public StreamThread( URLConnection urlconnection, AtomicReference<InputStream> is )
		{
			this.urlconnection = urlconnection;
			this.is = is;
		}
		
		@Override
		public void run()
		{
			try
			{
				is.set( urlconnection.getInputStream() );
			}
			catch ( SocketException e )
			{
				if ( e.getMessage().equalsIgnoreCase( "Permission denied: connect" ) )
				{
					permDenied.set( true );
				}
			}
			catch ( IOException ignore )
			{
			}
		}
	}
	
	private class MonitorThread extends Thread
	{
		private final ReadableByteChannel rbc;
		private final Thread downloadThread;
		private long last = System.currentTimeMillis();
		
		public MonitorThread( Thread downloadThread, ReadableByteChannel rbc )
		{
			super( "Download Monitor Thread" );
			this.setDaemon( true );
			this.rbc = rbc;
			this.downloadThread = downloadThread;
		}
		
		@Override
		public void run()
		{
			while ( !this.isInterrupted() )
			{
				long diff = outFile.length() - downloaded;
				downloaded = outFile.length();
				if ( diff == 0 )
				{
					if ( ( System.currentTimeMillis() - last ) > TIMEOUT )
					{
						if ( listener != null )
						{
							listener.stateChanged( "Download Failed", getProgress() );
						}
						try
						{
							rbc.close();
							downloadThread.interrupt();
						}
						catch ( Exception ignore )
						{
							// We catch all exceptions here, because ReadableByteChannel is AWESOME
							// and was throwing NPE's sometimes when we tried to close it after
							// the connection broke.
						}
						return;
					}
				}
				else
				{
					last = System.currentTimeMillis();
				}
				
				stateChanged();
				try
				{
					sleep( 50 );
				}
				catch ( InterruptedException ignore )
				{
					return;
				}
			}
		}
	}
	
	public enum Result
	{
		SUCCESS, FAILURE, PERMISSION_DENIED,
	}
}
