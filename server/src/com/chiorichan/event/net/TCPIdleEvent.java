package com.chiorichan.event.net;

import com.esotericsoftware.kryonet.Connection;

public class TCPIdleEvent extends NetEvent
{
	private Connection connection;
	
	public TCPIdleEvent(Connection _connection)
	{
		connection = _connection;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
}
