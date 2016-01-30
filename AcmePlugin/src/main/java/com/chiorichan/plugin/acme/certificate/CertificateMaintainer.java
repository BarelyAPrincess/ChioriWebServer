package com.chiorichan.plugin.acme.certificate;

import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.Validate;

import com.chiorichan.LogColor;
import com.chiorichan.configuration.ConfigurationSection;
import com.chiorichan.configuration.file.YamlConfiguration;
import com.chiorichan.http.ssl.CertificateWrapper;
import com.chiorichan.http.ssl.SslManager;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.site.Site;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.Namespace;
import com.chiorichan.util.ObjectStacker;
import com.chiorichan.util.SecureFunc;

public class CertificateMaintainer
{
	private static final ObjectStacker<Certificate> certificateStack = new ObjectStacker<>();
	private static Set<Certificate> certificates = new HashSet<>();
	private static boolean bakeCertificates = false;

	private static Certificate defaultCertificate = null;

	private static final Map<String, File> privateKeys = new HashMap<>();

	private static final AcmePlugin plugin = AcmePlugin.INSTANCE;

	static
	{
		ConfigurationSection keys = plugin.getSubConfig().getConfigurationSection( "keys", true );

		for ( String key : keys.getKeys() )
			if ( keys.isConfigurationSection( key ) )
			{
				ConfigurationSection section = keys.getConfigurationSection( key );
				File sslKeyFile = new File( section.getString( "file" ) );
				if ( FileFunc.checkMd5( sslKeyFile, section.getString( "md5" ) ) )
					privateKeys.put( key, sslKeyFile );
			}

		ConfigurationSection certificates = plugin.getSubConfig().getConfigurationSection( "certificates", true );

		for ( String key : certificates.getKeys() )
			try
			{
				if ( certificates.isConfigurationSection( key ) )
				{
					ConfigurationSection section = certificates.getConfigurationSection( key );
					Certificate cert = new Certificate( section );

					if ( cert.validateCertificate() )
						if ( "default".equals( key ) )
							setDefaultCertificate( cert );
						else
							loadCertificate( cert );
				}
			}
			catch ( Throwable e )
			{
				plugin.getLogger().severe( "There was a severe problem loading certificate key " + key, e );
			}
	}

	public static void bakeCertificates( boolean force )
	{
		if ( bakeCertificates || force )
		{
			certificateStack.clear();

			for ( Certificate cert : certificates )
				if ( cert.getCertificate() != null )
				{
					CertificateWrapper wrapper = cert.getCertificate();
					if ( wrapper != null )
					{
						List<String> domains = wrapper.getSubjectAltDNSNames();
						if ( wrapper.getCommonName() != null && !wrapper.getCommonName().contains( " " ) )
							domains.add( wrapper.getCommonName() );
						for ( String d : domains )
							if ( d != null && d.length() > 0 )
								certificateStack.value( new Namespace( d ).reverseOrder(), cert );
					}
				}

			bakeCertificates = false;
		}
	}

	public static boolean certificateLoaded( String key )
	{
		for ( Certificate cert : certificates )
			if ( cert.key().equals( key ) )
				return true;
		if ( key.equals( "default" ) && getDefaultCertificate() != null )
			return true;
		return false;
	}

	public static void checkAndSignCertificates() throws NoSuchAlgorithmException, IOException
	{
		for ( Certificate cert : certificates )
			cert.validateCertificate();

		if ( defaultCertificate == null && AcmePlugin.INSTANCE.isDefaultCertificateAllowed() )
			generateDefaultCertificate();
		else
			defaultCertificate.validateCertificate();

		plugin.getLogger().info( LogColor.AQUA + "Certificate check and sign process has finished without error." );
	}

	public static Certificate generateCertificate( Site site, String privateKeyId ) throws CertificateException, NoSuchAlgorithmException, IOException
	{
		Validate.notNull( site );

		if ( "default".equals( site.getSiteId() ) )
			throw new IllegalArgumentException( "You can't sign a certificate for the default site" );

		String key = site.getSiteId() + "Acme";

		if ( privateKeyId == null )
			privateKeyId = "domain";

		getPrivateKey( privateKeyId );

		ConfigurationSection section = plugin.getSubConfig().getConfigurationSection( "certificates." + key, true );
		section.set( "privateKey", privateKeyId );
		section.set( "mapping", Arrays.asList( site.getSiteId() ) );

		AcmePlugin.INSTANCE.saveConfig();

		Certificate cert = new Certificate( section );

		plugin.saveConfig();

		loadCertificate( cert );

		return cert;
	}

