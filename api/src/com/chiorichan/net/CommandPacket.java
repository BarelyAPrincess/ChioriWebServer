package com.chiorichan.net;

public class CommandPacket extends Packet
{
	public String key = "";
	public Object payload = null;
	
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