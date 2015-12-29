package com.chiorichan.plugin.acme.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.bouncycastle.jce.provider.X509CertParser;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;

public class AcmeStorage
{
	private static void savePEM( OutputStream outputStream, Object object ) throws IOException
	{
		try ( JcaPEMWriter writer = new JcaPEMWriter( new PrintWriter( outputStream ) ) )
		{
			writer.writeObject( object );
		}
	}

	private File data;

	public AcmeStorage( File data )
	{
		this.data = data;
	}

	public KeyPair defaultPrivateKey() throws AcmeException
	{
		return privateKey( "private", 4096 );
	}

	public KeyPair domainPrivateKey( Collection<String> domains ) throws AcmeException
	{
		try
		{
			KeyPair first = null;

			for ( String domain : domains )
			{
				Site site = SiteManager.INSTANCE.getSiteByDomain( domain );

				if ( site != null )
				{
					File siteDirectory = site.directory( "ssl" );
					File privateKey = new File( siteDirectory, "privkey.pem" );

					if ( first == null )
						first = privateKey( privateKey, 2048 );

					saveKeyPair( privateKey, first );
				}
				else
					Loader.getLogger().warning( "Failed to find site for domain " + domain );
			}

			return first;
		}
		catch ( IOException e )
		{
			throw new AcmeException( e );
		}
	}

	protected KeyPair generatePrivateKey( File privateKey, int keySize ) throws NoSuchAlgorithmException, IOException
	{
		Validate.notNull( privateKey );

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "RSA" );
		keyGen.initialize( keySize );
		KeyPair keyPair = keyGen.generateKeyPair();

		OutputStream stream = new FileOutputStream( privateKey );

		try ( JcaPEMWriter writer = new JcaPEMWriter( new PrintWriter( stream ) ) )
		{
			writer.writeObject( keyPair );
			writer.close();
		}
		finally
		{
			stream.close();
		}

		return keyPair;
	}

	protected KeyPair loadPrivateKey( File privateKey ) throws IOException
	{
		Validate.notNull( privateKey );

		InputStream stream = new FileInputStream( privateKey );

		try ( PEMParser pemParser = new PEMParser( new InputStreamReader( stream ) ) )
		{
			PEMKeyPair keyPair = ( PEMKeyPair ) pemParser.readObject();
			return new JcaPEMKeyConverter().getKeyPair( keyPair );
		}
		finally
		{
			stream.close();
		}
	}

	protected KeyPair privateKey( File privateKey, int keySize ) throws AcmeException
	{
		try
		{
			if ( privateKey.exists() )
				return loadPrivateKey( privateKey );
			else
				return generatePrivateKey( privateKey, keySize );
		}
		catch ( NoSuchAlgorithmException | IOException e )
		{
			throw new AcmeException( "We failed to generate or load a new private key", e );
		}
	}

	protected KeyPair privateKey( String keyName, int keySize ) throws AcmeException
	{
		return privateKey( new File( data, keyName + ".key" ), keySize );
	}

	public void saveCertificate( File parentDir, X509Certificate certificate ) throws AcmeException
	{
		try
		{
			String caIntermediateCertificateURL = AcmeUtils.getCACertificateURL( certificate );
			X509CertificateObject caIntermediateCertificate = null;
			if ( caIntermediateCertificateURL != null )
				try ( InputStream is = new URL( caIntermediateCertificateURL ).openStream() )
				{
					X509CertParser certParser = new X509CertParser();
					certParser.engineInit( is );
					caIntermediateCertificate = ( X509CertificateObject ) certParser.engineRead();
				}

			try ( OutputStream outputStream = new FileOutputStream( new File( parentDir, "cert.pem" ) ) )
			{
				savePEM( outputStream, certificate );
			}
			catch ( IOException e )
			{
				throw new AcmeException( e );
			}

			if ( caIntermediateCertificate != null )
				try ( OutputStream outputStream = new FileOutputStream( new File( parentDir, "chain.pem" ) ) )
				{
					savePEM( outputStream, caIntermediateCertificate );
				}
		}
		catch ( IOException | StreamParsingException e )
		{
			throw new AcmeException( e );
		}
	}

	public void saveCertificate( List<String> domains, X509Certificate certificate ) throws AcmeException
	{
		try
		{
			String caIntermediateCertificateURL = AcmeUtils.getCACertificateURL( certificate );
			X509CertificateObject caIntermediateCertificate = null;
			if ( caIntermediateCertificateURL != null )
				try ( InputStream is = new URL( caIntermediateCertificateURL ).openStream() )
				{
					X509CertParser certParser = new X509CertParser();
					certParser.engineInit( is );
					caIntermediateCertificate = ( X509CertificateObject ) certParser.engineRead();
				}

			for ( String domain : domains )
			{
				Site site = SiteManager.INSTANCE.getSiteByDomain( domain );

				if ( site != null )
				{
					try ( OutputStream outputStream = new FileOutputStream( new File( site.directory( "ssl" ), "cert.pem" ) ) )
					{
						savePEM( outputStream, certificate );
					}
					catch ( IOException e )
					{
						throw new AcmeException( e );
					}

					if ( caIntermediateCertificate != null )
						try ( OutputStream outputStream = new FileOutputStream( new File( site.directory( "ssl" ), "chain.pem" ) ) )
						{
							savePEM( outputStream, caIntermediateCertificate );
						}
				}
				else
					Loader.getLogger().severe( "Failed for find site for domain " + domain );
			}
		}
		catch ( IOException | StreamParsingException e )
		{
			throw new AcmeException( e );
		}
	}

	public void saveCertificationRequest( File parentDir, PKCS10CertificationRequest csr ) throws AcmeException
	{
		try ( OutputStream outputStream = new FileOutputStream( new File( parentDir, "cert.csr" ) ) )
		{
			savePEM( outputStream, csr );
		}
		catch ( IOException e )
		{
			throw new AcmeException( e );
		}
	}

	public void saveCertificationRequest( List<String> domains, PKCS10CertificationRequest csr ) throws AcmeException
	{
		for ( String domain : domains )
		{
			Site site = SiteManager.INSTANCE.getSiteByDomain( domain );

			if ( site != null )
				try ( OutputStream outputStream = new FileOutputStream( new File( site.directory( "ssl" ), "cert.csr" ) ) )
				{
					savePEM( outputStream, csr );
				}
				catch ( IOException e )
				{
					throw new AcmeException( e );
				}
			else
				Loader.getLogger().severe( "Failed for find site for domain " + domain );
		}
	}

	protected void saveKeyPair( File privateKey, KeyPair keyPair ) throws IOException
	{
		try ( OutputStream outputStream = new FileOutputStream( privateKey ) )
		{
			savePEM( outputStream, keyPair );
			outputStream.close();
		}
	}
}
