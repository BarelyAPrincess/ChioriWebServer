package com.chiorichan.net.Packet;

import com.esotericsoftware.kryonet.Connection;

public abstract class Packet
{
	public long creation = 0;
	
	public Packet()
	{
		creation = System.currentTimeMillis() / 1000;
	}
	
	public abstract void received( Connection var1 );
}
