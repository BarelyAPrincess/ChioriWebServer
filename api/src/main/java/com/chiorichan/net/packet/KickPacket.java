package com.chiorichan.net.packet;

import com.chiorichan.net.BasePacket;

public class KickPacket extends BasePacket
{
	public String reason = "";
	
	protected KickPacket()
	{
		
	}
	
	public KickPacket( String _reason )
	{
		super();
		
		reason = _reason;
	}
	
	public String getReason()
	{
		return reason;
	}
}