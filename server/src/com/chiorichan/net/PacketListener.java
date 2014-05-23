package com.chiorichan.net;

import com.chiorichan.ChatColor;
import com.chiorichan.Loader;
import com.chiorichan.event.net.TCPConnectedEvent;
import com.chiorichan.event.net.TCPDisconnectedEvent;
import com.chiorichan.event.net.TCPIdleEvent;
import com.chiorichan.event.net.TCPIncomingEvent;
import com.chiorichan.net.packet.CommandPacket;
import com.chiorichan.net.packet.DataPacket;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.FrameworkMessage.Ping;

public class PacketListener extends PacketManager
{
	public PacketListener(Kryo kryo)
	{
		super.registerApiPackets( kryo );
	}
	
	@Override
	public void received( Connection var1, Object var2 )
	{
		if ( var2 instanceof BasePacket )
		{
			super.handleBasePacket( var1, (BasePacket) var2 );
		}
		else if ( var2 instanceof Packet )
		{
			boolean handled = ( (Packet) var2 ).received( var1 );
			
			if ( handled == false )
			{
				handled = true;
				if ( var2 instanceof CommandPacket )
				{
					CommandPacket var3 = ( (CommandPacket) var2 );
					
					switch ( var3.getKeyword().toUpperCase() )
					{
						case "PING":
							Loader.getLogger().info( ChatColor.NEGATIVE + "&2 Received a ping from the client: " + var3.getPayload() + "ms " );
							var1.sendTCP( new CommandPacket( "PONG", System.currentTimeMillis() ) );
					}
				}
				else
				{
					handled = false;
				}
			}
			
			if ( !handled )
				Loader.getLogger().info( "&5Got packet '" + var2 + "' from connection at '" + var1.getRemoteAddressTCP().getAddress().getHostAddress() + "'" );
			
			TCPIncomingEvent event = new TCPIncomingEvent( var1, (Packet) var2, handled );
			Loader.getEventBus().callEvent( event );
		}
		else if ( var2 instanceof KeepAlive )
		{
			// KryoNet KeepAlive Packet
		}
		else if ( var2 instanceof Ping )
		{
			// KryoNet Ping Packet
		}
		else
		{
			Loader.getLogger().severe( "We received an incoming packet from the server but we can't process it because it was not an instance of Packet. (" + var2.getClass() + ")" );
		}
	}
	
	@Override
	public void connected( Connection var1 )
	{
		TCPConnectedEvent event = new TCPConnectedEvent( var1 );
		Loader.getEventBus().callEvent( event );
		if ( event.isCancelled() )
		{
			Loader.getLogger().info( ChatColor.YELLOW + "Connection from " + var1.getRemoteAddressTCP().getAddress().getHostAddress() + " was disconnected because it was concelled by the TCPConnectedEvent!" );
			var1.close();
		}
		else if ( NetworkManager.isClientMode() )
		{
			var1.sendTCP( new DataPacket( "beginSession", null ) );
		}
	}
	
	@Override
	public void disconnected( Connection var1 )
	{
		Loader.getEventBus().callEvent( new TCPDisconnectedEvent( var1 ) );
	}
	
	@Override
	public void idle( Connection var1 )
	{
		Loader.getEventBus().callEvent( new TCPIdleEvent( var1 ) );
	}
}