	public static void generateDefaultCertificate() throws NoSuchAlgorithmException, IOException
	{
		if ( getDefaultCertificate() != null )
			throw new IllegalStateException( "There is already a default certificate loaded" );

		String key = "default";

		ConfigurationSection section = plugin.getSubConfig().getConfigurationSection( "certificates." + key, true );
		section.set( "privateKey", "domain" );
		AcmePlugin.INSTANCE.saveConfig();

		Certificate cert = new Certificate( section );

		plugin.saveConfig();

		if ( cert.validateCertificate() )
			setDefaultCertificate( cert );
	}

	public static Certificate getDefaultCertificate()
	{
		return defaultCertificate;
	}

	public static SslContext getDefaultCertificateContext()
	{
		try
		{
			return defaultCertificate == null ? null : defaultCertificate.getCertificate() == null ? null : defaultCertificate.getCertificate().context();
		}
		catch ( SSLException | FileNotFoundException | CertificateException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	public static File getPrivateKey( String privateKeyIden ) throws NoSuchAlgorithmException, IOException
	{
		File sslKeyFile = privateKeys.get( privateKeyIden );

		if ( sslKeyFile == null )
			sslKeyFile = new File( plugin.getDataFolder(), privateKeyIden + ".key" );

		if ( !sslKeyFile.exists() )
			AcmePlugin.INSTANCE.getClient().getAcmeStorage().generatePrivateKey( sslKeyFile, plugin.getConfig().getInt( "config.defaultKeySize", 4096 ) );

		return sslKeyFile;
	}

	public static String getPrivateKeyIden( File sslKeyFile )
	{
		for ( Entry<String, File> e : privateKeys.entrySet() )
			if ( e.getValue() == sslKeyFile || e.getValue().getAbsolutePath().equals( sslKeyFile.getAbsolutePath() ) )
				return e.getKey();
		return null;
	}

	static void loadCertificate( Certificate cert )
	{
		if ( "default".equals( cert.key() ) )
			throw new IllegalArgumentException( "One does not simply load default certificate!" );

		for ( Certificate c : certificates )
			if ( c.key().equals( cert.key() ) )
				throw new IllegalStateException( "Certificate " + cert.key() + " is already loaded!" );

		certificates.add( cert );
		bakeCertificates( true );
	}

	public static SslContext map( String hostname )
	{
		bakeCertificates( false );

		Namespace ns = new Namespace( hostname ).reverseOrder();
		ObjectStacker<Certificate> stack = certificateStack.getChild( ns, false );

		if ( stack != null && stack.value() != null && stack.value().getCertificate() != null )
			try
			{
				return stack.value().getCertificate().context();
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
		{
			File sslKey = key.getValue();
			config.set( "keys." + key.getKey() + ".file", FileFunc.relPath( sslKey ) );
			if ( sslKey.exists() )
				config.set( "keys." + key.getKey() + ".md5", SecureFunc.md5( sslKey ) );
		}

		for ( Certificate cert : certificateStack.allValues() )
			if ( cert != null )
			{
				config.set( "certificates." + cert.key() + ".certFile", FileFunc.relPath( cert.getCertFile() ) );
				config.set( "certificates." + cert.key() + ".privateKey", getPrivateKeyIden( cert.getKeyFile() ) );
				config.set( "certificates." + cert.key() + ".uri", cert.certUri() );
				config.set( "certificates." + cert.key() + ".md5", cert.md5() );
			}
	}

	static void setDefaultCertificate( Certificate cert )
	{
		if ( !AcmePlugin.INSTANCE.isDefaultCertificateAllowed() )
			return;

		if ( !"default".equals( cert.key() ) )
			throw new IllegalArgumentException( "Certificate key is not default" );

		if ( cert.getCertificate() == null )
			throw new IllegalArgumentException( "There is a problem with the certificate" );

		try
		{
			SslManager.INSTANCE.updateDefaultCertificateWithException( cert.getCertificate(), false );
			defaultCertificate = cert;
		}
		catch ( FileNotFoundException | SSLException | CertificateException e )
		{
			plugin.getLogger().severe( "Failed to set Default SSL Certificate because", e );
		}
	}

	private CertificateMaintainer()
	{

	}
}