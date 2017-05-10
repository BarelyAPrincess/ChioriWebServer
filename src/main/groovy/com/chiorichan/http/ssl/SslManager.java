/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http.ssl;

import com.chiorichan.AppConfig;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.http.SslCertificateDefaultEvent;
import com.chiorichan.event.http.SslCertificateMapEvent;
import com.chiorichan.factory.api.Builtin;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.lang.StartupException;
import com.chiorichan.logger.Log;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.services.AppManager;
import com.chiorichan.services.ServiceManager;
import com.chiorichan.site.DomainMapping;
import com.chiorichan.site.SiteManager;
import com.chiorichan.utils.UtilHttp;
import com.chiorichan.utils.UtilIO;
import com.google.common.base.Joiner;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.Mapping;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.List;

public class SslManager implements ServiceManager, Mapping<String, SslContext>
{
	public static Log getLogger()
	{
		return AppManager.manager( SslManager.class ).getLogger();
	}

	public static SslManager instance()
	{
		return AppManager.manager( SslManager.class ).instance();
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

	@Override
	public String getLoggerId()
	{
		return "SSL";
	}

	public File getServerCertificateFile()
	{
		String file = AppConfig.get().getString( "server.httpsSharedCert", "server.crt" );
		return UtilIO.isAbsolute( file ) ? new File( file ) : new File( AppConfig.get().getDirectory().getAbsolutePath(), file );
	}

	public String getServerCertificateSecret()
	{
		return AppConfig.get().getString( "server.httpsSharedSecret" );
	}

	public File getServerKeyFile()
	{
		String file = AppConfig.get().getString( "server.httpsSharedKey", "server.key" );
		return UtilIO.isAbsolute( file ) ? new File( file ) : new File( AppConfig.get().getDirectory().getAbsolutePath(), file );
	}

	@Override
	public void init() throws StartupException
	{
		final File sslCert = getServerCertificateFile();
		final File sslKey = getServerKeyFile();
		final String sslSecret = AppConfig.get().getString( "server.httpsSharedSecret" );

		try
		{
			if ( sslCert == null || sslKey == null || !sslCert.exists() || !sslKey.exists() )
				selfSignCertificate();
			else
				try
				{
					updateDefaultCertificateWithException( sslCert, sslKey, sslSecret, true );
				}
				catch ( CertificateExpiredException e )
				{
					getLogger().severe( "The SSL Certificate specified in server configuration was expired. (" + e.getMessage() + ") Loading a self signed certificate." );
					selfSignCertificate();
				}
				catch ( FileNotFoundException e )
				{
					getLogger().severe( "SSL Certificate specified in server configuration was not found. Loading a self signed certificate." );
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
	public SslContext map( String host )
	{
		final String hostname = UtilHttp.normalize( host );

		if ( hostname != null )
		{
			SslCertificateMapEvent event = EventBus.instance().callEvent( new SslCertificateMapEvent( hostname ) );

			if ( event.getSslContext() != null )
				return event.getSslContext();

			DomainMapping mapping = SiteManager.instance().getDomainMapping( hostname );
			if ( mapping != null )
			{
				SslContext context = mapping.getSslContext( true );
				if ( context != null )
					return context;
			}
		}

		SslCertificateDefaultEvent event = EventBus.instance().callEvent( new SslCertificateDefaultEvent( hostname ) );

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
			AppConfig.get().set( "server.httpsSharedCert", UtilIO.relPath( sslCert ) );
			AppConfig.get().set( "server.httpsSharedKey", UtilIO.relPath( sslKey ) );
			AppConfig.get().set( "server.httpsSharedSecret", lastSslSecret );
			AppConfig.get().save();
		}

		X509Certificate cert = wrapper.getCertificate();

		try
		{
			cert.checkValidity();
		}
		catch ( CertificateExpiredException e )
		{
			getLogger().severe( "The server SSL certificate is expired, please obtain a renewed certificate ASAP." );
		}

		List<String> names = wrapper.getSubjectAltDNSNamesWithException();
		names.add( wrapper.getCommonNameWithException() );

		NetworkManager.getLogger().info( String.format( "Updating default SSL cert with '%s', key '%s', and hasSecret? %s", UtilIO.relPath( sslCert ), UtilIO.relPath( sslKey ), sslSecret != null && !sslSecret.isEmpty() ) );
		NetworkManager.getLogger().info( EnumColor.AQUA + "The SSL Certificate has the following DNS names: " + EnumColor.GOLD + Joiner.on( EnumColor.AQUA + ", " + EnumColor.GOLD ).join( names ) );
		NetworkManager.getLogger().info( EnumColor.AQUA + "The SSL Certificate will expire after: " + EnumColor.GOLD + Builtin.date( cert.getNotAfter() ) );

		serverContext = wrapper.context();
		lastSslCert = sslCert;
		lastSslKey = sslKey;
		lastSslSecret = sslSecret;
		usingSelfSignedCert = false; // TODO Check for Self Signed
	}

	/**
	 * Used to set/update the server wide global SSL certificate.
	 *
	 * @param sslCertFile The updated SSL Certificate
	 * @param sslKeyFile  The updated SSL Key
	 * @param sslSecret   The SSL Shared Secret
	 */
	public void updateDefaultCertificateWithException( final File sslCertFile, final File sslKeyFile, final String sslSecret, boolean updateConfig ) throws FileNotFoundException, SSLException, CertificateException
	{
		if ( !sslCertFile.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + UtilIO.relPath( sslCertFile ) + "' (aka. SSL Cert) file does not exist" );

		if ( !sslKeyFile.exists() )
			throw new FileNotFoundException( "We could not set the server SSL Certificate because the '" + UtilIO.relPath( sslKeyFile ) + "' (aka. SSL Key) file does not exist" );

		CertificateWrapper wrapper = new CertificateWrapper( sslCertFile, sslKeyFile, sslSecret );

		updateDefaultCertificateWithException( wrapper, updateConfig );
	}
}
