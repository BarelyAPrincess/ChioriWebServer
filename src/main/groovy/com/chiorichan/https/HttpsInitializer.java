package com.chiorichan.https;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import com.chiorichan.http.HttpHandler;

public class HttpsInitializer extends ChannelInitializer<SocketChannel>
{
	@Override
	protected void initChannel( SocketChannel ch ) throws Exception
	{
		ChannelPipeline p = ch.pipeline();
		
		SSLEngine engine = SslContextFactory.getServerContext().createSSLEngine();
		engine.setUseClientMode( false );
		
		p.addLast( "ssl", new SslHandler( engine ) );
		
		p.addLast( "decoder", new HttpRequestDecoder() );
		// Uncomment the following line if you don't want to handle HttpChunks.
		// p.addLast("aggregator", new HttpObjectAggregator(1048576));
		p.addLast( "encoder", new HttpResponseEncoder() );
		p.addLast( "deflater", new HttpContentCompressor() );
		p.addLast( "handler", new HttpHandler() );
	}
}
