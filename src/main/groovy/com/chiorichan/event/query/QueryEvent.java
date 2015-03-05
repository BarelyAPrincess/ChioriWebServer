/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.event.query;

import io.netty.channel.ChannelHandlerContext;

import com.chiorichan.event.Cancellable;
import com.chiorichan.event.server.ServerEvent;

public class QueryEvent extends ServerEvent implements Cancellable
{
	public enum QueryType
	{
		CONNECTED, RECEIVED_MESSAGE;
	}
	
	private String reason;
	private String message;
	private boolean cancelled = false;
	private ChannelHandlerContext context;
	private QueryType type;
	
	public QueryEvent( ChannelHandlerContext context, QueryType type, String message )
	{
		this.context = context;
		this.type = type;
		this.message = message;
	}
	
	public void setReason( String reason )
	{
		this.reason = reason;
	}
	
	public String getReason()
	{
		return reason;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public ChannelHandlerContext getContext()
	{
		return context;
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
}
