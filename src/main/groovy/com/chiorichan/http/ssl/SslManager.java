/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com> All Right Reserved.
 */
package com.chiorichan.http.ssl;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.Mapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.IDN;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.chiorichan.APILogger;
import com.chiorichan.Loader;
import com.chiorichan.LogColor;
import com.chiorichan.ServerManager;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.http.SslCertificateDefaultEvent;
import com.chiorichan.event.http.SslCertificateMapEvent;
import com.chiorichan.factory.api.Builtin;
import com.chiorichan.lang.StartupException;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;
import com.google.common.base.Joiner;

public class SslManager implements ServerManager, Mapping<String, SslContext>
{
	public static final SslManager INSTANCE = new SslManager();
	private static boolean isInitialized = false;

	private static final Pattern DNS_WILDCARD_PATTERN = Pattern.compile( "^\\*\\..*" );

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

	public static boolean matches( String hostNameTemplate, String hostName )
	{
		if ( DNS_WILDCARD_PATTERN.matcher( hostNameTemplate ).matches() )
			return hostNameTemplate.substring( 2 ).equals( hostName ) || hostName.endsWith( hostNameTemplate.substring( 1 ) );
		else
			return hostNameTemplate.equals( hostName );
	}

	private static boolean needsNormalization( String hostname )
	{
		final int length = hostname.length();
		for ( int i = 0; i < length; i++ )
		{
			int c = hostname.charAt( i );
			if ( c > 0x7F )
				return true;
		}
		return false;
	}

	private static String normalizeHostname( String hostname )
	{
		if ( needsNormalization( hostname ) )
			hostname = IDN.toASCII( hostname, IDN.ALLOW_UNASSIGNED );
		return hostname.toLowerCase( Locale.US );
	}

	private File lastSslCert;
	private File lastSslKey;
	private String lastSslSecret;

	private SslContext serverContext;

	private boolean usingSelfSignedCert = false;

	private SslManager()
	{

	}

	public File getLastCertificateFile()
	{
		return lastSslCert;
	}

	public File getLastKeyFile()
	{
		return lastSslKey;
	}

	public File getServerCertificateFile()
	{
		return new File( Loader.getConfig().getString( "server.httpsSharedCert", "server.crt" ) );
	}

	public String getServerCertificateSecret()
	{
		return Loader.getConfig().getString( "server.httpsSharedSecret" );
	}

	public File getServerKeyFile()
	{
		return new File( Loader.getConfig().getString( "server.httpsSharedKey", "server.key" ) );
	}

	public void init0() throws StartupException
	{
		final File sslCert = getServerCertificateFile();
		final File sslKey = getServerKeyFile();
		final String sslSecret = Loader.getConfig().getString( "server.httpsSharedSecret" );

		Security.addProvider( new BouncyCastleProvider() );

		try
		{
			if ( sslCert == null || sslKey == null || !sslCert.exists() || !sslKey.exists() )
				selfSignCertificate();
			else
				try
				{
					updateDefaultCertificateWithException( sslCert, sslKey, sslSecret, true );
				}
				catch ( FileNotFoundException e )
				{
					getLogger().severe( "SSL Certificate specified in server configuration was not found, loading a self signed certificate" );
					selfSignCertificate();
				}
		}
		catch ( CertificateException e )
		{
			throw new StartupException( "Certificate Exception Thrown", e );
		}
		catch ( SSLException e )
		{
			throw new StartupException( "SSL Exception Thrown", e );
		}
	}

	public boolean isUsingSelfSignedCert()
	{
		return usingSelfSignedCert;
	}

	@Override
	public SslContext map( String hostname )
	{
		if ( hostname != null )
		{
			hostname = normalizeHostname( hostname );

			SslCertificateMapEvent event = EventBus.INSTANCE.callEvent( new SslCertificateMapEvent( hostname ) );

			if ( event.getSslContext() != null )
				return event.getSslContext();

			for ( Site site : SiteManager.INSTANCE.getSites() )
				if ( site.getDefaultSslContext() != null )
					for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
					{
						if ( matches( normalizeHostname( "*." + e.getKey() ), hostname ) )
							return site.getDefaultSslContext();

						for ( String subdomain : e.getValue() )
						{
							SslContext context = site.getSslContext( e.getKey(), subdomain );
							if ( context != null )
								if ( matches( normalizeHostname( subdomain + "." + e.getKey() ), hostname ) )
									return context;
						}
					}
		}

		SslCertificateDefaultEvent event = EventBus.INSTANCE.callEvent( new SslCertificateDefaultEvent( hostname ) );

		if ( event.getSslContext() != null )
			return event.getSslContext();

		return serverContext;
	}

