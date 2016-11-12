package com.chiorichan.plugin.acme.certificate;

import io.jsonwebtoken.impl.TextCodec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.AppConfig;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.http.HttpCode;
import com.chiorichan.http.ssl.CertificateWrapper;
import com.chiorichan.lang.EnumColor;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.AcmeScheduledTask;
import com.chiorichan.plugin.acme.api.AcmeCertificateRequest;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.api.AcmeUtils;
import com.chiorichan.plugin.acme.api.CertificateDownloader;
import com.chiorichan.plugin.acme.api.HttpResponse;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.SecureFunc;
import com.google.common.base.Joiner;

public class Certificate
{
	private File sslCertFile;
	private File sslKeyFile;
	private final String key;
	private boolean pendingUpdate = false;
	private String uri;
	private ConfigurationSection configSection;

	private CertificateWrapper certificateCache = null;
	private final Set<String> domains = new HashSet<>();

	public Certificate( ConfigurationSection configSection ) throws NoSuchAlgorithmException, IOException
	{
		String privateKey = configSection.getString( "privateKey", "domain" );

		sslCertFile = configSection.has( "certFile" ) ? FileFunc.isAbsolute( configSection.getString( "certFile" ) ) ? new File( configSection.getString( "certFile" ) ) : new File( AppConfig.get().getDirectory().getAbsolutePath(), configSection.getString( "certFile" ) ) : null;
		sslKeyFile = CertificateMaintainer.getPrivateKey( privateKey );

		this.key = configSection.getName();
		this.configSection = configSection;
		this.uri = configSection.getString( "uri" );

		if ( key.equals( "default" ) )
			domains.addAll( AcmeScheduledTask.getVerifiedDomains() );
		else
			// TODO Allow mapping to contain domains and/or siteIds
			for ( String map : configSection.getAsList( "mapping", new ArrayList<String>() ) )
			{
				Site site = SiteManager.instance().getSiteById( map );
				if ( site != null )
					for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
					{
						domains.add( e.getKey() );
						for ( String s : e.getValue() )
							domains.add( s + "." + e.getKey() );
					}
			}
	}

	public String certUri()
	{
		return uri;
	}

	public File getCertFile()
	{
		return sslCertFile;
	}

	public CertificateWrapper getCertificate()
	{
		if ( certificateCache == null )
			try
			{
				certificateCache = new CertificateWrapper( sslCertFile, sslKeyFile, null );
				domains.addAll( certificateCache.getSubjectAltDNSNames() );
			}
			catch ( FileNotFoundException | CertificateException e )
			{
				e.printStackTrace();
				return null;
			}

		return certificateCache;
	}

	public File getKeyFile()
	{
		return sslKeyFile;
	}

	public String key()
	{
		return key;
	}

	public String md5()
	{
		if ( configSection.getString( "md5" ) == null && sslCertFile != null )
			configSection.set( "md5", SecureFunc.md5( sslCertFile ) );
		return configSection.getString( "md5" );
	}

	private void renewCertificate() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, AcmeException, StreamParsingException, OperatorCreationException, IOException
	{
		if ( pendingUpdate )
			throw new AcmeException( "Certificate " + key() + " is pending an update!" );

		Log.get().info( EnumColor.AQUA + "Attempting to renew certificate: " + key() );

		CertificateDownloader downloader = new CertificateDownloader( null, certUri() );

		File parentDir = new File( AcmePlugin.instance().getDataFolder(), key );

		do
			downloader.save( parentDir );
		while ( downloader.isPending() && !downloader.isDownloaded() );

		File sslCertFileTemp = new File( parentDir, "fullchain.pem" );

		// Log.get().debug( "Old MD5 " + md5() + " and New MD5 " + SecureFunc.md5( sslCertFileTemp ) );

		// According to the Acme Protocol, it is plausible for the CA to not response with a renewed certificate. For now, we compare the old and new certificate MD5 then revoke and reissue to force a renewal.
		if ( md5().equals( SecureFunc.md5( sslCertFileTemp ) ) )
		{
			Log.get().warning( "The CA did not provide a renewed certifcate, we will request the certificate is revoked and reissued now." );
			revokeCertificate();
			signNewCertificate();
		}

		sslCertFile = sslCertFileTemp;
		certificateCache = null;

		save();
	}

