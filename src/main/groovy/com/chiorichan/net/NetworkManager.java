/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.File;
import java.net.InetSocketAddress;

import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.http.HttpInitializer;
import com.chiorichan.https.HttpsInitializer;
import com.chiorichan.util.Common;

public class NetworkManager
{
	public static EventLoopGroup bossGroup = new NioEventLoopGroup( 1 );
	public static EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	private static String SSLData;
	
	/**
	 * Only effects Unit-like OS'es (Linux and Mac OS X)
	 * Will return false if the port is under 1024 and we are not running as root.
	 * It's possible to give non-root users access to Privileged Ports but it's
	 * complicated for Java Apps and a Security Risk.
	 *
	 * @param port
	 * @return
	 */
	public static boolean checkPrivilegedPort( int port )
	{
		// Privilaged Ports only exist on Linux, Unix, and Mac OS X (I know I'm missing some)
		if ( !System.getProperty( "os.name" ).equalsIgnoreCase( "linux" ) || !System.getProperty( "os.name" ).equalsIgnoreCase( "unix" ) || !System.getProperty( "os.name" ).equalsIgnoreCase( "mac os x" ) )
			return true;
		
		// Privilaged Port range from 0 to 1024
		if ( port > 1024 )
			return true;
		
		// If we are trying to use a Privilaged Port, We need to be running as root
		return Common.isRoot();
	}
	
	public static void initTcpServer()
	{
		// TCP IS TEMPORARY REMOVED UNTIL IT CAN BE PORTED TO NETTY.
	}
	
	public static void initWebServer() throws StartupException
	{
		try
		{
			InetSocketAddress socket;
			String httpIp = Loader.getConfig().getString( "server.httpHost", "" );
			int httpPort = Loader.getConfig().getInt( "server.httpPort", 8080 );
			
			if ( httpPort > 0 )
			{
				if ( !checkPrivilegedPort( httpPort ) )
				{
					Loader.getLogger().warning( "It would seem that you are trying to start ChioriWebServer's Web Server on a privileged port without root access." );
					Loader.getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					Loader.getLogger().warning( "It's recommended that you either run CWS on a port like 8080 then use the firewall to redirect or run as root if you must use port: " + httpPort );
				}
				
				if ( httpIp.isEmpty() )
					socket = new InetSocketAddress( httpPort );
				else
					socket = new InetSocketAddress( httpIp, httpPort );
				
				Loader.getLogger().info( "Starting Web Server on " + (httpIp.length() == 0 ? "*" : httpIp) + ":" + httpPort );
				
				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new HttpInitializer() );
					
					final Channel ch = b.bind( socket ).sync().channel();
					
					Thread thread = new Thread( "HTTP Server Thread" )
					{
						public void run()
						{
							try
							{
								ch.closeFuture().sync();
							}
							catch( InterruptedException e )
							{
								e.printStackTrace();
							}
							finally
							{
								bossGroup.shutdownGracefully();
								workerGroup.shutdownGracefully();
							}
						}
					};
					thread.start();
				}
				catch( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch( Throwable e )
				{
					Loader.getLogger().warning( "**** FAILED TO BIND WEB SERVER TO PORT!" );
					// Loader.getLogger().warning( "The exception was: {0}", new Object[] {e.toString()} );
					Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
					
					throw new StartupException( e );
				}
			}
			
			int httpsPort = Loader.getConfig().getInt( "server.httpsPort", 4443 );
			
			if ( httpsPort > 0 )
			{
				
				if ( !checkPrivilegedPort( httpsPort ) )
				{
					Loader.getLogger().warning( "It would seem that you are trying to start ChioriWebServer's Web Server on a privileged port without root access." );
					Loader.getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					Loader.getLogger().warning( "It's recommended that you either run CWS on a port like 8080 then use the firewall to redirect or run as root if you must use port: " + httpsPort );
				}
				
				if ( httpIp.isEmpty() )
					socket = new InetSocketAddress( httpsPort );
				else
					socket = new InetSocketAddress( httpIp, httpsPort );
				
				Loader.getLogger().info( "Starting Web Server on " + (httpIp.length() == 0 ? "*" : httpIp) + ":" + httpsPort );
				
				File sslCert = new File( Loader.getRoot(), Loader.getConfig().getString( "server.httpsKeystore", "server.keystore" ) );
				
				if ( !sslCert.exists() )
					throw new StartupException( sslCert.getAbsolutePath() + " We could not start the HTTPS Server because the '" + sslCert.getName() + "' (aka. SSL Cert) file does not exist. Please generate one and reload the server, or disable SSL in the configs." );
				
				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new HttpsInitializer() );
					
					final Channel ch = b.bind( socket ).sync().channel();
					
					Thread thread = new Thread( "HTTPS Server Thread" )
					{
						public void run()
						{
							try
							{
								ch.closeFuture().sync();
							}
							catch( InterruptedException e )
							{
								e.printStackTrace();
							}
							finally
							{
								bossGroup.shutdownGracefully();
								workerGroup.shutdownGracefully();
								
								Loader.getLogger().info( "The HTTPS Server has been shutdown!" );
							}
						}
					};
					thread.start();
				}
				catch( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch( Throwable e )
				{
					Loader.getLogger().warning( "**** FAILED TO BIND WEB SERVER TO PORT!" );
					// Loader.getLogger().warning( "The exception was: {0}", new Object[] {e.toString()} );
					Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
					
					throw new StartupException( e );
				}
			}
		}
		catch( Throwable e )
		{
			throw new StartupException( e );
		}
	}
	
	public static void cleanup()
	{
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
	
	public static String getSSLData()
	{
		return SSLData;
	}
}
