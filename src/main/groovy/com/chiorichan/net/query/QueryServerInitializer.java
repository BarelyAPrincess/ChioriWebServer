/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
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
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class QueryServerInitializer extends ChannelInitializer<SocketChannel>
{
	private static final StringDecoder DECODER = new StringDecoder();
	private static final StringEncoder ENCODER = new StringEncoder();
	
	private static final QueryServerHandler SERVER_HANDLER = new QueryServerHandler();
	
	@Override
	public void initChannel( SocketChannel ch ) throws Exception
	{
		ChannelPipeline pipeline = ch.pipeline();
		
		// Add the text line codec combination first,
		pipeline.addLast( new DelimiterBasedFrameDecoder( 8192, Delimiters.lineDelimiter() ) );
		// the encoder and decoder are static as these are sharable
		pipeline.addLast( DECODER );
		pipeline.addLast( ENCODER );
		
		// and then business logic.
		pipeline.addLast( SERVER_HANDLER );
	}
}
