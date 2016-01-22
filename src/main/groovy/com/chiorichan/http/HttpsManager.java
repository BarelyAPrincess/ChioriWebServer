/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com> All Right Reserved.
 */
package com.chiorichan.http;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.DomainNameMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.commons.io.IOUtils;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.lang.StartupException;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;

public class HttpsManager implements ServerManager
{
	public static final HttpsManager INSTANCE = new HttpsManager();
	private static boolean isInitialized = false;

	public static SslContext context( File sslCert, File sslKey ) throws SSLException, FileNotFoundException, CertificateException
	{
		return context( sslCert, sslKey, null );
	}

	public static SslContext context( File sslCert, File sslKey, String sslSecret ) throws SSLException, FileNotFoundException, CertificateException
	{
		if ( !sslCert.exists() || !sslKey.exists() )
			throw new FileNotFoundException();

		CertificateFactory cf;
		try
		{
			cf = CertificateFactory.getInstance( "X.509" );
		}
		catch ( CertificateException e )
		{
			throw new IllegalStateException( "Failed to initalize X.509 certificate factory." );
		}

		X509Certificate cert;
		InputStream in = null;
		try
		{
			in = new FileInputStream( sslCert );
			cert = ( X509Certificate ) cf.generateCertificate( in );
		}
		finally
		{
			if ( in != null )
				IOUtils.closeQuietly( in );
		}

		cert.checkValidity();

		// TODO Improve Certificate Checking

		SslContext sslContext;
		if ( sslSecret == null || sslSecret.isEmpty() )
			sslContext = SslContext.newServerContext( sslCert, sslKey );
		else
			sslContext = SslContext.newServerContext( sslCert, sslKey, sslSecret );

		NetworkManager.getLogger().info( String.format( "Initalized new SslContext %s using cert '%s', key '%s', and hasSecret? %s", sslContext.toString(), sslCert.getName(), sslKey.getName(), sslSecret != null && !sslSecret.isEmpty() ) );

		return sslContext;
	}

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

	private DomainNameMapping<SslContext> mapping;
	private boolean changesMade = true;
	private SslContext serverContext;
	private boolean usingSelfSignedCert = false;

	private HttpsManager()
	{

	}

	public File getServerCertificateFile()
	{
		return new File( Loader.getConfig().getString( "server.httpsSharedCert", "server.crt" ) );
	}

	public File getServerKeyFile()
	{
		return new File( Loader.getConfig().getString( "server.httpsSharedKey", "server.key" ) );
	}

	public HttpsSniHandler getSniHandler()
	{
		if ( changesMade )
		{
			mapping = new DomainNameMapping<SslContext>( serverContext );

			for ( Site site : SiteManager.INSTANCE.getSites() )
				if ( site.getDefaultSslContext() != null )
					for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
					{
						mapping.add( "*." + e.getKey(), site.getDefaultSslContext() );
						Loader.getLogger().debug( "Mapping *." + e.getKey() + " to " + site.getDefaultSslContext() );

						for ( String subdomain : e.getValue() )
						{
							SslContext context = site.getSslContext( e.getKey(), subdomain );
							if ( context != null )
							{
								mapping.add( subdomain + "." + e.getKey(), context );
								Loader.getLogger().debug( "Mapping " + subdomain + "." + e.getKey() + " to " + context );
							}
						}
					}

			changesMade = false;
		}

		return new HttpsSniHandler( mapping );
	}

	public void init0() throws StartupException
	{
		final File sslCert = getServerCertificateFile();
		final File sslKey = getServerKeyFile();
		final String sslSecret = Loader.getConfig().getString( "server.httpsSharedSecret" );

		try
		{
			if ( sslCert == null || sslKey == null || !sslCert.exists() || !sslKey.exists() )
				selfSignCertificate();
			else
				try
				{
					HttpsManager.INSTANCE.updateCertificate( sslCert, sslKey, sslSecret, true );
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
			updateCertificate( ssc.certificate(), ssc.privateKey(), null, false );
			usingSelfSignedCert = true;
		}
		catch ( FileNotFoundException | CertificateException e )
		{
			// Ignore
		}
	}

	/**
	 * Used to set/update the server wide global SSL certificate.
	 *
	 * @param sslCert
	 *             The updated SSL Certificate
	 * @param sslKey
	 *             The updated SSL Key
	 * @param sslSecret
	 *             The SSL Shared Secret
	 */
	public void updateCertificate( final File sslCert, final File sslKey, final String sslSecret, boolean updateConfig ) throws FileNotFoundException, SSLException
	{
		if ( !sslCert.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + sslCert.getName() + "' (aka. SSL Cert) file does not exist. Please check your file path, obtain a new certificate, or disable SSL in server configuration." );

		if ( !sslKey.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + sslKey.getName() + "' (aka. SSL Key) file does not exist. Please check your file path, obtain a new certificate, or disable SSL in server configuration." );

		if ( updateConfig )
		{
			Loader.getConfig().set( "server.httpsSharedCert", FileFunc.relPath( sslCert ) );
			Loader.getConfig().set( "server.httpsSharedKey", FileFunc.relPath( sslKey ) );
			Loader.getConfig().set( "server.httpsSharedSecret", sslSecret );
			Loader.saveConfig();
		}

		NetworkManager.getLogger().info( String.format( "Initalizing the SslContext using cert '%s', key '%s', and hasSecret? %s", FileFunc.relPath( sslCert ), FileFunc.relPath( sslKey ), sslSecret != null && !sslSecret.isEmpty() ) );

		if ( sslSecret == null || sslSecret.isEmpty() )
			serverContext = SslContext.newServerContext( sslCert, sslKey );
		else
			serverContext = SslContext.newServerContext( sslCert, sslKey, sslSecret );

		usingSelfSignedCert = false; // TODO Check for Self Signed
		changesMade = true;
	}
}
