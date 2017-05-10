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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * Initializes the Query Server for Netty
 */
public class QueryServerInitializer extends ChannelInitializer<SocketChannel>
{
	private static final StringDecoder DECODER = new StringDecoder();
	private static final StringEncoder ENCODER = new StringEncoder();
	
	private static final QueryServerTerminal SERVER_HANDLER = new QueryServerTerminal();
	
	@Override
	public void initChannel( SocketChannel ch ) throws Exception
	{
		ChannelPipeline pipeline = ch.pipeline();
		
		pipeline.addLast( new DelimiterBasedFrameDecoder( 8192, Delimiters.lineDelimiter() ) );
		
		pipeline.addLast( DECODER );
		pipeline.addLast( ENCODER );
		
		pipeline.addLast( SERVER_HANDLER );
	}
}
