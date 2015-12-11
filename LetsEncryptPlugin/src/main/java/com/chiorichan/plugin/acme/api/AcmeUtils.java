package com.chiorichan.plugin.acme.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AcmeUtils
{
	private static final Encoder BASE64_ENC = Base64.getUrlEncoder();

	private static Gson gson = null;

	private static String base64UrlEncode( final byte[] v )
	{
		return BASE64_ENC.encodeToString( v ).replaceAll( "=*$", "" );
	}

	public static HttpResponse get( final String target, final String accept ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException
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

			byte[] resBody;
			try ( InputStream s = status < 400 ? c.getInputStream() : c.getErrorStream() )
			{
				if ( s == null )
					resBody = new byte[0];
				else
				{
					resBody = new byte[c.getContentLength()];
					s.read( resBody );
				}
			}

			return new HttpResponse( status, responseHeader, resBody );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
			return null;
		}
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

			c.setRequestMethod( method );
			c.setRequestProperty( "Accept", accept );
			if ( "POST".equals( method ) )
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

			byte[] resBody;
			try ( InputStream s = status < 400 ? c.getInputStream() : c.getErrorStream() )
			{
				if ( s == null )
					resBody = new byte[0];
				else
				{
					resBody = new byte[c.getContentLength()];
					s.read( resBody );
				}
			}

			return new HttpResponse( status, responseHeader, resBody );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
			return null;
		}
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
