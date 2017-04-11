/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.net;

import com.chiorichan.AppConfig;
import com.chiorichan.AppController;
import com.chiorichan.Loader;
import com.chiorichan.http.HttpInitializer;
import com.chiorichan.http.ssl.SslInitializer;
import com.chiorichan.http.ssl.SslManager;
import com.chiorichan.lang.StartupException;
import com.chiorichan.logger.Log;
import com.chiorichan.logger.LogSource;
import com.chiorichan.net.query.QueryServerInitializer;
import com.chiorichan.services.AppManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.TaskRegistrar;
import com.chiorichan.tasks.Ticks;
import com.chiorichan.utils.UtilSystem;
import com.google.common.collect.Lists;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import joptsimple.OptionSet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.Security;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * Works as the main network managing class for netty implementations, e.g., Http, Https, and Query
 */
public class NetworkManager implements TaskRegistrar, LogSource
{
	private static final NetworkManager SELF = new NetworkManager();

	private static EventLoopGroup bossGroup = new NioEventLoopGroup( 1 );
	private static EventLoopGroup workerGroup = new NioEventLoopGroup( 100 );

	private static Channel httpChannel = null;
	private static Channel httpsChannel = null;
	private static Channel queryChannel = null;
	private static Channel tcpChannel = null;

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

	public static List<String> getListeningIps()
	{
		List<String> ips = Lists.newArrayList();
		try
		{
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while ( e.hasMoreElements() )
			{
				NetworkInterface n = e.nextElement();
				Enumeration<InetAddress> ee = n.getInetAddresses();
				while ( ee.hasMoreElements() )
				{
					InetAddress i = ee.nextElement();
					ips.add( i.getHostAddress() );
				}
			}
		}
		catch ( SocketException e1 )
		{
			getLogger().severe( "Failed to retrieve all active server ips.", e1 );
		}

		String ip = ( ( InetSocketAddress ) ( httpChannel == null ? httpsChannel : httpChannel ).localAddress() ).getAddress().getHostAddress();

		// Assert that both unsecure and secure servers are listening on the same address
		if ( httpsChannel != null && httpsChannel.isOpen() )
			assert ip.equals( ( ( InetSocketAddress ) httpsChannel.localAddress() ).getAddress().getHostAddress() );

		if ( ip.contains( "%" ) )
			ip = ip.split( "\\%" )[0];

		if ( "0.0.0.0".equals( ip ) || "0:0:0:0:0:0:0:0".equals( ip ) )
			return ips;
		else
			return Arrays.asList( ip );
	}

