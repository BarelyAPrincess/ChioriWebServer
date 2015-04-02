/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
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
import com.chiorichan.exception.StartupException;
import com.chiorichan.http.HttpInitializer;
import com.chiorichan.https.HttpsInitializer;
import com.chiorichan.net.query.QueryServerInitializer;
import com.chiorichan.util.Common;

public class NetworkManager
{
	public static EventLoopGroup bossGroup = new NioEventLoopGroup( 4 );
	public static EventLoopGroup workerGroup = new NioEventLoopGroup();
	
	public static Channel httpChannel = null;
	public static Channel httpsChannel = null;
	public static Channel queryChannel = null;
	public static Channel tcpChannel = null;
	
	/**
	 * Only effects Unit-like OS'es (Linux and Mac OS X)
	 * Will return false if the port is under 1024 and we are not running as root.
	 * It's possible to give non-root users access to Privileged Ports but it's
	 * complicated for Java Apps and a Security Risk.
	 *
	 * @param port
	 * @return Is this a privileged port?
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
	
	public static void shutdownHttpServer()
	{
		if ( httpChannel != null && httpChannel.isOpen() )
			httpChannel.close();
	}
	
	public static void shutdownHttpsServer()
	{
		if ( httpsChannel != null && httpsChannel.isOpen() )
			httpsChannel.close();
	}
	
	public static void shutdownTcpServer()
	{
		if ( tcpChannel != null && tcpChannel.isOpen() )
			tcpChannel.close();
	}
	
	public static void shutdownQueryServer()
	{
		if ( queryChannel != null && queryChannel.isOpen() )
			queryChannel.close();
	}
	
	public static void startHttpServer() throws StartupException
	{
		if ( httpChannel != null && httpChannel.isOpen() )
			throw new StartupException( "The HTTP Server is already running" );
		
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
				
				Loader.getLogger().info( "Starting Web Server on " + ( httpIp.isEmpty() ? "*" : httpIp ) + ":" + httpPort );
				
				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new HttpInitializer() );
					
					httpChannel = b.bind( socket ).sync().channel();
					
					Thread thread = new Thread( "HTTP Server Thread" )
					{
						public void run()
						{
							try
							{
								httpChannel.closeFuture().sync();
							}
							catch ( InterruptedException e )
							{
								e.printStackTrace();
							}
							finally
							{
								// bossGroup.shutdownGracefully();
								// workerGroup.shutdownGracefully();
							}
						}
					};
					thread.start();
				}
				catch ( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch ( Throwable e )
				{
					Loader.getLogger().warning( "**** FAILED TO BIND HTTP SERVER TO PORT!" );
					// Loader.getLogger().warning( "The exception was: {0}", new Object[] {e.toString()} );
					Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
					
					throw new StartupException( e );
				}
			}
		}
		catch ( Throwable e )
		{
			throw new StartupException( e );
		}
	}
	
	public static void startHttpsServer() throws StartupException
	{
		if ( httpsChannel != null && httpsChannel.isOpen() )
			throw new StartupException( "The HTTPS Server is already running" );
		
		try
		{
			InetSocketAddress socket;
			String httpIp = Loader.getConfig().getString( "server.httpHost", "" );
			int httpsPort = Loader.getConfig().getInt( "server.httpsPort", 4443 );
			
			if ( httpsPort >= 1 )
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
				
				Loader.getLogger().info( "Starting Secure Web Server on " + ( httpIp.isEmpty() ? "*" : httpIp ) + ":" + httpsPort );
				
				File sslCert = new File( Loader.getConfig().getString( "server.httpsKeystore", "server.keystore" ) );
				
				if ( !sslCert.exists() )
					throw new StartupException( sslCert.getAbsolutePath() + " We could not start the HTTPS Server because the '" + sslCert.getName() + "' (aka. SSL Cert) file does not exist. Please generate one and reload the server, or disable SSL in the configs." );
				
				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new HttpsInitializer() );
					
					httpsChannel = b.bind( socket ).sync().channel();
					
					Thread thread = new Thread( "HTTPS Server Thread" )
					{
						public void run()
						{
							try
							{
								httpsChannel.closeFuture().sync();
							}
							catch ( InterruptedException e )
							{
								e.printStackTrace();
							}
							finally
							{
								// bossGroup.shutdownGracefully();
								// workerGroup.shutdownGracefully();
								
								Loader.getLogger().info( "The HTTPS Server has been shutdown!" );
							}
						}
					};
					thread.start();
				}
				catch ( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch ( Throwable e )
				{
					Loader.getLogger().warning( "**** FAILED TO BIND HTTPS SERVER TO PORT!" );
					Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
					
					throw new StartupException( e );
				}
			}
		}
		catch ( Throwable e )
		{
			throw new StartupException( e );
		}
	}
	
	public static void startQueryServer() throws StartupException
	{
		if ( queryChannel != null && queryChannel.isOpen() )
			throw new StartupException( "The Query Server is already running" );
		
		try
		{
			InetSocketAddress socket;
			String queryHost = Loader.getConfig().getString( "server.queryHost", "" );
			int queryPort = Loader.getConfig().getInt( "server.queryPort", 4443 );
			
			if ( queryPort >= 1 && Loader.getConfig().getBoolean( "server.queryEnabled" ) )
			{
				if ( !checkPrivilegedPort( queryPort ) )
				{
					Loader.getLogger().warning( "It would seem that you are trying to start the Query Server on a privileged port without root access." );
					Loader.getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					Loader.getLogger().warning( "It's recommended that you either run CWS on a port like 8080 then use the firewall to redirect or run as root if you must use port: " + queryPort );
				}
				
				if ( queryHost.isEmpty() )
					socket = new InetSocketAddress( queryPort );
				else
					socket = new InetSocketAddress( queryHost, queryPort );
				
				Loader.getLogger().info( "Starting Query Server on " + ( queryHost.isEmpty() ? "*" : queryHost ) + ":" + queryPort );
				
				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new QueryServerInitializer() );
					
					queryChannel = b.bind( socket ).sync().channel();
					
					Thread thread = new Thread( "Query Server Thread" )
					{
						public void run()
						{
							try
							{
								queryChannel.closeFuture().sync();
							}
							catch ( InterruptedException e )
							{
								e.printStackTrace();
							}
							finally
							{
								// bossGroup.shutdownGracefully();
								// workerGroup.shutdownGracefully();
							}
						}
					};
					thread.start();
				}
				catch ( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch ( Throwable e )
				{
					Loader.getLogger().warning( "**** FAILED TO BIND QUERY SERVER TO PORT!" );
					Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
					
					throw new StartupException( e );
				}
			}
		}
		catch ( Throwable e )
		{
			throw new StartupException( e );
		}
	}
	
	public static void startTcpServer() throws StartupException
	{
		// XXX TCP IS TEMPORARY REMOVED UNTIL IT CAN BE PORTED TO NETTY.
	}
	
	public static void cleanup()
	{
		if ( httpChannel != null && httpChannel.isOpen() )
			httpChannel.close();
		
		if ( httpsChannel != null && httpsChannel.isOpen() )
			httpsChannel.close();
		
		if ( tcpChannel != null && tcpChannel.isOpen() )
			tcpChannel.close();
		
		if ( queryChannel != null && queryChannel.isOpen() )
			queryChannel.close();
		
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
}
