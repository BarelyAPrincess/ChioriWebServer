package com.chiorichan.net.packet;

import com.chiorichan.net.Packet;

public class PingPacket extends Packet
{
	protected PingPacket()
	{
		
	}
	
	public PingPacket(String pingId)
	{
		id = pingId;
		created = System.currentTimeMillis();
	}
	
	public String id;
	public long created;
	public long received = -1;
	public boolean isReply;
}
