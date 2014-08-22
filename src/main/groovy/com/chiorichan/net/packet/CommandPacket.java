package com.chiorichan.net.packet;

import com.chiorichan.net.Packet;

public class CommandPacket extends Packet
{
	public String key = "";
	public Object payload = null;
	
	protected CommandPacket()
	{
		
	}
	
	public CommandPacket( String _key, Object _payload )
	{
		super();
		
		key = _key;
		payload = _payload;
	}
	
	public Object getPayload()
	{
		return payload;
	}
	
	public String getKeyword()
	{
		return key;
	}
}