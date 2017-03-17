package com.chiorichan.plugin.acme.api;

import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.utils.UtilIO;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.provider.X509CertParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.x509.util.StreamParsingException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AcmeUtils
{
	private static final Encoder BASE64_ENC = Base64.getUrlEncoder();

	private static Gson gson = null;

	private static String base64UrlEncode( final byte[] v )
	{
		return BASE64_ENC.encodeToString( v ).replaceAll( "=*$", "" );
	}

	public static PKCS10CertificationRequest createCertificationRequest( KeyPair pair, Collection<String> domains, String country, String state, String city, String organization ) throws OperatorCreationException, IOException
	{
		if ( domains.size() == 0 )
			throw new IllegalArgumentException( "You must provide at least one domain" );

		X500NameBuilder namebuilder = new X500NameBuilder( X500Name.getDefaultStyle() );

		if ( country != null )
			namebuilder.addRDN( BCStyle.C, country );
		if ( state != null )
			namebuilder.addRDN( BCStyle.ST, state );
		if ( city != null )
			namebuilder.addRDN( BCStyle.L, city );
		if ( organization != null )
			namebuilder.addRDN( BCStyle.O, organization );

		namebuilder.addRDN( BCStyle.CN, domains.toArray( new String[0] )[0] );

		List<GeneralName> subjectAltNames = Lists.newArrayList();
		for ( String cn : domains )
			subjectAltNames.add( new GeneralName( GeneralName.dNSName, cn ) );
		GeneralNames subjectAltName = new GeneralNames( subjectAltNames.toArray( new GeneralName[0] ) );

		ExtensionsGenerator extGen = new ExtensionsGenerator();
		extGen.addExtension( Extension.subjectAlternativeName, false, subjectAltName.toASN1Primitive() );

		PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder( namebuilder.build(), pair.getPublic() );
		p10Builder.addAttribute( PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate() );
		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder( "SHA256withRSA" );
		ContentSigner signer = csBuilder.build( pair.getPrivate() );
		PKCS10CertificationRequest request = p10Builder.build( signer );
		return request;
	}

	public static X509Certificate extractCertificate( ByteBuf body ) throws AcmeException, StreamParsingException
	{
		if ( body.readableBytes() == 0 )
			throw new AcmeException( "Certificate body length is zero!" );
		X509CertParser certParser = new X509CertParser();
		certParser.engineInit( new ByteBufInputStream( body ) );
		X509Certificate certificate = ( X509Certificate ) certParser.engineRead();
		return certificate;
	}

	public static HttpResponse get( final String target, final String accept ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException
	{
		try
		{
			final URL u = new URL( target );
			final HttpURLConnection c = ( HttpURLConnection ) u.openConnection();

			if ( c instanceof HttpsURLConnection )
			{
				SSLContext context = SSLContext.getInstance( "TLSv1.2" );
				context.init( null, new TrustManager[] {AcmeTrustManager.INSTANCE}, null );
				( ( HttpsURLConnection ) c ).setSSLSocketFactory( context.getSocketFactory() );
			}

			c.setRequestMethod( "GET" );
			c.setRequestProperty( "Accept", accept );

			int status = c.getResponseCode();
			Map<String, List<String>> responseHeader = c.getHeaderFields();

			InputStream is = status < 400 ? c.getInputStream() : c.getErrorStream();
			return new HttpResponse( target, status, responseHeader, Unpooled.wrappedBuffer( UtilIO.inputStream2Bytes( is ) ) );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
			return null;
		}
	}

	public static String getCACertificateURL( X509Certificate certificate ) throws IOException
	{
		byte[] bOctets = ( ( ASN1OctetString ) ASN1Primitive.fromByteArray( certificate.getExtensionValue( Extension.authorityInfoAccess.getId() ) ) ).getOctets();
		AuthorityInformationAccess access = AuthorityInformationAccess.getInstance( ASN1Primitive.fromByteArray( bOctets ) );
		for ( AccessDescription ad : access.getAccessDescriptions() )
			if ( ad.getAccessMethod().equals( X509ObjectIdentifiers.id_ad_caIssuers ) )
				return ad.getAccessLocation().getName().toString();
		return null;
	}

	public static TreeMap<String, Object> getWebKey( PublicKey publicKey )
	{
		TreeMap<String, Object> key = new TreeMap<>();
		if ( publicKey instanceof RSAPublicKey )
		{
			key.put( "kty", "RSA" );
			key.put( "e", base64UrlEncode( toIntegerBytes( ( ( RSAPublicKey ) publicKey ).getPublicExponent() ) ) );
			key.put( "n", base64UrlEncode( toIntegerBytes( ( ( RSAPublicKey ) publicKey ).getModulus() ) ) );
			return key;
		}
		else
			throw new IllegalArgumentException();
	}

	public static String getWebKeyThumbprintSHA256( PublicKey publicKey )
	{
		TreeMap<String, Object> webKey = getWebKey( publicKey );
		String webKeyJson = new Gson().toJson( webKey );
		return base64UrlEncode( SHA256( webKeyJson ) );
	}

	public static Gson gson()
	{
		if ( gson == null )
		{
			GsonBuilder builder = new GsonBuilder();
			gson = builder.create();
		}
		return gson;
	}

	public static HttpResponse post( final String method, final String target, final String accept, final String body, final String contentType ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException
	{
		String m = method == null ? "POST" : method.trim().toUpperCase();

		AcmePlugin.instance().getLogger().fine( m + " request made to URL [" + target + "], contentType [" + contentType + "]" );

		final URL u = new URL( target );
		final HttpURLConnection c = ( HttpURLConnection ) u.openConnection();

		if ( c instanceof HttpsURLConnection )
		{
			SSLContext context = SSLContext.getInstance( "TLSv1.2" );
			context.init( null, new TrustManager[] {AcmeTrustManager.INSTANCE}, null );
			( ( HttpsURLConnection ) c ).setSSLSocketFactory( context.getSocketFactory() );
		}

		c.setRequestMethod( m );
		c.setRequestProperty( "Accept", accept );
		if ( "POST".equals( m ) )
		{
			final byte[] bytes = body.getBytes( "UTF8" );
			c.setDoOutput( true );
			c.setRequestProperty( "Content-Type", contentType );
			c.setRequestProperty( "Content-Length", Integer.toString( bytes.length ) );
			c.setFixedLengthStreamingMode( bytes.length );
			try ( OutputStream s = c.getOutputStream() )
			{
				s.write( bytes, 0, bytes.length );
				s.flush();
			}
		}

		int status = c.getResponseCode();
		Map<String, List<String>> responseHeader = c.getHeaderFields();

		InputStream is = status < 400 ? c.getInputStream() : c.getErrorStream();
		return new HttpResponse( target, status, responseHeader, Unpooled.wrappedBuffer( UtilIO.inputStream2Bytes( is ) ) );
	}

	public static byte[] SHA256( String text )
	{
		try
		{
			MessageDigest md;
			md = MessageDigest.getInstance( "SHA-256" );
			md.update( text.getBytes( "UTF-8" ), 0, text.length() );
			return md.digest();
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException( e );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static byte[] toIntegerBytes( final BigInteger bigInt )
	{
		int bitlen = bigInt.bitLength();
		// round bitlen
		bitlen = bitlen + 7 >> 3 << 3;
		final byte[] bigBytes = bigInt.toByteArray();

		if ( bigInt.bitLength() % 8 != 0 && bigInt.bitLength() / 8 + 1 == bitlen / 8 )
			return bigBytes;
		// set up params for copying everything but sign bit
		int startSrc = 0;
		int len = bigBytes.length;

		// if bigInt is exactly byte-aligned, just skip signbit in copy
		if ( bigInt.bitLength() % 8 == 0 )
		{
			startSrc = 1;
			len--;
		}
		final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
		final byte[] resizedBytes = new byte[bitlen / 8];
		System.arraycopy( bigBytes, startSrc, resizedBytes, startDst, len );
		return resizedBytes;
	}
}
