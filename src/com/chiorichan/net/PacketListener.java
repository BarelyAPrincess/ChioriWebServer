package com.chiorichan.net;

import com.chiorichan.Loader;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;

public class PacketListener extends PacketManager
{
	public PacketListener( Kryo kryo )
	{
		super.registerApiPackets( kryo );
	}
	
	@Override
	public void received( Connection var1, Object var2 )
	{
		if ( var2 instanceof BasePacket )
		{	
			
		}
		else if ( var2 instanceof Packet )
		{
			
		}
		else
		{
			Loader.getLogger().severe( "We received an incoming packet from the server but we can't process it because it was not an instance of Packet" );
		}
	}
	
	@Override
	public void connected( Connection var0 )
	{
		
	}
	
	@Override
	public void disconnected( Connection var0 )
	{
		
	}
	
	@Override
	public void idle( Connection var0 )
	{
		
	}
}