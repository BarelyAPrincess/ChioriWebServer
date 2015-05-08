/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
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
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.InteractiveConsoleHandler;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventException;
import com.chiorichan.event.query.QueryEvent;
import com.chiorichan.event.query.QueryEvent.QueryType;
import com.chiorichan.net.NetworkWrapper;
import com.chiorichan.util.StringFunc;

/**
 * Handles the Query Server traffic
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
@Sharable
public class QueryServerHandler extends SimpleChannelInboundHandler<String> implements InteractiveConsoleHandler
{
	private ChannelHandlerContext context;
	private NetworkWrapper persistence;
	private InteractiveConsole console;
	
	@Override
	public void channelActive( ChannelHandlerContext ctx ) throws Exception
	{
		context = ctx;
		
		persistence = new NetworkWrapper( this );
		console = InteractiveConsole.createInstance( this, persistence );
		
		console.displayWelcomeMessage();
		
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
			ChannelFuture future = ctx.write( parseColor( ( queryEvent.getReason().isEmpty() ) ? "We're sorry, you've been disconnected from the server by a Cancelled Event." : queryEvent.getReason() ) );
			future.addListener( ChannelFutureListener.CLOSE );
			ctx.flush();
			return;
		}
		
		console.resetPrompt();
	}
	
	@Override
	public void channelInactive( ChannelHandlerContext ctx ) throws Exception
	{
		if ( persistence != null )
			persistence.finish();
	}
	
	private String parseColor( String text )
	{
		if ( !Loader.getConfig().getBoolean( "server.queryUseColor" ) || !StringFunc.isTrue( console.getMetadata( "color", "true" ) ) )
			return ConsoleColor.removeAltColors( text );
		else
			return ConsoleColor.transAltColors( text );
	}
	
	@Override
	public void println( String... msgs )
	{
		for ( String msg : msgs )
			context.write( parseColor( msg ) + "\r\n" );
		context.flush();
		console.prompt();
	}
	
	@Override
	public void print( String... msgs )
	{
		for ( String msg : msgs )
			context.write( parseColor( msg ) );
		context.flush();
	}
	
	public void disconnect()
	{
		disconnect( ConsoleColor.RED + "The server is disconnecting you connection, good bye!" );
	}
	
	public void disconnect( String msg )
	{
		ChannelFuture future = context.write( parseColor( msg ) );
		future.addListener( ChannelFutureListener.CLOSE );
	}
	
	@Override
	public void messageReceived( ChannelHandlerContext ctx, String msg )
	{
		CommandDispatch.issueCommand( console, msg );
	}
	
	@Override
	public void channelReadComplete( ChannelHandlerContext ctx )
	{
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
	{
		cause.printStackTrace();
		ctx.close();
	}
	
	@Override
	public NetworkWrapper getPersistence()
	{
		return persistence;
	}
	
	public String getIpAddr()
	{
		return ( ( InetSocketAddress ) context.channel().remoteAddress() ).getAddress().getHostAddress();
	}
}
