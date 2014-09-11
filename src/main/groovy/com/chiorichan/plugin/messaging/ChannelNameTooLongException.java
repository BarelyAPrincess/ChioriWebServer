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
 * Thrown if a Plugin Channel is too long.
 */
@SuppressWarnings( "serial" )
public class ChannelNameTooLongException extends RuntimeException
{
	public ChannelNameTooLongException()
	{
		super( "Attempted to send a Plugin Message to a channel that was too large. The maximum length a channel may be is " + Messenger.MAX_CHANNEL_SIZE + " chars." );
	}
	
	public ChannelNameTooLongException(String channel)
	{
		super( "Attempted to send a Plugin Message to a channel that was too large. The maximum length a channel may be is " + Messenger.MAX_CHANNEL_SIZE + " chars (attempted " + channel.length() + " - '" + channel + "." );
	}
}
