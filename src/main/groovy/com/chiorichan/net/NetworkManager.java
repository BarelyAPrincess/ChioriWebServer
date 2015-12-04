/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerBus;
import com.chiorichan.http.HttpInitializer;
import com.chiorichan.https.HttpsInitializer;
import com.chiorichan.https.HttpsManager;
import com.chiorichan.lang.StartupException;
import com.chiorichan.net.query.QueryServerInitializer;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.util.Versioning;

/**
 * Works as the main network managing class for netty implementations, e.g., Http, Https, and Query
 */
public class NetworkManager implements TaskRegistrar
{
	@SuppressWarnings( "unused" )
	private static final NetworkManager SELF = new NetworkManager();
	
	public static EventLoopGroup bossGroup = new NioEventLoopGroup( 1 );
	public static EventLoopGroup workerGroup = new NioEventLoopGroup( 100 );
	
	public static Channel httpChannel = null;
	public static Channel httpsChannel = null;
	public static Channel queryChannel = null;
	public static Channel tcpChannel = null;
	
	private NetworkManager()
	{
		TaskManager.INSTANCE.scheduleAsyncRepeatingTask( this, Ticks.SECOND_15, Ticks.SECOND_15, new Runnable()
		{
			@Override
			public void run()
			{
				for ( WeakReference<SocketChannel> ref : HttpInitializer.activeChannels )
					if ( ref.get() == null )
						HttpInitializer.activeChannels.remove( ref );
			}
		} );
	}
	
	private static void close( Channel channel )
	{
		try
		{
			if ( channel != null && channel.isOpen() )
				channel.close();
		}
		catch ( Throwable t )
		{
			// Ignore
		}
	}
	
	public static APILogger getLogger()
	{
		return Loader.getLogger( "NetMgr" );
	}
	
	public static boolean isHttpRunning()
	{
		return httpChannel != null && httpChannel.isOpen();
	}
	
	public static boolean isHttpsRunning()
	{
		return httpsChannel != null && httpsChannel.isOpen();
	}
	
	public static boolean isQueryRunning()
	{
		return queryChannel != null && queryChannel.isOpen();
	}
	
	public static boolean isTcpRunning()
	{
		return false;
	}
	
	public static void shutdown()
	{
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		
		close( httpChannel );
		close( httpsChannel );
		close( tcpChannel );
		close( queryChannel );
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
	
	public static void shutdownQueryServer()
	{
		if ( queryChannel != null && queryChannel.isOpen() )
			queryChannel.close();
	}
	
	public static void shutdownTcpServer()
	{
		if ( tcpChannel != null && tcpChannel.isOpen() )
			tcpChannel.close();
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
				if ( Versioning.isPrivilegedPort( httpPort ) )
				{
					Loader.getLogger().warning( "It would seem that you are trying to start ChioriWebServer's Web Server on a privileged port without root access." );
					Loader.getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					Loader.getLogger().warning( "It's recommended that you either run CWS on a port like 8080 then use the firewall to redirect from 80 or run as root if you must use port: " + httpPort );
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
					
					// HTTP Server Thread
					ServerBus.registerRunnable( new Runnable()
					{
						@Override
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
							
							Loader.getLogger().info( "The HTTP Server has been shutdown!" );
						}
					} );
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
				if ( Versioning.isPrivilegedPort( httpsPort ) )
				{
					Loader.getLogger().warning( "It would seem that you are trying to start ChioriWebServer's Web Server (SSL) on a privileged port without root access." );
					Loader.getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					Loader.getLogger().warning( "It's recommended that you either run CWS (SSL) on a port like 4443 then use the firewall to redirect from 443 or run as root if you must use port: " + httpsPort );
				}
				
				if ( httpIp.isEmpty() )
					socket = new InetSocketAddress( httpsPort );
				else
					socket = new InetSocketAddress( httpIp, httpsPort );
				
				HttpsManager.init();
				
				Loader.getLogger().info( "Starting Secure Web Server on " + ( httpIp.isEmpty() ? "*" : httpIp ) + ":" + httpsPort );
				
				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new HttpsInitializer() );
					
					httpsChannel = b.bind( socket ).sync().channel();
					
					// HTTPS Server Thread
					ServerBus.registerRunnable( new Runnable()
					{
						@Override
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
							
							Loader.getLogger().info( "The HTTPS Server has been shutdown!" );
						}
					} );
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
			int queryPort = Loader.getConfig().getInt( "server.queryPort", 8992 );
			
			if ( queryPort >= 1 && Loader.getConfig().getBoolean( "server.queryEnabled" ) )
			{
				if ( Versioning.isPrivilegedPort( queryPort ) )
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
					
					// Query Server Thread
					ServerBus.registerRunnable( new Runnable()
					{
						@Override
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
							
							Loader.getLogger().info( "The Query Server has been shutdown!" );
						}
					} );
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
	
	@Override
	public String getName()
	{
		return "NetMgr";
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
}
