package com.chiorichan.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.chiorichan.Loader;
import com.chiorichan.StartupException;
import com.chiorichan.http.WebHandler;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.esotericsoftware.kryonet.Server;
import com.sun.net.httpserver.HttpServer;

public class NetworkManager
{
	private static HttpServer httpServer;
	private static EndPoint tcpConnection;
	
	private static Executor executor = Executors.newCachedThreadPool();
	
	private static String remoteTcpIp = null;
	private static Integer remoteTcpPort = -1;
	private static boolean isClientMode = false;
	
	public static void initTcpClient() throws StartupException
	{
		try
		{
			remoteTcpIp = Loader.getConfig().getString( "client.remoteTcpHost", null );
			remoteTcpPort = Loader.getConfig().getInt( "client.remoteTcpHost", 1024 );
			
			if ( Loader.getOptions().has( "client-ip" ) )
				remoteTcpIp = (String) Loader.getOptions().valueOf( "client-ip" );
			
			if ( Loader.getOptions().has( "client-port" ) )
				remoteTcpPort = (Integer) Loader.getOptions().valueOf( "client-port" );
			
			if ( remoteTcpIp == null || remoteTcpIp.isEmpty() || remoteTcpPort < 1 )
			{
				throw new StartupException( "The remote Host/IP and/or Port are missconfigured, Please define them in the local config file or use --client-ip and/or --client-port arguments." );
			}
			
			Client tcpClient = new Client();
			
			Loader.getLogger().info( "Connecting to Chiori Web Server (over TCPIP) at " + remoteTcpIp + ":" + remoteTcpPort + "..." );
			
			tcpClient.start();
			tcpClient.connect( 10000, remoteTcpIp, remoteTcpPort );
			
			tcpClient.addListener( new ThreadedListener( new PacketListener( tcpClient.getKryo() ), Executors.newFixedThreadPool( 3 ) ) );
			
			tcpConnection = tcpClient;
		}
		catch ( IOException e )
		{
			Loader.getLogger().warning( "**** FAILED TO CONNECT TO TCP SERVER!" );
			Loader.getLogger().warning( "The exception was: {0}", new Object[] { e.toString() } );
			Loader.getLogger().warning( "Is the server running and/or the port open in the firewall?" );
			throw new StartupException( e );
		}
	}
	
	public static void initTcpServer() throws StartupException
	{
		try
		{
			InetSocketAddress socket;
			String serverIp = Loader.getConfig().getString( "server.tcpHost", "" );
			int serverPort = Loader.getConfig().getInt( "server.tcpPort", 80 );
			
			// If there was no tcp host specified then attempt to use the same one as the http server.
			if ( serverIp.isEmpty() )
				serverIp = Loader.getConfig().getString( "server.httpHost", "" );
			
			if ( serverIp.isEmpty() )
				socket = new InetSocketAddress( serverPort );
			else
				socket = new InetSocketAddress( serverIp, serverPort );
			
			Server tcpServer = new Server()
			{
				protected Connection newConnection()
				{
					return new ServerConnection();
				}
			};
			
			Loader.getLogger().info( "Starting Tcp Server on " + ( serverIp.length() == 0 ? "*" : serverIp ) + ":" + serverPort );
			
			tcpServer.start();
			tcpServer.bind( socket, null );
			
			tcpServer.addListener( new ThreadedListener( new PacketListener( tcpServer.getKryo() ), Executors.newFixedThreadPool( 3 ) ) );
			
			tcpConnection = tcpServer;
		}
		catch ( IOException e )
		{
			Loader.getLogger().warning( "**** FAILED TO BIND TCP SERVER TO PORT!" );
			Loader.getLogger().warning( "The exception was: {0}", new Object[] { e.toString() } );
			Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
			throw new StartupException( e );
		}
	}
	
	public static boolean registerPacket( Class<? extends Packet> packet )
	{
		if ( tcpConnection != null )
		{
			tcpConnection.getKryo().register( packet );
			return true;
		}
		else
			return false;
	}
	
	public static void initWebServer() throws StartupException
	{
		try
		{
			InetSocketAddress socket;
			String serverIp = Loader.getConfig().getString( "server.httpHost", "" );
			int serverPort = Loader.getConfig().getInt( "server.httpPort", 80 );
			
			if ( serverIp.isEmpty() )
				socket = new InetSocketAddress( serverPort );
			else
				socket = new InetSocketAddress( serverIp, serverPort );
			
			httpServer = HttpServer.create( socket, 0 );
			
			httpServer.setExecutor( executor );
			httpServer.createContext( "/", new WebHandler() );
			
			// TODO: Add SSL support ONEDAY!
			
			Loader.getLogger().info( "Starting Web Server on " + ( serverIp.length() == 0 ? "*" : serverIp ) + ":" + serverPort );
			
			try
			{
				httpServer.start();
			}
			catch ( NullPointerException e )
			{
				Loader.getLogger().severe( "There was a problem starting the Web Server. Check logs and try again.", e );
				System.exit( 1 );
			}
			catch ( Throwable e )
			{
				Loader.getLogger().warning( "**** FAILED TO BIND WEB SERVER TO PORT!" );
				Loader.getLogger().warning( "The exception was: {0}", new Object[] { e.toString() } );
				Loader.getLogger().warning( "Perhaps a server is already running on that port?" );
			}
		}
		catch ( Throwable e )
		{
			throw new StartupException( e );
		}
	}
	
	public static boolean isClientMode()
	{
		return isClientMode;
	}
	
	public static void cleanup()
	{
		if ( httpServer != null )
			httpServer.stop( 0 );
		
		if ( tcpConnection != null )
			tcpConnection.stop();
	}
	
	public static void setClientMode( boolean clientMode )
	{
		isClientMode = clientMode;
	}
	
	public static EndPoint getTcpConnection()
	{
		return tcpConnection;
	}
	
	public static Server getTcpServer()
	{
		if ( isClientMode )
			return null;
		
		return (Server) tcpConnection;
	}
	
	public static Client getTcpClient()
	{
		if ( !isClientMode )
			return null;
		
		return (Client) tcpConnection;
	}
	
	public static HttpServer getWebServer()
	{
		return httpServer;
	}
	
	/**
	 * Sends the packet over the network using TCP.
	 * If in ClientMode will send to Server, otherwise will send packet to ALL CLIENTS!
	 * 
	 * @param packet
	 */
	public static void sendTCP( Packet packet )
	{
		if ( isClientMode )
			( (Client) tcpConnection ).sendTCP( packet );
		else
			( (Server) tcpConnection ).sendToAllTCP( packet );
	}
}
