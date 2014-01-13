package com.chiorichan.event.net;

import com.esotericsoftware.kryonet.Connection;

public class TCPDisconnectedEvent extends NetEvent
{
	private Connection connection;
	
	public TCPDisconnectedEvent( Connection _connection )
	{
		connection = _connection;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
}
