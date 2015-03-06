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

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.console.CommandDispatch;
import com.chiorichan.console.InteractiveConsole;
import com.chiorichan.console.InteractiveConsoleHandler;
import com.chiorichan.event.EventException;
import com.chiorichan.event.query.QueryEvent;
import com.chiorichan.event.query.QueryEvent.QueryType;
import com.chiorichan.session.SessionProviderQuery;

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
	private SessionProviderQuery session;
	private InteractiveConsole console;
	
	@Override
	public void channelActive( ChannelHandlerContext ctx ) throws Exception
	{
		context = ctx;
		
		session = new SessionProviderQuery( this );
		console = InteractiveConsole.createInstance( this, session );
		
		console.displayWelcomeMessage();
		
		/*
		 * if ( !session.checkPermission( "sys.query" ).isTrue() )
		 * {
		 * ChannelFuture future = ctx.write( parseColor( ConsoleColor.RED + "We're sorry, you are not permitted to login to this server via the Query Server.\r\n" ) );
		 * future.addListener( ChannelFutureListener.CLOSE );
		 * ctx.flush();
		 * return;
		 * }
		 */
		QueryEvent queryEvent = new QueryEvent( ctx, QueryType.CONNECTED, null );
		
		try
		{
			Loader.getEventBus().callEventWithException( queryEvent );
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
		
		session.handleUserProtocols();
		
		//prompt();
	}
	
	@Override
	public void channelInactive( ChannelHandlerContext ctx ) throws Exception
	{
		session.logoutAccount();
		session.onFinished();
	}
	
	private String parseColor( String text )
	{
		if ( Loader.getConfig().getBoolean( "server.queryUseColor" ) )
			return ConsoleColor.transAltColors( text );
		else
			return ConsoleColor.removeAltColors( text );
	}
	
	@Override
	public void println( String... msgs )
	{
		for ( String msg : msgs )
			println( msg );
	}
	
	@Override
	public void println( String msg )
	{
		context.write( parseColor( msg ) + "\r\n" );
		context.flush();
	}
	
	public void prompt()
	{
		print( ConsoleColor.AQUA + "(" + session.getAccount().getAcctId() + ") ?> " );
	}
	
	@Override
	public void print( String... msgs )
	{
		for ( String msg : msgs )
			print( msg );
	}
	
	@Override
	public void print( String msg )
	{
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
	public void messageReceived( ChannelHandlerContext ctx, String request )
	{
		CommandDispatch.issueCommand( session.getParentSession(), request );
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
}
