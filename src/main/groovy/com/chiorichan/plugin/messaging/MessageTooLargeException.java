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
 * Thrown if a Plugin Message is sent that is too large to be sent.
 */
@SuppressWarnings( "serial" )
public class MessageTooLargeException extends RuntimeException
{
	public MessageTooLargeException()
	{
		this( "Attempted to send a plugin message that was too large. The maximum length a plugin message may be is " + Messenger.MAX_MESSAGE_SIZE + " bytes." );
	}
	
	public MessageTooLargeException(byte[] message)
	{
		this( message.length );
	}
	
	public MessageTooLargeException(int length)
	{
		this( "Attempted to send a plugin message that was too large. The maximum length a plugin message may be is " + Messenger.MAX_MESSAGE_SIZE + " bytes (tried to send one that is " + length + " bytes long)." );
	}
	
	public MessageTooLargeException(String msg)
	{
		super( msg );
	}
}
