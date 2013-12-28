package com.chiorichan.net;

import com.chiorichan.util.Common;

public abstract class Packet
{
	public int creation = 0;
	
	public Packet()
	{
		creation = Common.getEpoch();
	}
}