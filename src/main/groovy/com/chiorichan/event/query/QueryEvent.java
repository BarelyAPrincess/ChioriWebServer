/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
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
