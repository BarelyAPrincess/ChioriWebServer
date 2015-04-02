/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.https;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.chiorichan.Loader;
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
			SSLContext context = SslContextFactory.getServerContext();
			
			if ( context == null )
			{
				NetworkManager.shutdownHttpsServer();
				Loader.getLogger().severe( "The SSL engine failed to initalize possibly due to a missing certificate file" );
				return;
			}
			
			SSLEngine engine = context.createSSLEngine();
			engine.setUseClientMode( false );
			
			p.addLast( "ssl", new SslHandler( engine ) );
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
