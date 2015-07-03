/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.chiorichan.event.Cancellable;
import com.chiorichan.messaging.MessageReceiver;
import com.chiorichan.messaging.MessageSender;
import com.google.common.collect.Lists;

/**
 * Fired when a system message will be delivered
 */
public class MessageEvent extends ServerEvent implements Cancellable
{
	private final MessageSender sender;
	private Collection<MessageReceiver> recipients;
	private Collection<Object> objs;
	private boolean cancelled = false;
	
	public MessageEvent( final MessageSender sender, final Collection<MessageReceiver> recipients, final Object... objs )
	{
		this.sender = sender;
		this.recipients = recipients;
		this.objs = Arrays.asList( objs );
	}
	
	public void addMessage( Object obj )
	{
		objs.add( obj );
	}
	
	public void addRecipient( MessageReceiver acct )
	{
		recipients.add( acct );
	}
	
	public boolean containsRecipient( MessageReceiver acct )
	{
		for ( MessageReceiver acct1 : recipients )
			if ( acct1.getId().equals( acct.getId() ) )
				return true;
		return false;
	}
	
	public Collection<Object> getMessages()
	{
		return objs;
	}
	
	@SuppressWarnings( "unchecked" )
	public <T> T[] getObjectMessages( Class<?> clz )
	{
		List<T> o = Lists.newArrayList();
		for ( Object obj : objs )
			if ( obj.getClass() == clz )
				o.add( ( T ) obj );
		return ( T[] ) o.toArray();
	}
	
	public Collection<MessageReceiver> getRecipients()
	{
		return recipients;
	}
	
	public MessageSender getSender()
	{
		return sender;
	}
	
	public String[] getStringMessages()
	{
		return ( String[] ) getObjectMessages( String.class );
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	public Object removeMessage( int index )
	{
		return objs.remove( index );
	}
	
	public boolean removeMessage( Object obj )
	{
		return objs.remove( obj );
	}
	
	public boolean removeRecipient( MessageReceiver acct )
	{
		for ( MessageReceiver acct1 : recipients )
			if ( acct1.getId().equals( acct.getId() ) )
				return recipients.remove( acct1 );
		return false;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		cancelled = cancel;
	}
	
	/**
	 * WARNING! This will completely clear and reset the messages.
	 * 
	 * @param objs
	 *            The new messages
	 */
	public void setMessages( Iterable<Object> objs )
	{
		this.objs = Lists.newArrayList( objs );
	}
	
	/**
	 * WARNING! This will completely clear and reset the messages.
	 * 
	 * @param objs
	 *            The new messages
	 */
	public void setMessages( Object... objs )
	{
		this.objs = Arrays.asList( objs );
	}
	
	public void setRecipients( Set<MessageReceiver> recipients )
	{
		this.recipients = recipients;
	}
}
