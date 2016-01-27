package com.chiorichan.plugin.acme;

import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.http.ssl.CertificateWrapper;
import com.chiorichan.http.ssl.CertificateWrapper.CertificateValidityState;
import com.chiorichan.http.ssl.SslManager;
import com.chiorichan.plugin.acme.api.AcmeCertificateRequest;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.Namespace;
import com.chiorichan.util.ObjectStacker;
import com.chiorichan.util.SecureFunc;
import com.google.common.base.Joiner;

public class CertificateMaintainer
{
	public static class Certificate extends CertificateWrapper
	{
		private final String key;
		private final String certificateUri;

		public Certificate( File sslCertFile, File sslKeyFile, String sslSecret, String key, String certificateUri ) throws FileNotFoundException, CertificateException
		{
			super( sslCertFile, sslKeyFile, sslSecret );
			this.key = key;
			this.certificateUri = certificateUri;
		}
	}

	private static Certificate defaultCertificate = null;
	private static final ObjectStacker<Certificate> certificateStack = new ObjectStacker<>();
	private static final Map<String, File> privateKeys = new HashMap<>();

	private static final AcmePlugin plugin = AcmePlugin.INSTANCE;

	static
	{
		ConfigurationSection keys = plugin.getSubConfig().getConfigurationSection( "keys", true );

		for ( String key : keys.getKeys() )
		{
			File sslKeyFile = new File( keys.getString( key ) );
			if ( sslKeyFile.exists() )
				privateKeys.put( key, sslKeyFile );
		}

		// TEMP!
		if ( !privateKeys.containsKey( "domain" ) )
			privateKeys.put( "domain", FileFunc.buildFile( plugin.getDataFolder(), "domain.key" ) );

		// TEMP!
		if ( !privateKeys.containsKey( "default" ) )
			privateKeys.put( "private", FileFunc.buildFile( plugin.getDataFolder(), "private.key" ) );

		ConfigurationSection certificates = plugin.getSubConfig().getConfigurationSection( "certificates", true );

		for ( String key : certificates.getKeys() )
			try
			{
				ConfigurationSection section = certificates.getConfigurationSection( key );

				String privateKey = section.getString( "privateKey", "domain" );

				File sslCertFile = section.has( "certFile" ) ? new File( section.getString( "certFile" ) ) : null;
				File sslKeyFile = privateKeys.get( privateKey );

				boolean certificateNeedsSigning = !sslCertFile.exists();

				if ( sslCertFile == null || sslKeyFile == null )
					certificateNeedsSigning = true;

				if ( sslKeyFile == null )
					privateKey = "domain";

				if ( !certificateNeedsSigning )
				{
					String md5 = section.getString( "md5" );
					String fileMd5 = SecureFunc.md5( sslCertFile );

					if ( fileMd5 == null )
						certificateNeedsSigning = true;

					if ( md5 == null )
						section.set( "md5", fileMd5 );
					else if ( !md5.equals( fileMd5 ) )
						certificateNeedsSigning = true;
				}

				if ( certificateNeedsSigning )
				{
					Set<String> domains = new HashSet<>();

					for ( String map : section.getAsList( "mapping", new ArrayList<String>() ) )
					{
						Site site = SiteManager.INSTANCE.getSiteById( map );
						if ( site != null )
							for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
							{
								domains.add( e.getKey() );
								for ( String s : e.getValue() )
									domains.add( s + "." + e.getKey() );
							}
					}

					// TODO Assign certificate signing to Scheduled Task, so we can double check domain verifications

					try
					{
						signNewCertificate( key, privateKey, domains );
					}
					catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | OperatorCreationException | AcmeException | IOException | StreamParsingException e )
					{
						e.printStackTrace();
					}
				}
				else
				{
					Certificate cert = new Certificate( sslCertFile, sslKeyFile, null, key, section.getString( "uri" ) );

					if ( cert.checkValidity() != CertificateValidityState.Valid )
						continue; // Renew Certificate

					if ( "default".equals( key ) )
						setDefaultCertificate( cert );
					else
						loadCertificate( cert );
				}
			}
			catch ( CertificateException e )
			{
				e.printStackTrace();
			}
			catch ( FileNotFoundException e )
			{
				// Ignore, previously checked
			}
	}

	public static void addCertificate( String key, File sslCertFile, String privateKey, String certUri ) throws FileNotFoundException, SSLException, CertificateException
	{
		if ( !sslCertFile.exists() )
			throw new FileNotFoundException( "We could not add SSL Certificate because the '" + FileFunc.relPath( sslCertFile ) + "' (aka. SSL Cert) file does not exist" );

		Validate.notNull( privateKey, "Private Key can't not be null" );

		File sslKeyFile = getPrivateKey( privateKey );

		if ( sslKeyFile == null )
			throw new FileNotFoundException( "Private Key does not exist!" );

		Certificate cert = new Certificate( sslCertFile, sslKeyFile, null, key, certUri );

		cert.getCertificate().checkValidity();

		YamlConfiguration config = plugin.getSubConfig();

		config.set( "certificates." + key + ".certFile", FileFunc.relPath( sslCertFile ) );
		config.set( "certificates." + key + ".privateKey", privateKey );
		config.set( "certificates." + key + ".uri", certUri );
		config.set( "certificates." + key + ".md5", cert.md5() );

		plugin.saveConfig();

		if ( "default".equals( key ) )
			setDefaultCertificate( cert );
		else
			loadCertificate( cert );
	}

	public static Certificate getDefaultCertificate()
	{
		return defaultCertificate;
	}

	public static SslContext getDefaultCertificateContext()
	{
		try
		{
			return defaultCertificate == null ? null : defaultCertificate.context();
		}
		catch ( SSLException | FileNotFoundException | CertificateException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public static File getPrivateKey( String privateKeyIden )
	{
		File sslKeyFile = privateKeys.get( privateKeyIden );

		// TODO Generate new key if null

		return sslKeyFile;
	}

	public static String getPrivateKeyIden( File sslKeyFile )
	{
		for ( Entry<String, File> e : privateKeys.entrySet() )
			if ( e.getValue() == sslKeyFile || e.getValue().getAbsolutePath().equals( sslKeyFile.getAbsolutePath() ) )
				return e.getKey();
		return null;
	}

	private static void loadCertificate( Certificate cert )
	{
		List<String> domains = cert.getSubjectAltDNSNames();
		if ( cert.getCommonName() != null && !cert.getCommonName().contains( " " ) )
			domains.add( cert.getCommonName() );

		for ( String d : domains )
			certificateStack.value( d, cert );
	}

	public static SslContext map( String hostname )
	{
		plugin.getLogger().debug( "Looking up hostname: " + hostname );

		Namespace ns = new Namespace( hostname ).reverseOrder();
		ObjectStacker<Certificate> stack = certificateStack.getChild( ns, false );

		if ( stack != null && stack.value() != null )
			try
			{
				return stack.value().context();
			}
			catch ( SSLException | FileNotFoundException | CertificateException e )
			{
				e.printStackTrace();
			}
		return null;
	}

	public static void saveConfig()
	{
		YamlConfiguration config = plugin.getSubConfig();

		for ( Entry<String, File> key : privateKeys.entrySet() )
			config.set( "keys." + key.getKey(), FileFunc.relPath( key.getValue() ) );

		for ( Certificate cert : certificateStack.allValues() )
		{
			config.set( "certificates." + cert.key + ".certFile", FileFunc.relPath( cert.getCertFile() ) );
			config.set( "certificates." + cert.key + ".privateKey", getPrivateKeyIden( cert.getKeyFile() ) );
			config.set( "certificates." + cert.key + ".uri", cert.certificateUri );
			config.set( "certificates." + cert.key + ".md5", cert.md5() );
		}
	}

	private static void setDefaultCertificate( Certificate cert )
	{
		if ( !"default".equals( cert.key ) )
			throw new IllegalArgumentException( "Certificate key is not default" );

		try
		{
			SslManager.INSTANCE.updateDefaultCertificateWithException( cert, false );
			defaultCertificate = cert;
		}
		catch ( FileNotFoundException | SSLException | CertificateException e )
		{
			plugin.getLogger().severe( "Failed to set Default SSL Certificate because", e );
		}
	}

	public static void signNewCertificate( String key, String privateKey, Set<String> domains ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, AcmeException, IOException, StreamParsingException
	{
		Loader.getLogger().debug( "Signing a new certificate for " + key + " and domains " + Joiner.on( ", " ).join( domains ) );

		AcmeCertificateRequest signingRequest = AcmePlugin.INSTANCE.getClient().newSigningRequest( domains );
		signingRequest.doCallback( true, new Runnable()
		{
			@Override
			public void run()
			{
				if ( signingRequest.getState() == AcmeState.SUCCESS )
					try
					{
						signingRequest.save( FileFunc.buildFile( plugin.getDataFolder(), key ) );
						File sslCertFile = FileFunc.buildFile( plugin.getDataFolder(), key, "fullchain.pem" );

						CertificateMaintainer.addCertificate( key, sslCertFile, privateKey, signingRequest.getUri() );
					}
					catch ( AcmeException | FileNotFoundException | SSLException | CertificateException e )
					{
						plugin.getLogger().severe( "Unexpected Exception Thrown", e );
					}
				else
					plugin.getLogger().severe( "Failed certificate signing for reason " + signingRequest.lastMessage() );
			}
		} );
	}

	public static void signNewDefaultCertificate( Set<String> domains ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, AcmeException, IOException, StreamParsingException
	{
		signNewCertificate( "default", "domain", domains );
	}

	private CertificateMaintainer()
	{

	}
}
