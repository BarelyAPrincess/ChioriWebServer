/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com> All Right Reserved.
 */
package com.chiorichan.https;

import io.netty.handler.ssl.SniHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.DomainNameMapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.lang.StartupException;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;

public class HttpsManager implements ServerManager
{
	public static final HttpsManager INSTANCE = new HttpsManager();
	private static boolean isInitialized = false;
	public static APILogger getLogger()
	{
		return Loader.getLogger( "SSL" );
	}

	public static void init() throws StartupException
	{
		if ( isInitialized )
			throw new IllegalStateException( "The SSL Manager has already been initialized." );

		assert INSTANCE != null;

		INSTANCE.init0();

		isInitialized = true;
	}

	private boolean usingSelfSignedCert = false;

	private DomainNameMapping<SslContext> mapping;

	private HttpsManager()
	{

	}

	public void addMapping( String hostname, File sslCert, File sslKey ) throws SSLException, FileNotFoundException
	{
		addMapping( hostname, sslCert, sslKey, null );
	}

	public void addMapping( String hostname, File sslCert, File sslKey, String sslSecret ) throws SSLException, FileNotFoundException
	{
		// TODO Open SSL certificate and confirm that the CN contains the provided hostname

		if ( !sslCert.exists() || !sslKey.exists() )
			throw new FileNotFoundException();

		NetworkManager.getLogger().info( String.format( "Initalizing a new SslContext using cert '%s', key '%s', and hasSecret? %s for hostname '%s'", sslCert.getName(), sslKey.getName(), sslSecret != null && !sslSecret.isEmpty(), hostname ) );

		SslContext sslContext;
		if ( sslSecret == null || sslSecret.isEmpty() )
			sslContext = SslContext.newServerContext( sslCert, sslKey );
		else
			sslContext = SslContext.newServerContext( sslCert, sslKey, sslSecret );

		addMapping( hostname, sslContext );
	}

	public void addMapping( String hostname, SslContext context )
	{
		// Using *.example.com will include all subdomains, including the root TLD
		mapping.add( hostname, context );
	}

	public SniHandler getSniHandler()
	{
		if ( mapping == null )
			throw new IllegalStateException( "The SSL Virtual Host Mapping is null, has the HttpsManager been properly initalized?" );

		return new SniHandler( mapping );
	}

	public void init0() throws StartupException
	{
		final File sslCert = new File( Loader.getConfig().getString( "server.httpsSharedCert", "server.crt" ) );
		final File sslKey = new File( Loader.getConfig().getString( "server.httpsSharedKey", "server.key" ) );
		final String sslSecret = Loader.getConfig().getString( "server.httpsSharedSecret" );

		try
		{
			if ( sslCert == null || sslKey == null )
				selfSignCertificate();
			else
				try
				{
					HttpsManager.INSTANCE.updateCertificate( sslCert, sslKey, sslSecret );
				}
				catch ( FileNotFoundException e )
				{
					getLogger().severe( e.getMessage() );
					selfSignCertificate();
				}
		}
		catch ( SSLException e )
		{
			throw new StartupException( "We could not start the server because the HttpsManager has thrown a Exception", e );
		}
	}

	public boolean isUsingSelfSignedCert()
	{
		return usingSelfSignedCert;
	}

	private void selfSignCertificate() throws SSLException
	{
		getLogger().warning( "No proper server-wide SSL certificate was provided, we will generate an extremely insecure temporary self signed one for now but please obtain an official one or self sign one of your own ASAP." );

		try
		{
			SelfSignedCertificate ssc = new SelfSignedCertificate( "chiorichan.com" );
			updateCertificate( ssc.certificate(), ssc.privateKey(), null );
			usingSelfSignedCert = true;
		}
		catch ( FileNotFoundException | CertificateException e )
		{
			// Ignore
		}
	}

	/**
	 * Used to set/update the server-wide global SSL certificate Warning: if you are updating the certificate the previous virtual host mappings will be lost to the GC
	 *
	 * @param sslCert
	 *             The updated SSL Certificate
	 * @param sslKey
	 *             The updated SSL Key
	 * @param sslSecret
	 *             The SSL Shared Secret
	 */
	public void updateCertificate( final File sslCert, final File sslKey, final String sslSecret ) throws FileNotFoundException, SSLException
	{
		if ( !sslCert.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + sslCert.getName() + "' (aka. SSL Cert) file does not exist. Please check your file path, obtain a new certificate, or disable SSL in server configuration." );

		if ( !sslKey.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + sslKey.getName() + "' (aka. SSL Key) file does not exist. Please check your file path, obtain a new certificate, or disable SSL in server configuration." );

		NetworkManager.getLogger().info( String.format( "Initalizing the SslContext using cert '%s', key '%s', and hasSecret? %s", sslCert.getName(), sslKey.getName(), sslSecret != null && !sslSecret.isEmpty() ) );

		SslContext sslContext;
		if ( sslSecret == null || sslSecret.isEmpty() )
			sslContext = SslContext.newServerContext( sslCert, sslKey );
		else
			sslContext = SslContext.newServerContext( sslCert, sslKey, sslSecret );

		mapping = new DomainNameMapping<SslContext>( sslContext );

		for ( Site site : SiteManager.INSTANCE.getSites() )
			site.loadSsl( this );

		usingSelfSignedCert = false;
	}
}
