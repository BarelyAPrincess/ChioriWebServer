package com.chiorichan.event.http;

import io.netty.handler.ssl.SslContext;

import com.chiorichan.event.AbstractEvent;

public class SslCertificateMapEvent extends AbstractEvent
{
	private final String hostname;
	private SslContext context = null;

	public SslCertificateMapEvent( String hostname )
	{
		this.hostname = hostname;
	}

	public String getHostname()
	{
		return hostname;
	}

	public SslContext getSslContext()
	{
		return context;
	}

	public void setContext( SslContext context )
	{
		this.context = context;
	}
}
