/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.chiorichan.account.Account;
import com.chiorichan.account.AccountManager;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.MessageEvent;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Handles the delivery and receiving of chat messages
 */
public class MessageDispatch
{
	private static final Map<MessageChannel, List<MessageReceiver>> channels = Maps.newConcurrentMap();
	
	public static Collection<MessageReceiver> channelRecipents( MessageChannel channel )
	{
		return Collections.unmodifiableList( channels.get( channel ) );
	}
	
	public static void channelRegister( MessageChannel channel, MessageReceiver receiver )
	{
		if ( channels.containsKey( channel ) )
			channels.get( channel ).add( receiver );
		else
			channels.put( channel, new ArrayList<MessageReceiver>( Arrays.asList( receiver ) ) );
	}
	
	public static void channelUnregister( MessageChannel channel, MessageReceiver... receivers )
	{
		if ( channels.containsKey( channel ) )
			channels.get( channel ).removeAll( Arrays.asList( receivers ) );
	}
	
	public static void sendMessage( MessageBuilder builder ) throws MessageException
	{
		MessageEvent event = new MessageEvent( builder.getSender(), builder.compileReceivers(), builder.getMessages() );
		try
		{
			EventBus.INSTANCE.callEventWithException( event );
		}
		catch ( EventException e )
		{
			throw new MessageException( "Encountered an exception while tring to deliver a message", builder.getSender(), builder.getMessages().toArray( new MessageReceiver[0] ), e );
		}
		if ( !event.isCancelled() && !event.getRecipients().isEmpty() && !event.getMessages().isEmpty() )
			for ( MessageReceiver dest : event.getRecipients() )
				dest.sendMessage( event.getSender(), event.getMessages() );
	}
	
	/**
	 * Attempts to send specified object to every initialized Account
	 * 
	 * @param excludedAcct
	 *            Ignore this account when sending
	 * @param objs
	 *            The objects to send
	 */
	public static void sendMessage( Object... objs ) throws MessageException
	{
		Set<MessageReceiver> recipients = Sets.newHashSet();
		for ( Account acct : AccountManager.INSTANCE.getAccounts() )
			if ( acct.meta().isInitialized() )
				for ( MessageReceiver receiver : acct.instance().getAttachments() )
					recipients.add( receiver );
		sendMessage( MessageBuilder.msg( objs ).to( recipients ) );
	}
}
