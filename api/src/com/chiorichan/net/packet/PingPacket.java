package com.chiorichan.net.packet;

import com.chiorichan.net.Packet;
import com.esotericsoftware.kryonet.Connection;

public class PingPacket extends Packet
{
	public static String lastPingId = "";
	public String id;
	public long created;
	public long received = -1;
	public boolean isReply;
	
	protected PingPacket()
	{
		
	}
	
	public PingPacket(String pingId)
	{
		id = pingId;
		lastPingId = id;
		created = System.currentTimeMillis();
	}
	
	@Override
	public boolean received( Connection var1 )
	{
		// TODO I'd really like to make this packet awesome with some super awesome statistic data like Latency, Jitter, Time Offset, etc.
		
		if ( isReply )
		{
			if ( id.equals( lastPingId ) )
			{
				long curentTime = System.currentTimeMillis();
				
				long localDelay = curentTime - created;
				long outDelay = created - received;
				long inDelay = curentTime - received;
				
				System.out.println( "Network Latency Report: Round Trip " + localDelay + "ms, Outbound Trip " + outDelay + "ms, Inbound Trip " + inDelay + "ms." );
				
				//System.out.println( "Remote Connection is about " + syncDelay1 + "ms/" + syncDelay2 + "ms out of sync with us." );
			}
		}
		else
		{
			isReply = true;
			received = System.currentTimeMillis();
			var1.sendTCP( this );
		}
		
		return true;
	}
}