	public static Log getLogger()
	{
		return Log.get( SELF );
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

		NetworkSecurity.shutdown();
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
			String httpHost = AppConfig.get().getString( "server.httpHost", "" );
			int httpPort = AppConfig.get().getInt( "server.httpPort", 8080 );

			OptionSet options = Loader.options();

			if ( options.has( "httpHost" ) )
				httpHost = ( String ) options.valueOf( "httpHost" );
			if ( options.has( "httpPort" ) )
				httpPort = ( Integer ) options.valueOf( "httpPort" );

			if ( httpPort > 0 )
			{
				if ( UtilSystem.isPrivilegedPort( httpPort ) )
				{
					getLogger().warning( "It would seem that you are trying to start ChioriWebServer's Web Server on a privileged port without root access." );
					getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					getLogger().warning( "It's recommended that you either run CWS on a port like 8080 then use the firewall to redirect from 80 or run as root if you must use port: " + httpPort );
				}

				if ( httpHost.isEmpty() )
					socket = new InetSocketAddress( httpPort );
				else
					socket = new InetSocketAddress( httpHost, httpPort );

				// TODO Allow the server to bind to more than one IP

				getLogger().info( "Starting HTTP Server on " + ( httpHost.isEmpty() ? "*" : httpHost ) + ":" + httpPort );

				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new HttpInitializer() );

					httpChannel = b.bind( socket ).sync().channel();

					// HTTP Server Thread
					AppController.registerRunnable( new Runnable()
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

							getLogger().info( "The HTTP Server has been shutdown!" );
						}
					} );
				}
				catch ( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch ( Throwable e )
				{
					getLogger().warning( "**** FAILED TO BIND HTTP SERVER TO PORT!" );
					// getLogger().warning( "The exception was: {0}", new Object[] {e.toString()} );
					getLogger().warning( "Perhaps a server is already running on that port?" );

					throw new StartupException( e );
				}
			}
			else
				getLogger().warning( "The HTTP server is disabled per configs." );
		}
		catch ( Throwable e )
		{
			if ( e instanceof StartupException )
				throw e;
			else
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
			String httpHost = AppConfig.get().getString( "server.httpHost", "" );
			int httpsPort = AppConfig.get().getInt( "server.httpsPort", 8443 );

			OptionSet options = Loader.options();

			if ( options.has( "httpHost" ) )
				httpHost = ( String ) options.valueOf( "httpHost" );
			if ( options.has( "httpPort" ) )
				httpsPort = ( Integer ) options.valueOf( "httpsPort" );

			Security.addProvider( new BouncyCastleProvider() );

			if ( httpsPort >= 1 )
			{
				if ( UtilSystem.isPrivilegedPort( httpsPort ) )
				{
					getLogger().warning( "It would seem that you are trying to start ChioriWebServer's Web Server (SSL) on a privileged port without root access." );
					getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					getLogger().warning( "It's recommended that you either run CWS (SSL) on a port like 4443 then use the firewall to redirect from 443 or run as root if you must use port: " + httpsPort );
				}

				if ( httpHost.isEmpty() )
					socket = new InetSocketAddress( httpsPort );
				else
					socket = new InetSocketAddress( httpHost, httpsPort );

				AppManager.manager( SslManager.class ).init();

				getLogger().info( "Starting Secure Web Server on " + ( httpHost.isEmpty() ? "*" : httpHost ) + ":" + httpsPort );

				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new SslInitializer() );

					httpsChannel = b.bind( socket ).sync().channel();

					// HTTPS Server Thread
					AppController.registerRunnable( new Runnable()
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

							getLogger().info( "The HTTPS Server has been shutdown!" );
						}
					} );
				}
				catch ( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch ( Throwable e )
				{
					getLogger().warning( "**** FAILED TO BIND HTTPS SERVER TO PORT!" );
					getLogger().warning( "Perhaps a server is already running on that port?" );

					throw new StartupException( e );
				}
			}
			else
				getLogger().warning( "The HTTPS server is disabled per configs." );
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
			String queryHost = AppConfig.get().getString( "server.queryHost", "" );
			int queryPort = AppConfig.get().getInt( "server.queryPort", 8992 );

			OptionSet options = Loader.options();

			if ( options.has( "queryHost" ) )
				queryHost = ( String ) options.valueOf( "queryHost" );
			if ( options.has( "queryPort" ) )
				queryPort = ( Integer ) options.valueOf( "queryPort" );

			if ( queryPort >= 1 && AppConfig.get().getBoolean( "server.queryEnabled" ) )
			{
				if ( UtilSystem.isPrivilegedPort( queryPort ) )
				{
					getLogger().warning( "It would seem that you are trying to start the Query Server on a privileged port without root access." );
					getLogger().warning( "Most likely you will see an exception thrown below this. http://www.w3.org/Daemon/User/Installation/PrivilegedPorts.html" );
					getLogger().warning( "It's recommended that you either run CWS on a port like 8080 then use the firewall to redirect or run as root if you must use port: " + queryPort );
				}

				if ( queryHost.isEmpty() )
					socket = new InetSocketAddress( queryPort );
				else
					socket = new InetSocketAddress( queryHost, queryPort );

				getLogger().info( "Starting Query Server on " + ( queryHost.isEmpty() ? "*" : queryHost ) + ":" + queryPort );

				try
				{
					ServerBootstrap b = new ServerBootstrap();
					b.group( bossGroup, workerGroup ).channel( NioServerSocketChannel.class ).childHandler( new QueryServerInitializer() );

					queryChannel = b.bind( socket ).sync().channel();

					// Query Server Thread
					AppController.registerRunnable( new Runnable()
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

							getLogger().info( "The Query Server has been shutdown!" );
						}
					} );
				}
				catch ( NullPointerException e )
				{
					throw new StartupException( "There was a problem starting the Web Server. Check logs and try again.", e );
				}
				catch ( Throwable e )
				{
					getLogger().warning( "**** FAILED TO BIND QUERY SERVER TO PORT!" );
					getLogger().warning( "Perhaps a server is already running on that port?" );

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

	private NetworkManager()
	{
		TaskManager.instance().scheduleAsyncRepeatingTask( this, Ticks.SECOND_15, Ticks.SECOND_15, new Runnable()
		{
			@Override
			public void run()
			{
				for ( WeakReference<SocketChannel> ref : HttpInitializer.activeChannels )
					if ( ref.get() == null )
						HttpInitializer.activeChannels.remove( ref );
				for ( WeakReference<SocketChannel> ref : SslInitializer.activeChannels )
					if ( ref.get() == null )
						SslInitializer.activeChannels.remove( ref );
			}
		} );
	}

	@Override
	public String getLoggerId()
	{
		return "NetMgr";
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
