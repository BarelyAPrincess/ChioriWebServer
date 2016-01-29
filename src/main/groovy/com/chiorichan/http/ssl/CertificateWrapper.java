package com.chiorichan.http.ssl;

import io.netty.handler.ssl.SslContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLException;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import com.chiorichan.net.NetworkManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.ObjectFunc;
import com.chiorichan.util.SecureFunc;

public class CertificateWrapper
{
	public enum CertificateValidityState
	{
		Valid, NotYetValid, Expired;
	}

	private final File sslCertFile;
	private final File sslKeyFile;
	private final String sslSecret;

	private final X509Certificate cert;
	private SslContext context = null;

	public CertificateWrapper( File sslCertFile, File sslKeyFile, String sslSecret ) throws FileNotFoundException, CertificateException
	{
		if ( !sslCertFile.exists() )
			throw new FileNotFoundException( "The SSL Certificate '" + FileFunc.relPath( sslCertFile ) + "' (aka. SSL Cert) file does not exist" );

		if ( !sslKeyFile.exists() )
			throw new FileNotFoundException( "The SSL Key '" + FileFunc.relPath( sslKeyFile ) + "' (aka. SSL Key) file does not exist" );

		this.sslCertFile = sslCertFile;
		this.sslKeyFile = sslKeyFile;
		this.sslSecret = sslSecret;

		CertificateFactory cf;
		try
		{
			cf = CertificateFactory.getInstance( "X.509" );
		}
		catch ( CertificateException e )
		{
			throw new IllegalStateException( "Failed to initalize X.509 certificate factory." );
		}

		InputStream in = null;
		try
		{
			in = new FileInputStream( sslCertFile );
			cert = ( X509Certificate ) cf.generateCertificate( in );
		}
		finally
		{
			if ( in != null )
				IOUtils.closeQuietly( in );
		}
	}

	public CertificateValidityState checkValidity()
	{
		try
		{
			cert.checkValidity();
			return CertificateValidityState.Valid;
		}
		catch ( CertificateExpiredException e )
		{
			return CertificateValidityState.Expired;
		}
		catch ( CertificateNotYetValidException e )
		{
			return CertificateValidityState.NotYetValid;
		}
	}

	public SslContext context() throws SSLException, FileNotFoundException, CertificateException
	{
		if ( context == null )
		{
			if ( sslSecret == null || sslSecret.isEmpty() )
				context = SslContext.newServerContext( sslCertFile, sslKeyFile );
			else
				context = SslContext.newServerContext( sslCertFile, sslKeyFile, sslSecret );

			NetworkManager.getLogger().info( String.format( "Initalized SslContext %s using cert '%s', key '%s', and hasSecret? %s", context.getClass(), FileFunc.relPath( sslCertFile ), FileFunc.relPath( sslKeyFile ), sslSecret != null && !sslSecret.isEmpty() ) );
		}

		return context;
	}

	/**
	 * Returns the number of days left on this certificate
	 * Will return -1 if already expired
	 */
	public int daysRemaining()
	{
		if ( isExpired() )
			return -1;
		return Days.daysBetween( LocalDate.fromDateFields( new Date() ), LocalDate.fromDateFields( cert.getNotAfter() ) ).getDays();
	}

	public File getCertFile()
	{
		return sslCertFile;
	}

	public X509Certificate getCertificate()
	{
		return cert;
	}

	public String getCommonName()
	{
		try
		{
			return getCommonNameWithException();
		}
		catch ( CertificateEncodingException e )
		{
			return null;
		}
	}

	public String getCommonNameWithException() throws CertificateEncodingException
	{
		X500Name x500name = new JcaX509CertificateHolder( cert ).getSubject();
		RDN cn = x500name.getRDNs( BCStyle.CN )[0];

		return IETFUtils.valueToString( cn.getFirst().getValue() );
	}

	public byte[] getEncoded() throws CertificateEncodingException
	{
		return getCertificate().getEncoded();
	}

	public File getKeyFile()
	{
		return sslKeyFile;
	}

	public String getSslSecret()
	{
		return sslSecret;
	}

	public List<String> getSubjectAltDNSNames()
	{
		return getSubjectAltNames( 2 );
	}

	public List<String> getSubjectAltDNSNamesWithException() throws CertificateParsingException
	{
		return getSubjectAltNamesWithException( 2 );
	}

	public List<String> getSubjectAltNames( int type )
	{
		try
		{
			return getSubjectAltNamesWithException( type );
		}
		catch ( CertificateParsingException e )
		{
			return new ArrayList<>();
		}
	}

	public List<String> getSubjectAltNamesWithException( int type ) throws CertificateParsingException
	{
		/*
		 * otherName [0] OtherName,
		 * rfc822Name [1] IA5String,
		 * dNSName [2] IA5String,
		 * x400Address [3] ORAddress,
		 * directoryName [4] Name,
		 * ediPartyName [5] EDIPartyName,
		 * uniformResourceIdentifier [6] IA5String,
		 * iPAddress [7] OCTET STRING,
		 * registeredID [8] OBJECT IDENTIFIER}
		 */

		if ( type < 0 || type > 8 )
			throw new IllegalArgumentException( "Type range out of bounds!" );

		return new ArrayList<String>()
		{
			{

				if ( cert.getSubjectAlternativeNames() != null )
					for ( List<?> l : cert.getSubjectAlternativeNames() )
						try
						{
							int i = ObjectFunc.castToIntWithException( l.get( 0 ) );
							String dns = ObjectFunc.castToStringWithException( l.get( 1 ) );

							if ( i == type )
								add( dns );
						}
						catch ( ClassCastException e )
						{
							NetworkManager.getLogger().severe( e.getMessage() );
						}
			}
		};
	}

	public boolean isExpired()
	{
		return checkValidity() == CertificateValidityState.Expired;
	}

	public String md5()
	{
		return SecureFunc.md5( sslCertFile );
	}
}
