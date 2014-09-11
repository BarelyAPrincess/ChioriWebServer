/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin.messaging;

/**
 * Thrown if a Plugin attempts to send a message on an unregistered channel.
 */
@SuppressWarnings( "serial" )
public class ChannelNotRegisteredException extends RuntimeException
{
	public ChannelNotRegisteredException()
	{
		this( "Attempted to send a plugin message through an unregistered channel." );
	}
	
	public ChannelNotRegisteredException(String channel)
	{
		super( "Attempted to send a plugin message through an unregistered channel ('" + channel + "'." );
	}
}
