package com.chiorichan.net;

import java.io.IOException;
import java.net.InetAddress;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;

public final class TcpClient extends PacketManager
{
	protected Client client;
	
	/**
	 * Attempts to make a connection with the specified server.
	 * 
	 * @param Remote
	 *           Address
	 * @param Remote
	 *           Port
	 * @param Class
	 *           <? extends TcpConnection> - This class must contain a no parameter constructor.
	 * @throws IOException
	 */
	public TcpClient(InetAddress addr, int port, final Class<? extends TcpConnection> receiver) throws IOException
	{
		client = new Client()
		{
			protected Connection newConnection()
			{
				try
				{
					return receiver.newInstance();
				}
				catch ( InstantiationException e )
				{
					e.printStackTrace();
				}
				catch ( IllegalAccessException e )
				{
					e.printStackTrace();
				}
				
				return null;
			}
		};
		
		registerApiPackets( client.getKryo() );
		
		client.addListener( this );
		
		client.connect( 60, addr, port );
	}
	
	/**
	 * Sends the specified object to the server. Packet parameter must be registered with both the Client and Server.
	 * 
	 * @param Packet
	 * @return The number of bytes sent.
	 * @see RegisterPacket
	 */
	public int sendPacket( Packet var1 )
	{
		return client.sendTCP( var1 );
	}
	
	public void registerPacket( Class<? extends Packet> var1 )
	{
		client.getKryo().register( var1 );
	}
	
	@Override
	public void received( Connection var0, Object var2 )
	{
		TcpConnection var1 = (TcpConnection) var0;
		
		if ( var2 instanceof BasePacket )
		{
			// Handle packet for auth methods
		}
		else if ( var2 instanceof Packet )
		{
			var1.onReceived( (Packet) var2 );
		}
		else
		{
			System.err.println( "We received an incoming packet from the server but we can't process it because it was not an instance of Packet" );
		}
	}
	
	@Override
	public void connected( Connection var0 )
	{
		TcpConnection var1 = (TcpConnection) var0;
		var1.onConnect();
	}
	
	@Override
	public void disconnected( Connection var0 )
	{
		TcpConnection var1 = (TcpConnection) var0;
		var1.onDisconnect();
	}
	
	@Override
	public void idle( Connection var0 )
	{
		TcpConnection var1 = (TcpConnection) var0;
		var1.onIdle();
	}
}
