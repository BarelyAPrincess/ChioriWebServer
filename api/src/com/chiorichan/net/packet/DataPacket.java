package com.chiorichan.net.packet;

import com.chiorichan.net.BasePacket;

public class DataPacket extends BasePacket
{
	public String key = "";
	public Object payload = null;
	
	protected DataPacket()
	{
		
	}
	
	public DataPacket( String _key, Object _payload )
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