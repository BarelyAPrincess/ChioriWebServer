/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.net;

public abstract class ConnectionReceiver
{
	/**
	 * Called when a new connection is made
	 * 
	 * @param tcpClient
	 */
	public abstract void onConnect( TcpClient parm );
	
	/**
	 * Called when no data is sent or received for over the idle timeout
	 * 
	 * @param tcpClient
	 */
	public abstract void onIdle( TcpClient parm );
	
	/**
	 * Called when the connection is terminated
	 * 
	 * @param tcpClient
	 */
	public abstract void onDisconnect( TcpClient parm );
	
	/**
	 * Called when a new packet is received from the remote client.
	 * 
	 * @return Return true if packet was successfully handled.
	 */
	public abstract boolean onReceived( TcpClient var0, Packet var1 );
}
