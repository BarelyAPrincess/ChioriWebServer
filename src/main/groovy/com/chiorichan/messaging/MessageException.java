/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.messaging;

import com.chiorichan.event.EventException;

/**
 * Thrown for problems encountered within the MessageDispatch class
 */
public class MessageException extends Exception
{
	private static final long serialVersionUID = 6236409081662686334L;
	
	private final MessageSender sender;
	private final Object[] objs;
	
	public MessageException( String message, MessageSender sender, Object[] objs )
	{
		this( message, sender, objs, null );
	}
	
	public MessageException( String message, MessageSender sender, Object[] objs, EventException cause )
	{
		super( message, cause );
		
		this.sender = sender;
		this.objs = objs;
	}
	
	public Object[] getMessages()
	{
		return objs;
	}
	
	public MessageSender getSender()
	{
		return sender;
	}
}
