package com.chiorichan.net.Packet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class PacketManager extends Listener
{
	public static Map<String, Class<? extends Packet>> packets = new LinkedHashMap<String, Class<? extends Packet>>();
	
	public static void registerPacket( Class<? extends Packet> var1 )
	{
		Loader.getTcpServer().getKryo().register( CommandPacket.class );
		packets.put( var1.getClass().getName(), var1 );
	}
	
	@Override
	public void received( Connection var1, Object var2 )
	{
		if ( !( var2 instanceof Packet ) )
			return;
		
		for ( Entry<String, Class<? extends Packet>> var3 : packets.entrySet() )
		{
			if ( var2.getClass().equals( var3 ) )
			{
				( (Packet) var2 ).received( var1 );
				
				break;
			}
		}
	}
}
