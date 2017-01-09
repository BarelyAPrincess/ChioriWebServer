/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.event.query;

import io.netty.channel.ChannelHandlerContext;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.application.ApplicationEvent;
import com.chiorichan.net.query.QueryServerTerminal;

public class QueryEvent extends ApplicationEvent implements Cancellable
{
	public enum QueryType
	{
		CONNECTED, RECEIVED_MESSAGE;
	}
	
	private String reason;
	private String message;
	private boolean cancelled = false;
	private ChannelHandlerContext context;
	private QueryServerTerminal terminal;
	private QueryType type;
	
	public QueryEvent( QueryServerTerminal terminal, ChannelHandlerContext context, QueryType type, String message )
	{
		this.context = context;
		this.type = type;
		this.message = message;
		this.terminal = terminal;
	}
	
	public ChannelHandlerContext getContext()
	{
		return context;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public QueryServerTerminal getTerminal()
	{
		return terminal;
	}
	
	public QueryType getType()
	{
		return type;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancel )
	{
		cancelled = cancel;
	}
	
	public void setReason( String reason )
	{
		this.reason = reason;
	}
}
