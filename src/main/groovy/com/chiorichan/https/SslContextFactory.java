/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.https;

import io.netty.handler.ssl.SslContext;

import java.io.File;

import javax.net.ssl.SSLException;

import com.chiorichan.Loader;
import com.chiorichan.lang.StartupException;
import com.chiorichan.net.NetworkManager;

public class SslContextFactory
{
	private static SslContext sslContext;
	
	private SslContextFactory()
	{
		
	}
	
	public static SslContext getContext()
	{
		return sslContext;
	}
	
	public static void init() throws StartupException
	{
		final File sslCert = new File( Loader.getConfig().getString( "server.httpsSharedCert", "server.crt" ) );
		final File sslKey = new File( Loader.getConfig().getString( "server.httpsSharedKey", "server.key" ) );
		final String sslSecret = Loader.getConfig().getString( "server.httpsSharedSecret" );
		
		if ( !sslCert.exists() )
			throw new StartupException( "We could not start the HTTPS Server because the '" + sslCert.getName() + "' (aka. SSL Cert) file does not exist. Please generate one and reload the server, or disable SSL in the configs." );
		
		try
		{
			NetworkManager.getLogger().info( String.format( "Initalizing the SslContext using cert '%s', key '%s', and hasSecret? %s", sslCert.getName(), sslKey.getName(), ( sslSecret != null && !sslSecret.isEmpty() ) ) );
			
			if ( sslSecret == null || sslSecret.isEmpty() )
				sslContext = SslContext.newServerContext( sslCert, sslKey );
			else
				sslContext = SslContext.newServerContext( sslCert, sslKey, sslSecret );
		}
		catch ( SSLException e )
		{
			throw new StartupException( "We could not start the HTTPS Server because " + e.getMessage(), e );
		}
		catch ( Exception e )
		{
			throw new StartupException( "We could not start the HTTPS Server for an uncaught exception", e );
		}
	}
}
