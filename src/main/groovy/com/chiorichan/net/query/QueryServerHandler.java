/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.net.query;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Date;

import com.chiorichan.util.Versioning;

/**
 * Handles the Query Server traffic
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class QueryServerHandler extends SimpleChannelInboundHandler<String>
{
	@Override
	public void channelActive( ChannelHandlerContext ctx ) throws Exception
	{
		// Send greeting for a new connection.
		ctx.write( "Welcome to " + Versioning.getProduct() + " " + Versioning.getVersion() + "!\r\n" );
		ctx.write( "It is " + new Date() + " now.\r\n" );
		ctx.flush();
	}
	
	@Override
	public void messageReceived( ChannelHandlerContext ctx, String request )
	{
		QuerySession
		
		String response;
		boolean close = false;
		if ( request.isEmpty() )
		{
			response = "Please type something.\r\n";
		}
		else if ( "bye".equals( request.toLowerCase() ) )
		{
			response = "Have a good day!\r\n";
			close = true;
		}
		else
		{
			response = "Did you say '" + request + "'?\r\n";
		}
		
		// We do not need to write a ChannelBuffer here.
		// We know the encoder inserted at TelnetPipelineFactory will do the conversion.
		ChannelFuture future = ctx.write( response );
		
		// Close the connection after sending 'Have a good day!'
		// if the client has sent 'bye'.
		if ( close )
		{
			future.addListener( ChannelFutureListener.CLOSE );
		}
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
