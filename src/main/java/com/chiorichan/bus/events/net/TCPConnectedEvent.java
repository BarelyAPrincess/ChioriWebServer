package com.chiorichan.bus.events.net;

import com.chiorichan.bus.bases.Cancellable;
import com.esotericsoftware.kryonet.Connection;

public class TCPConnectedEvent extends NetEvent implements Cancellable
{
	private boolean cancelled = false;
	private Connection connection;
	
	public TCPConnectedEvent( Connection _connection )
	{
		connection = _connection;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		cancelled = cancel;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
}
