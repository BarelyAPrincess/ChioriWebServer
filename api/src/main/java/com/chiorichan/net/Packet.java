package com.chiorichan.net;

import com.chiorichan.util.Common;
import com.esotericsoftware.kryonet.Connection;

public abstract class Packet
{
	public int creation = 0;
	
	public Packet()
	{
		creation = Common.getEpoch();
	}
	
	/**
	 * Override this method if you would like the packet to be notified when it reaches it's destination.
	 */
	public boolean received( Connection var1 )
	{
		return false;
	}
}