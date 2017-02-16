/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http.ssl;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.lang.ref.WeakReference;
import java.util.List;

import com.chiorichan.http.HttpHandler;
import com.chiorichan.net.NetworkManager;
import com.google.common.collect.Lists;

public class SslInitializer extends ChannelInitializer<SocketChannel>
{
	public static final List<WeakReference<SocketChannel>> activeChannels = Lists.newCopyOnWriteArrayList();

	@Override
	protected void initChannel( SocketChannel ch ) throws Exception
	{
		ChannelPipeline p = ch.pipeline();

		try
		{
			p.addLast( new SniNegotiator() );
		}
		catch ( Exception e )
		{
			NetworkManager.shutdownHttpsServer();
			throw new IllegalStateException( "The SSL engine failed to initialize", e );
		}

		p.addLast( "decoder", new HttpRequestDecoder() );
		p.addLast( "aggregator", new HttpObjectAggregator( Integer.MAX_VALUE ) );
		p.addLast( "encoder", new HttpResponseEncoder() );
		p.addLast( "deflater", new HttpContentCompressor() );
		p.addLast( "handler", new HttpHandler( true ) );

		activeChannels.add( new WeakReference<>( ch ) );
	}
}
