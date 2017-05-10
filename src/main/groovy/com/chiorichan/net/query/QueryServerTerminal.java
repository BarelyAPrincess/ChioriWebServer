/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.net.query;

import com.chiorichan.AppConfig;
import com.chiorichan.account.Kickable;
import com.chiorichan.account.lang.AccountDescriptiveReason;
import com.chiorichan.account.lang.AccountResult;
import com.chiorichan.event.network.NetworkEventFactory;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.terminal.CommandDispatch;
import com.chiorichan.terminal.QueryTerminalEntity;
import com.chiorichan.terminal.TerminalEntity;
import com.chiorichan.terminal.TerminalHandler;
import com.chiorichan.utils.UtilObjects;
import com.chiorichan.utils.UtilSystem;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

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
		terminal = new QueryTerminalEntity( this );

		// TODO Implement the Security Manager

		terminal.displayWelcomeMessage();

		if ( NetworkEventFactory.buildQueryConnected( this, ctx ) )
		{
			println( "Server Uptime: " + UtilSystem.uptime() );
			println( "The last visit from IP " + terminal.getIpAddress() + " is unknown." );
			// TODO Add more information here

			terminal.resetPrompt();
		}
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
		return disconnect( EnumColor.RED + "The server is closing your connection, goodbye!" );
	}

	public boolean disconnect( String msg )
	{
		NetworkManager.getLogger().info( EnumColor.YELLOW + "The connection to Query Client `" + getIpAddress() + "` is being disconnected with message `" + msg + "`." );
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

	public String getLocId()
	{
		return terminal.getLocId();
	}

	@Override
	public String getId()
	{
		return terminal.getId();
	}

	@Override
	public String getIpAddress()
	{
		return ( ( InetSocketAddress ) context.channel().remoteAddress() ).getAddress().getHostAddress();
	}

	@Override
	public AccountResult kick( String reason )
	{
		disconnect( reason );
		return new AccountResult( getLocId(), getId(), AccountDescriptiveReason.LOGOUT_SUCCESS );
	}

	@Override
	public void messageReceived( ChannelHandlerContext ctx, String msg ) throws IOException
	{
		if ( NetworkEventFactory.buildQueryMessageReceived( this, ctx, msg ) )
			CommandDispatch.issueCommand( terminal, msg );
	}

	private String parseColor( String text )
	{
		if ( text == null || text.isEmpty() )
			return "";

		if ( !AppConfig.get().getBoolean( "server.queryUseColor" ) || terminal != null && !UtilObjects.isTrue( terminal.getVariable( "color", "true" ) ) )
			return EnumColor.removeAltColors( text );
		else
			return EnumColor.transAltColors( text );
	}

	@Override
	public void print( String... messages )
	{
		for ( String msg : messages )
			context.write( parseColor( msg ) );
		context.flush();
	}

	@Override
	public void println( String... messages )
	{
		for ( String msg : messages )
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
