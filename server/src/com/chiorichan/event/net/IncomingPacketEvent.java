package com.chiorichan.event.net;

import com.chiorichan.event.Cancellable;
import com.chiorichan.net.Packet;
import com.esotericsoftware.kryonet.Connection;

public class IncomingPacketEvent extends NetEvent implements Cancellable
{
	private boolean handled = false;
	private boolean cancelled = false;
	private Packet attachedPacket;
	private Connection connection;
	
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
	
	public boolean isHandled()
	{
		return handled;
	}
	
	public void setHandled( boolean handle )
	{
		handled = handle;
	}
	
	public IncomingPacketEvent( Connection _connection, Packet packet, boolean handle )
	{
		connection = _connection;
		attachedPacket = packet;
		handled = handle;
	}
	
	public Packet getPacket()
	{
		return attachedPacket;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
}
