package com.chiorichan.net;

import java.io.IOException;
import java.net.InetAddress;

import com.chiorichan.net.packet.PingPacket;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;

public final class TcpClient extends PacketManager
{
	protected Client client;
	protected ConnectionReceiver receiver;
	protected String lastPingId;
	
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
	public TcpClient(InetAddress addr, int port, ConnectionReceiver _receiver) throws IOException
	{
		this( _receiver );
		attemptConnection( addr, port );
	}
	
	public TcpClient(ConnectionReceiver _receiver) throws IOException
	{
		receiver = _receiver;
		
		client = new Client();
		client.getKryo().setAsmEnabled( true );
		client.start();
		
		registerApiPackets( client.getKryo() );
	}
	
	public void attemptConnection( InetAddress addr, int port ) throws IOException
	{
		client.addListener( this );
		client.connect( 5000, addr, port );
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
	
	public void sendPing()
	{
		//lastPingId = Common.md5( System.currentTimeMillis() + client.getRemoteAddressTCP().getAddress().toString() );
		//sendPacket( new PingPacket( lastPingId ) );
		
		client.updateReturnTripTime();
	}
	
	@Override
	public void received( Connection var0, Object var1 )
	{
		if ( var1 instanceof BasePacket )
		{
			super.handleBasePacket( var0, (BasePacket) var1 );
		}
		else if ( var1 instanceof Packet )
		{
			if ( var1 instanceof PingPacket )
			{
				PingPacket ping = (PingPacket) var1;
				
				if ( ping.isReply )
				{
					if ( lastPingId.equals( ping.id ) )
					{
						long localDelay = System.currentTimeMillis() - ping.created;
						long outDelay = ping.received - ping.created;
						long inDelay = ping.received - System.currentTimeMillis();
						
						System.out.println( "Network Latency Report: Round Trip " + localDelay + ", Outbound Trip " + outDelay + ", Inbound Trip " + inDelay );
					}
				}
				else
				{
					ping.isReply = true;
					ping.received = System.currentTimeMillis();
					sendPacket( ping );
				}
			}
			
			( (Packet) var1 ).received( var0 );
			if ( !receiver.onReceived( this, (Packet) var1 ) )
				System.err.println( "There was a problem processing the received packet. Try checking the logs." );
		}
		else if ( var1 instanceof KeepAlive )
		{	
			
		}
		else
		{
			System.err.println( "We received an incoming packet from the remote client but we can't process it because it was not an instance of Packet" );
		}
	}
	
	@Override
	public void connected( Connection var0 )
	{
		receiver.onConnect( this );
	}
	
	@Override
	public void disconnected( Connection var0 )
	{
		receiver.onDisconnect( this );
	}
	
	@Override
	public void idle( Connection var0 )
	{
		receiver.onIdle( this );
	}
	
	public boolean isConnected()
	{
		return client.isConnected();
	}
	
	public void disconnect()
	{
		client.close();
	}
}
