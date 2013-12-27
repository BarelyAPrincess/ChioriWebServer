package com.chiorichan.net.Packet;

import com.chiorichan.Loader;
import com.esotericsoftware.kryonet.Connection;

public abstract class Packet
{
	public int creation = 0;
	
	public Packet()
	{
		creation = Loader.getEpoch();
	}
	
	public abstract void received( Connection var1 );
}
