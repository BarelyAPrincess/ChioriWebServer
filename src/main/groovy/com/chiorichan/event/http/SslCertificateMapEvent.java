/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
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