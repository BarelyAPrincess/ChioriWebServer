/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.net.query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.account.Kickable;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.query.QueryEvent;
import com.chiorichan.event.query.QueryEvent.QueryType;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.terminal.CommandDispatch;
import com.chiorichan.terminal.TerminalEntity;
import com.chiorichan.terminal.TerminalHandler;
import com.chiorichan.util.StringFunc;

/**
 * Handles the Query Server traffic
 */
@Sharable
public class QueryServerTerminal extends SimpleChannelInboundHandler<String> implements TerminalHandler, Kickable
{
	private ChannelHandlerContext context;
	private TerminalEntity terminal;
	
	@Override
	public void channelActive( ChannelHandlerContext ctx ) throws Exception
	{
		context = ctx;
		terminal = new TerminalEntity( this );
		
		// TODO Implement the Security Manager
		
		terminal.displayWelcomeMessage();
		
		QueryEvent queryEvent = new QueryEvent( ctx, QueryType.CONNECTED, null );
		
		try
		{
			EventBus.INSTANCE.callEventWithException( queryEvent );
		}
		catch ( EventException ex )
		{
			throw new IOException( "Exception encountered during query event call, most likely the fault of a plugin.", ex );
		}
		
		if ( queryEvent.isCancelled() )
		{
			ChannelFuture future = ctx.writeAndFlush( parseColor( ( queryEvent.getReason().isEmpty() ) ? "We're sorry, you've been disconnected from the server by a Cancelled Event." : queryEvent.getReason() ) );
			future.addListener( ChannelFutureListener.CLOSE );
			return;
		}
		
		println( "Server Uptine: " + Loader.getUptime() );
		println( "The last visit from IP " + terminal.getIpAddr() + " is unknown." );
		// TODO Add more information here
		
		terminal.resetPrompt();
	}
	
	@Override
	public void channelInactive( ChannelHandlerContext ctx ) throws Exception
	{
		if ( terminal != null )
			terminal.finish();
	}
	
	@Override
	public void channelReadComplete( ChannelHandlerContext ctx )
	{
		ctx.flush();
	}
	
	@Override
	public boolean disconnect()
	{
		return disconnect( ConsoleColor.RED + "The server is closing your connection, goodbye!" );
	}
	
	public boolean disconnect( String msg )
	{
		NetworkManager.getLogger().info( ConsoleColor.YELLOW + "The connection to Query Client `" + getIpAddr() + "` is being disconnected with message `" + msg + "`." );
		ChannelFuture future = context.writeAndFlush( "\r" + parseColor( msg ) + "\r\n" );
		future.addListener( ChannelFutureListener.CLOSE );
		return true;
	}
	
	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
	{
		cause.printStackTrace();
		ctx.close();
	}
	
	@Override
	public String getId()
	{
		return terminal.getId();
	}
	
	@Override
	public String getIpAddr()
	{
		return ( ( InetSocketAddress ) context.channel().remoteAddress() ).getAddress().getHostAddress();
	}
	
	@Override
	public AccountResult kick( String reason )
	{
		disconnect( reason );
		return AccountResult.SUCCESS;
	}
	
	@Override
	public void messageReceived( ChannelHandlerContext ctx, String msg )
	{
		CommandDispatch.issueCommand( terminal, msg );
	}
	
	private String parseColor( String text )
	{
		if ( text == null || text.isEmpty() )
			return "";
		
		if ( !Loader.getConfig().getBoolean( "server.queryUseColor" ) || ( terminal != null && !StringFunc.isTrue( terminal.getVariable( "color", "true" ) ) ) )
			return ConsoleColor.removeAltColors( text );
		else
			return ConsoleColor.transAltColors( text );
	}
	
	@Override
	public void print( String... msgs )
	{
		for ( String msg : msgs )
			context.write( parseColor( msg ) );
		context.flush();
	}
	
	@Override
	public void println( String... msgs )
	{
		for ( String msg : msgs )
			context.write( "\r" + parseColor( msg ) + "                   " + "\r\n" );
		context.flush();
		if ( terminal != null )
			terminal.prompt();
	}
	
	@Override
	public TerminalType type()
	{
		return TerminalType.TELNET;
	}
}
