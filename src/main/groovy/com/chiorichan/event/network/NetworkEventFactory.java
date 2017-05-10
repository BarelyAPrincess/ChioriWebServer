/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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
