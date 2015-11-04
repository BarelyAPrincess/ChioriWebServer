/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.https;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import com.chiorichan.http.HttpHandler;
import com.chiorichan.net.NetworkManager;

public class HttpsInitializer extends ChannelInitializer<SocketChannel>
{
	@Override
	protected void initChannel( SocketChannel ch ) throws Exception
	{
		ChannelPipeline p = ch.pipeline();
		
		try
		{
			p.addLast( SslContextFactory.getContext().newHandler( ch.alloc() ) );
		}
		catch ( Exception e )
		{
			NetworkManager.shutdownHttpsServer();
			throw new IllegalStateException( "The SSL engine failed to initalize", e );
		}
		
		p.addLast( "decoder", new HttpRequestDecoder() );
		p.addLast( "aggregator", new HttpObjectAggregator( 104857600 ) );
		p.addLast( "encoder", new HttpResponseEncoder() );
		p.addLast( "deflater", new HttpContentCompressor() );
		p.addLast( "handler", new HttpHandler( true ) );
	}
}
