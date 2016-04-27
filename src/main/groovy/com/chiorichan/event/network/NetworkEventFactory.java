/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.network;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.query.QueryEvent;
import com.chiorichan.event.query.QueryEvent.QueryType;
import com.chiorichan.net.query.QueryServerTerminal;

/**
 * Produces common server events
 */
public class NetworkEventFactory
{
	public static boolean buildQueryConnected( QueryServerTerminal terminal, ChannelHandlerContext ctx ) throws IOException
	{
		try
		{
			QueryEvent queryEvent = new QueryEvent( terminal, ctx, QueryType.CONNECTED, null );
			if ( EventBus.instance().callEventWithException( queryEvent ).isCancelled() )
			{
				terminal.disconnect( queryEvent.getReason().isEmpty() ? "We're sorry, you've been disconnected from the server by a Cancelled Event." : queryEvent.getReason() );
				return false;
			}
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during query event call, most likely the fault of a plugin.", ex );
		}

		return true;
	}

	public static boolean buildQueryMessageReceived( QueryServerTerminal terminal, ChannelHandlerContext ctx, String msg ) throws IOException
	{
		try
		{
			return !EventBus.instance().callEventWithException( new QueryEvent( terminal, ctx, QueryType.RECEIVED_MESSAGE, msg ) ).isCancelled();
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during query event call, most likely the fault of a plugin.", ex );
		}
	}
}
