package com.chiorichan.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Listener;

class PacketManager extends Listener
{
	protected PacketManager(  )
	{
		
	}
	
	protected void registerApiPackets( Kryo kryo )
	{
		kryo.register( CommandPacket.class );
	}
}