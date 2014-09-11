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
