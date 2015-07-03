/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.messaging;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.chiorichan.account.AccountType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Constructs a new message
 */
public class MessageBuilder
{
	private Set<Object> objs = Sets.newHashSet();
	private MessageSender sender = null;
	private List<MessageReceiver> receivers = Lists.newArrayList();
	private List<MessageChannel> channels = Lists.newArrayList();
	private boolean includeSender = false;
	
	private MessageBuilder()
	{
		
	}
	
	public static MessageBuilder msg( Object... objs )
	{
		return new MessageBuilder().addMsg( objs );
	}
	
	public MessageBuilder addMsg( Object... objs )
	{
		this.objs.addAll( Arrays.asList( objs ) );
		return this;
	}
	
	Collection<MessageReceiver> compileReceivers()
	{
		List<MessageReceiver> receivers = Lists.newArrayList();
		receivers.addAll( receivers );
		for ( MessageChannel channel : channels )
			receivers.addAll( MessageDispatch.channelRecipents( channel ) );
		
		if ( includeSender )
		{
			if ( sender instanceof MessageChannel )
				receivers.addAll( MessageDispatch.channelRecipents( ( MessageChannel ) sender ) );
			else if ( sender instanceof MessageReceiver )
				receivers.add( ( MessageReceiver ) sender );
		}
		else
			for ( MessageReceiver receiver : receivers )
				if ( receiver.getId().equals( sender.getId() ) )
					receivers.remove( receiver );
		
		return Collections.unmodifiableCollection( receivers );
	}
	
	public MessageBuilder excludeSender()
	{
		includeSender = false;
		return this;
	}
	
	public MessageBuilder from( MessageSender sender )
	{
		this.sender = sender;
		return this;
	}
	
	public Collection<MessageChannel> getChannels()
	{
		return channels;
	}
	
	public Collection<Object> getMessages()
	{
		return objs;
	}
	
	public Collection<MessageReceiver> getReceivers()
	{
		return receivers;
	}
	
	public MessageSender getSender()
	{
		if ( sender == null )
			sender = AccountType.ACCOUNT_ROOT;
		return sender;
	}
	
	public boolean hasSender()
	{
		return sender != null && !AccountType.isNoneAccount( sender );
	}
	
	public MessageBuilder includeSender()
	{
		includeSender = true;
		return this;
	}
	
	public MessageBuilder remove( Collection<MessageReceiver> receivers )
	{
		this.receivers.removeAll( receivers );
		return this;
	}
	
	public MessageBuilder remove( MessageChannel channel )
	{
		channels.remove( channel );
		return this;
	}
	
	public MessageBuilder remove( MessageReceiver... receivers )
	{
		this.receivers.removeAll( Arrays.asList( receivers ) );
		return this;
	}
	
	public MessageBuilder to( Collection<MessageReceiver> receivers )
	{
		this.receivers.addAll( receivers );
		return this;
	}
	
	public MessageBuilder to( MessageChannel channel )
	{
		channels.add( channel );
		return this;
	}
	
	public MessageBuilder to( MessageReceiver... receivers )
	{
		this.receivers.addAll( Arrays.asList( receivers ) );
		return this;
	}
}
