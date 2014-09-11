/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.bus.events.net;

import com.chiorichan.bus.bases.Cancellable;
import com.chiorichan.net.Packet;
import com.esotericsoftware.kryonet.Connection;

public class TCPIncomingEvent extends NetEvent implements Cancellable
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
	
	public TCPIncomingEvent( Connection _connection, Packet packet, boolean handle )
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