	public void reloadCertificate() throws FileNotFoundException, SSLException, CertificateException
	{
		updateDefaultCertificate( lastSslCert, lastSslKey, lastSslSecret, false );
	}

	private void selfSignCertificate() throws SSLException
	{
		getLogger().warning( "No proper server-wide SSL certificate was provided, we will generate an extremely insecure temporary self signed one for now but please obtain an official one or self sign one of your own ASAP." );

		try
		{
			SelfSignedCertificate ssc = new SelfSignedCertificate( "chiorichan.com" );
			updateDefaultCertificate( ssc.certificate(), ssc.privateKey(), null, false );
			usingSelfSignedCert = true;
		}
		catch ( FileNotFoundException | CertificateException e )
		{
			// Ignore
		}
	}

	public boolean updateDefaultCertificate( final CertificateWrapper wrapper, boolean updateConfig ) throws FileNotFoundException
	{
		try
		{
			updateDefaultCertificateWithException( wrapper, updateConfig );
			return true;
		}
		catch ( SSLException | CertificateException e )
		{
			NetworkManager.getLogger().severe( "Unexpected Exception thrown while updating default SSL certificate", e );
			return false;
		}
	}

	public boolean updateDefaultCertificate( final File sslCert, final File sslKey, final String sslSecret, boolean updateConfig ) throws FileNotFoundException
	{
		try
		{
			updateDefaultCertificateWithException( sslCert, sslKey, sslSecret, updateConfig );
			return true;
		}
		catch ( SSLException | CertificateException e )
		{
			NetworkManager.getLogger().severe( "Unexpected Exception thrown while updating default SSL certificate", e );
			return false;
		}
	}

	public void updateDefaultCertificateWithException( final CertificateWrapper wrapper, boolean updateConfig ) throws FileNotFoundException, SSLException, CertificateException
	{
		File sslCert = wrapper.getCertFile();
		File sslKey = wrapper.getKeyFile();
		String sslSecret = wrapper.getSslSecret();

		if ( updateConfig )
		{
			Loader.getConfig().set( "server.httpsSharedCert", FileFunc.relPath( sslCert ) );
			Loader.getConfig().set( "server.httpsSharedKey", FileFunc.relPath( sslKey ) );
			Loader.getConfig().set( "server.httpsSharedSecret", lastSslSecret );
			Loader.saveConfig();
		}

		X509Certificate cert = wrapper.getCertificate();

		cert.checkValidity();

		List<String> names = wrapper.getSubjectAltDNSNamesWithException();
		names.add( wrapper.getCommonNameWithException() );

		NetworkManager.getLogger().info( String.format( "Updating default SSL cert with '%s', key '%s', and hasSecret? %s", FileFunc.relPath( sslCert ), FileFunc.relPath( sslKey ), sslSecret != null && !sslSecret.isEmpty() ) );
		NetworkManager.getLogger().info( LogColor.AQUA + "The SSL Certificate has the following DNS names: " + LogColor.GOLD + Joiner.on( LogColor.AQUA + ", " + LogColor.GOLD ).join( names ) );
		NetworkManager.getLogger().info( LogColor.AQUA + "The SSL Certificate will expire after: " + LogColor.GOLD + Builtin.date( cert.getNotAfter() ) );

		serverContext = wrapper.context();
		lastSslCert = sslCert;
		lastSslKey = sslKey;
		lastSslSecret = sslSecret;
		usingSelfSignedCert = false; // TODO Check for Self Signed
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
	public void updateDefaultCertificateWithException( final File sslCertFile, final File sslKeyFile, final String sslSecret, boolean updateConfig ) throws FileNotFoundException, SSLException, CertificateException
	{
		if ( !sslCertFile.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + FileFunc.relPath( sslCertFile ) + "' (aka. SSL Cert) file does not exist" );

		if ( !sslKeyFile.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + FileFunc.relPath( sslKeyFile ) + "' (aka. SSL Key) file does not exist" );

		CertificateWrapper wrapper = new CertificateWrapper( sslCertFile, sslKeyFile, sslSecret );

		updateDefaultCertificateWithException( wrapper, updateConfig );
	}
}
