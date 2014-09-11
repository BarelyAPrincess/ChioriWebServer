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