	public boolean revokeCertificate()
	{
		if ( pendingUpdate )
			return false;

		try
		{
			if ( getCertificate() == null )
				return false;

			pendingUpdate = true;

			AcmeProtocol proto = AcmePlugin.instance().getClient();

			String body = proto.newJwt( new TreeMap<String, Object>()
			{
				{
					put( "resource", "revoke-cert" );
					put( "certificate", TextCodec.BASE64URL.encode( getCertificate().getEncoded() ) );
				}
			} );

			HttpResponse response = AcmeUtils.post( "POST", proto.getUrlNewRevoke(), "application/json", body, "application/json" );
			proto.nonce( response.getHeaderString( "Replay-Nonce" ) );

			pendingUpdate = false;

			if ( response.getStatus() == HttpCode.HTTP_OK )
			{
				certificateCache = null;
				return true;
			}
			else
				return false;
		}
		catch ( AcmeException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | IOException | CertificateEncodingException e )
		{
			return false;
		}
	}

	private void save()
	{
		ConfigurationSection section = AcmePlugin.instance().getSubConfig().getConfigurationSection( "certificates." + key, true );

		section.set( "certFile", FileFunc.relPath( sslCertFile ) );
		section.set( "privateKey", CertificateMaintainer.getPrivateKeyIden( sslKeyFile ) );
		section.set( "uri", certUri() );
		section.set( "md5", SecureFunc.md5( sslCertFile ) );

		AcmePlugin.instance().saveConfig();
	}

	private void signNewCertificate() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, AcmeException, IOException, StreamParsingException
	{
		if ( pendingUpdate )
			throw new AcmeException( "Certificate " + key() + " is pending an update!" );

		final Certificate cert = this;
		pendingUpdate = true;

		AcmeCertificateRequest signingRequest = AcmePlugin.instance().getClient().newSigningRequest( domains, sslKeyFile );
		signingRequest.doCallback( true, new Runnable()
		{
			@Override
			public void run()
			{
				if ( signingRequest.getState() == AcmeState.SUCCESS )
					try
					{
						File parentDir = FileFunc.buildFile( AcmePlugin.instance().getDataFolder(), key );

						signingRequest.getDownloader().save( parentDir );
						sslCertFile = new File( parentDir, "fullchain.pem" );
						uri = signingRequest.getDownloader().getCertificateUri();

						save();

						if ( key.equals( "default" ) )
							CertificateMaintainer.setDefaultCertificate( cert );
						else if ( !CertificateMaintainer.certificateLoaded( key ) )
							CertificateMaintainer.loadCertificate( cert );
					}
					catch ( AcmeException e )
					{
						AcmePlugin.instance().getLogger().severe( "Unexpected Exception Thrown", e );
					}
				else
					AcmePlugin.instance().getLogger().severe( "Failed certificate signing for reason " + signingRequest.lastMessage() );

				pendingUpdate = false;
			}
		} );
	}

	public boolean validateCertificate()
	{
		try
		{
			if ( pendingUpdate )
				return false;

			if ( sslKeyFile == null )
				sslKeyFile = CertificateMaintainer.getPrivateKey( "domain" );

			if ( key.equals( "default" ) )
				domains.addAll( AcmeScheduledTask.getVerifiedDomains() );

			if ( !FileFunc.checkMd5( sslCertFile, md5() ) )
			{
				if ( sslCertFile == null )
					sslCertFile = FileFunc.buildFile( AcmePlugin.instance().getDataFolder(), key, "fullchain.pem" );

				if ( certUri() == null )
				{
					signNewCertificate();
					return false;
				}
				else
					renewCertificate();
			}

			// If the certificate will expire in less than 15 days, we will attempt to renew it.
			if ( getCertificate() == null || getCertificate().daysRemaining() < 15 )
				renewCertificate();

			if ( key.equals( "default" ) && getCertificate() != null )
			{
				List<String> dnsNames = getCertificate().getSubjectAltDNSNames();
				if ( !dnsNames.containsAll( domains ) )
				{
					Log.get().debug( "Default certificate is missing domains --> " + Joiner.on( ", " ).join( domains ) + " // " + Joiner.on( ", " ).join( dnsNames ) );

					revokeCertificate();
					signNewCertificate();
					return false;
				}
			}

			return true;
		}
		catch ( AcmeException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | OperatorCreationException | IOException | StreamParsingException e )
		{
			e.printStackTrace();
			return false;
		}
	}
}
