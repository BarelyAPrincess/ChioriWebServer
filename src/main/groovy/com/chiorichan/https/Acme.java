/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.https;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import sun.security.pkcs10.PKCS10;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;

public final class Acme
{
	public interface ChallengeListener
	{
		boolean challengeSimpleHTTP( String domain, String token, String challengeURI, String content, boolean tls, SSLContext ctx ) throws Exception;
		
		boolean challengeDVSNI( String domain, String token, String challengeURI, String content );
		
		boolean challengeHTTP01( String domain, String token, String challengeURI, String content );
		
		boolean challengeTlsSni01( String domain, String token, String challengeURI, String content );
		
		void challengeFailed( String domain, String reason );
		
		void challengeCompleted( String domain );
	}
	
	static final class MyChallengeListener implements ChallengeListener, com.sun.net.httpserver.HttpHandler
	{
		private String token = "";
		private String httpChallenge = "";
		private com.sun.net.httpserver.HttpsServer httpsServer;
		
		@Override
		public void challengeCompleted( final String domain )
		{
			System.out.println( "challengeCompleted(" + domain + ")" );
			stop();
		}
		
		@Override
		public boolean challengeDVSNI( final String domain, final String token, final String challengeURI, final String content )
		{
			System.out.println( "challengeDVSNI(" + domain + " , " + token + " , " + challengeURI + " , " + content + ")" );
			return false;
			
		}
		
		@Override
		public void challengeFailed( final String domain, final String reason )
		{
			System.out.println( "challengeFailed(" + domain + " , " + reason + ")" );
			stop();
			
		}
		
		@Override
		public boolean challengeHTTP01( final String domain, final String token, final String challengeURI, final String content )
		{
			
			System.out.println( "challengeSimpleHTTP(" + domain + " , " + token + " , " + challengeURI + " , " + content + ")" );
			return false;
			
		}
		
		/**
		 * @return true = Challenge will be implemented * @throws IOException * @throws
		 *         NoSuchAlgorithmException * @throws KeyStoreException * @throws UnrecoverableKeyException * @throws KeyManagementException
		 */
		@Override
		public boolean challengeSimpleHTTP( final String domain, final String token, final String challengeURI, final String content, final boolean tls, final SSLContext ctx ) throws Exception
		{
			this.token = token;
			
			httpChallenge = content;
			startServer( ctx );
			System.out.println( "challengeSimpleHTTP(" + domain + " , " + token + " , " + challengeURI + " , " + content + " , " + tls + ")" );
			return true;
			
		}
		
		@Override
		public boolean challengeTlsSni01( final String domain, final String token, final String challengeURI, final String content )
		{
			
			System.out.println( "challengeSimpleHTTP(" + domain + " , " + token + " , " + challengeURI + " , " + content + ")" );
			return false;
			
		}
		
		@Override
		public void handle( final HttpExchange e ) throws IOException
		{
			//
			System.out.println( e.getRequestMethod() + " " + e.getRequestURI() );
			e.sendResponseHeaders( 200, httpChallenge.length() );
			e.getResponseHeaders().set( "Content-Type", "application/jose+json" );
			try (
				final OutputStream o = e.getResponseBody() )
			{
				o.write( httpChallenge.getBytes() );
				o.flush();
				
			}
			e.close();
			
		}
		
		private void startServer( final SSLContext sslCtx ) throws IOException
		{
			httpsServer = com.sun.net.httpserver.HttpsServer.create( new InetSocketAddress( 443 ), 0 );
			final com.sun.net.httpserver.HttpsConfigurator c = new com.sun.net.httpserver.HttpsConfigurator( sslCtx );
			final com.sun.net.httpserver.HttpsParameters p = new com.sun.net.httpserver.HttpsParameters()
			{
				
				{
					setCipherSuites( new String[] {"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"} );
					setProtocols( new String[] {"TLSv1.2"} );
					setCipherSuites( sslCtx.getDefaultSSLParameters().getCipherSuites() );
					
					setProtocols( sslCtx.getDefaultSSLParameters().getProtocols() );
					
				}
				
				@Override
				public InetSocketAddress getClientAddress()
				{
					return null;
					
				}
				
				@Override
				public HttpsConfigurator getHttpsConfigurator()
				{
					return c;
					
				}
				
				@Override
				public void setSSLParameters( final SSLParameters paramSSLParameters )
				{
					
				}
				
			};
			c.configure( p );
			
			httpsServer.setHttpsConfigurator( c );
			httpsServer.createContext( "/.well-known/acme-challenge/" + token, this );
			
			httpsServer.setExecutor( Executors.newCachedThreadPool() );
			httpsServer.start();
			
		}
		
		private void stop()
		{
			httpsServer.stop( 0 );
			
		}
		
	}
	
	private static final String AGREEMENT_KEY = "agreement";
	private static final String STATUS_KEY = "status";
	private static final String STATUS_PENDING = "pending";
	private static final String STATUS_VALID = "valid";
	private static final String CHALLENGE_TLS_KEY = "tls";
	private static final String TOKEN_KEY = "token";
	private static final String CHALLENGE_SIMPLE_HTTP = "simpleHttp";
	private static final String URI_KEY = "uri";
	private static final String CHALLENGES_KEY = "challenges";
	private static final String CONTACT_KEY = "contact";
	private static final String CSR_KEY = "csr";
	private static final String HEADER_REPLAY_NONCE = "replay-nonce";
	private static final String IDENTIFIER_KEY = "identifier";
	private static final String TYPE_DNS = "dns";
	private static final String TYPE_KEY = "type";
	private static final String VALUE_KEY = "value";
	private static final String NONCE_KEY = "nonce";
	private static final String RESOURCE_CHALLENGE = "challenge";
	private static final String RESOURCE_KEY = "resource";
	private static final int CREATED = 201;
	private static final int CONFLICT = 409;
	private static final int ACCEPTED = 202;
	private static final int FORBIDDEN = 403;
	private static final String SIG_ALG = "RS256";
	private static final String RESOURCE_UPDATE_REGISTRATION = "reg";
	private static final String APPLICATION_JSON = "application/json";
	private static final String JSON_WEB_KEY = "jwk";
	private static final String LOCATION = "Location";
	private static final Pattern p0 = Pattern.compile( "^\\{\"" + IDENTIFIER_KEY + "\":\\{\"" + TYPE_KEY + "\":\"(.*)\",\"" + VALUE_KEY + "\":\"(.*)\"},\"" + STATUS_KEY + "\":\"(.*)\",\"expires\":\"(.*)\",\"" + CHALLENGES_KEY + "\":\\[\\{(.*)}\\],\"combinations\":\\[\\[(.*)\\]\\]}$" );
	private static final Pattern p1 = Pattern.compile( "^\"" + TYPE_KEY + "\":\"(.*)\",\"" + STATUS_KEY + "\":\"(.*)\",\"" + URI_KEY + "\":\"(.*)\",\"" + TOKEN_KEY + "\":\"(.*)\",\"tls\":(.*)" );
	private static final Pattern p2 = Pattern.compile( "^\"" + TYPE_KEY + "\":\"(.*)\",\"" + STATUS_KEY + "\":\"(.*)\",\"" + URI_KEY + "\":\"(.*)\",\"" + TOKEN_KEY + "\":\"(.*)\"" );
	// private static final Pattern p3 =
	// Pattern.compile("\\{\""+TYPE_KEY+"\":\"(.*)\",\""+STATUS_KEY+"\":\"(.*)\",\"error\":\\{\""+TYPE_KEY+"\":\"(.*)\",\"detail\":\"(.*)\"},\"uri\":\"(.*)\",\""+TOKEN_KEY+"\":\"(.*)\",\"tls\":(.*),\"validationRecord\":\\[\\{\"url\":\"(.*)\",\"hostname\":\"(.*)\",\"port\":\"(.*)\",\"addressesResolved\":(.*),\"addressUsed\":\"(.*)\"}]}");
	private static final Pattern p3 = Pattern.compile( "\\{\"type\":\"(.*)\",\"status\":\"(.*)\",\"uri\":\"(.*)\",\"token\":\"(.*)\",\"tls\":true,\"validationRecord\":\\[\\{\"url\":\"(.*)\",\"hostname\":\"(.*)\",\"port\":\"(.*)\",\"addressesResolved\":\\[\"(.*)\"],\"addressUsed\":\"(.*)\"}]}" );
	private static final String USER_ALIAS = "letsencryptUserAlias";
	private static final Encoder BASE64_ENC = Base64.getUrlEncoder();
	private static final int KEY_SIZE = 4096;
	
	private static final char[] KS_PASS = getPassword();
	
	private final KeyPair uk;
	
	private String nextNonce = null;
	
	// ---
	private SSLContext sslCtx0;
	private int status;
	private byte[] resBody;
	
	// javac -Xlint -XDignore.symbol.file LetsEncrypt.java
	// java -cp . LetsEncrypt https://acme-v01.api.letsencrypt.org/directory lussnig@suche.org suche.org crypto.p12
	// java -cp . LetsEncrypt https://acme-v01.api.letsencrypt.org/directory lussnig@suche.org www.suche.org crypto.p12
	
	public final String urlNewAuthz;
	public final String urlNewCert;
	public final String urlNewReg;
	public final String urlRevokeCert;
	
	private final KeyStore keyStore;
	
	private final ChallengeListener challengeListener;
	private final boolean trustAllCertificate;
	
	private Map<String, List<String>> responseHeader = null;
	
	// }
	
	/**
	 * trustAllCertificate=false as default
	 * 
	 * @throws SignatureException
	 * @throws NoSuchProviderException
	 * @throws CertificateException
	 * @throws InvalidKeyException
	 */
	public Acme( final String dirUrl, final KeyStore keyStore, final ChallengeListener challengeListener, final boolean trustAllCertificate ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidKeyException, CertificateException, NoSuchProviderException, SignatureException
	{
		if ( challengeListener == null )
			throw new IllegalArgumentException( "ChallengeListener=null" );
		
		this.keyStore = keyStore;
		this.challengeListener = challengeListener;
		this.trustAllCertificate = trustAllCertificate;
		
		http( "GET", dirUrl, APPLICATION_JSON, null, null );
		final String json = new String( resBody );
		final Pattern p4 = Pattern.compile( "\\{\"new-authz\":\"(.*)\",\"new-cert\":\"(.*)\",\"new-reg\":\"(.*)\",\"revoke-cert\":\"(.*)\"}" );
		final Matcher m4 = p4.matcher( json );
		
		if ( !m4.matches() )
			throw new UnsupportedOperationException( new String( resBody ) );
		
		urlNewAuthz = m4.group( 1 );
		urlNewCert = m4.group( 2 );
		urlNewReg = m4.group( 3 );
		urlRevokeCert = m4.group( 4 );
		
		uk = getKeyPair( this.keyStore, USER_ALIAS, USER_ALIAS, Acme.KS_PASS );
	}
	
	private static StringBuilder arrToJsonString( final StringBuilder out, final String[] m )
	{
		out.append( "[" );
		boolean first = true;
		for ( final String e : m )
		{
			if ( first )
				first = false;
			else
				out.append( ", " );
			toJsonString( out, e );
		}
		return out.append( "]" );
	}
	
	// }
	private static String base64UrlEncode( final byte[] v )
	{
		return BASE64_ENC.encodeToString( v ).replaceAll( "=*$", "" );
	}
	
	private static sun.security.pkcs10.PKCS10 generateCSR( final String domain, final KeyPair domainKey ) throws CertificateException, SignatureException, IOException, NoSuchAlgorithmException, InvalidKeyException
	{
		final sun.security.pkcs10.PKCS10 pkcs10 = new sun.security.pkcs10.PKCS10( domainKey.getPublic() );
		final Signature localSignature = Signature.getInstance( "SHA256withRSA" );
		
		localSignature.initSign( domainKey.getPrivate() );
		pkcs10.encodeAndSign( new X500Name( "cn=" + domain ), localSignature );
		return pkcs10;
		
	}
	
	static KeyPair getKeyPair( final KeyStore keyStore, final String ali, final String cn, final char[] pass ) throws NoSuchAlgorithmException, InvalidKeyException, KeyStoreException, CertificateException, NoSuchProviderException, SignatureException, IOException, UnrecoverableKeyException
	{
		final RSAPrivateCrtKey k = ( RSAPrivateCrtKey ) keyStore.getKey( ali, pass );
		if ( k == null )
		{
			final KeyPairGenerator g = KeyPairGenerator.getInstance( "RSA" );
			g.initialize( KEY_SIZE );
			final KeyPair p = g.generateKeyPair();
			keyStore.setKeyEntry( ali, p.getPrivate(), pass, new Certificate[] {newSelfSigned( p, cn )} );
			try (
				OutputStream s = new FileOutputStream( new File( "le-keystore." + KeyStore.getDefaultType() ), false ) )
			{
				keyStore.store( s, pass );
				s.flush();
			}
			return p;
		}
		final PublicKey pub = keyStore.getCertificate( ali ).getPublicKey();
		return new KeyPair( pub, k );
	}
	
	private static char[] getPassword()
	{
		final Console c = System.console();
		if ( c != null )
			return c.readPassword( "1.Master Password: " );
		throw new UnsupportedOperationException();
	}
	
	public static Object getWebKey( final PublicKey publicKey )
	{
		final TreeMap key = new TreeMap<>();
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
	
	static KeyStore loadKeyStore( final String ksFile, final char[] pass ) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException
	{
		final File ksFile2 = new File( ksFile );
		final KeyStore ks = KeyStore.getInstance( "pkcs12" );
		if ( ksFile2.exists() )
			try (
				InputStream s = new FileInputStream( ksFile2 ) )
			{
				ks.load( s, pass );
			}
		else
			ks.load( null, null );
		return ks;
	}
	
	public static void main( final String[] argc ) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, InvalidKeySpecException, NoSuchProviderException, SignatureException
	{
		final String baseUrl = argc[0];
		
		final String mail = argc[1];
		final String domain = argc[2];
		// static String BASE_URL = "https://acme-v01.api.letsencrypt.org/directory";
		// final String BASE_URL = "https://acme-staging.api.letsencrypt.org/directory";
		final String ksFile = argc[3];
		final File ksFile2 = new File( ksFile );
		final KeyStore ks = loadKeyStore( ksFile, KS_PASS );
		
		try
		{
			// final String domain = "me.here.com";
			final MyChallengeListener h = new MyChallengeListener();
			final Acme acme = new Acme( baseUrl, ks, h, true );
			getKeyPair( ks, "le.rsa." + domain, domain, KS_PASS );
			String challengeUri = null;
			if ( challengeUri == null )
				challengeUri = acme.getChallengeURI( domain, "https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf", new String[] {"mailto:" + mail} );
			acme.getCertificate( domain, challengeUri );
			
		}
		catch ( final Throwable t )
		{
			
			t.printStackTrace( System.out );
			
		}
		finally
		{
			try (
				OutputStream s = new FileOutputStream( ksFile2 ) )
			{
				ks.store( s, KS_PASS );
				s.flush();
				
			}
			
		}
		System.exit( 0 );
		
	}
	
	private static StringBuilder mapToJsonString( final StringBuilder out, final Map<?, ?> m )
	{
		out.append( "{" );
		boolean first = true;
		
		for ( final Entry<?, ?> e : m.entrySet() )
		{
			if ( first )
				first = false;
			else
				out.append( ", " );
			out.append( '"' ).append( e.getKey() ).append( "\": " );
			toJsonString( out, e.getValue() );
			
		}
		
		return out.append( "}" );
	}
	
	static X509Certificate newSelfSigned( final KeyPair p, final String cn ) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
	{
		final X509CertInfo localX509CertInfo = new X509CertInfo();
		localX509CertInfo.set( "version", new CertificateVersion( 2 ) );
		localX509CertInfo.set( "serialNumber", new CertificateSerialNumber( new Random().nextInt() & 0x7FFFFFFF ) );
		localX509CertInfo.set( "algorithmID", new CertificateAlgorithmId( AlgorithmId.get( "SHA256withRSA" ) ) );
		localX509CertInfo.set( "subject", new X500Name( "cn=" + cn ) );
		localX509CertInfo.set( "key", new CertificateX509Key( p.getPublic() ) );
		localX509CertInfo.set( "validity", new CertificateValidity( new Date(), new Date( System.currentTimeMillis() + 365 * 24 * 3600000L ) ) );
		localX509CertInfo.set( "issuer", new X500Name( "cn=" + cn ) );
		final X509CertImpl localX509CertImpl = new X509CertImpl( localX509CertInfo );
		localX509CertImpl.sign( p.getPrivate(), "SHA256withRSA" );
		return localX509CertImpl;
	}
	
	private static String signWith( final String sig_alg, final PrivateKey privateKey, final Map header_, final Map protected_, final Map payload_ ) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException
	{
		
		String sigAlg;
		if ( "RS256".equals( sig_alg ) )
			sigAlg = "SHA256withRSA";
		else
			throw new IllegalArgumentException( "Unknown: " + sig_alg );
		header_.put( "alg", sig_alg );
		final String header = mapToJsonString( new StringBuilder(), header_ ).toString();
		final String prot = base64UrlEncode( mapToJsonString( new StringBuilder(), protected_ ).toString().getBytes() );
		
		final String payload = base64UrlEncode( mapToJsonString( new StringBuilder(), payload_ ).toString().getBytes() );
		final Signature sig = Signature.getInstance( sigAlg );
		
		sig.initSign( privateKey );
		sig.update( prot.getBytes() );
		sig.update( ".".getBytes() );
		sig.update( payload.getBytes() );
		final String signature = base64UrlEncode( sig.sign() );
		return "{\"header\": " + header + ", \"protected\": \"" + prot + "\", \"payload\": \"" + payload + "\", \"signature\": \"" + signature + "\"}";
	}
	
	// JSON-Builder
	private static StringBuilder strToJsonString( final StringBuilder out, final String v )
	{
		return out.append( '"' ).append( v ).append( '"' );
		
	}
	
	// toIntegerBytes := Copied from Apache Commons Codec 1.10
	private static byte[] toIntegerBytes( final BigInteger bigInt )
	{
		int bitlen = bigInt.bitLength();
		// round bitlen bitlen = ((bitlen + 7) >>3) << 3;
		final byte[] bigBytes = bigInt.toByteArray();
		if ( ( ( bigInt.bitLength() % 8 ) != 0 ) && ( ( ( bigInt.bitLength() / 8 ) + 1 ) == ( bitlen / 8 ) ) )
			return bigBytes;
		
		// set up params for copying everything but sign bit
		int startSrc = 0;
		int len = bigBytes.length;
		// if bigInt is exactly byte-aligned, just skip signbit in copy if ((bigInt.bitLength() % 8) == 0) { startSrc = 1; len--; }
		
		final int startDst = bitlen / 8 - len;
		// to pad w/ nulls as per spec
		final byte[] resizedBytes = new byte[bitlen / 8];
		System.arraycopy( bigBytes, startSrc, resizedBytes, startDst, len );
		return resizedBytes;
	}
	
	@SuppressWarnings( "unchecked" )
	private static StringBuilder toJsonString( final StringBuilder out, final Object v )
	{
		if ( v == null )
			return out.append( "null" );
		if ( v instanceof String )
			return strToJsonString( out, ( String ) v );
		if ( v instanceof String[] )
			return arrToJsonString( out, ( String[] ) v );
		if ( v instanceof Map )
			return mapToJsonString( out, ( Map ) v );
		if ( v instanceof Boolean )
			return out.append( ( ( Boolean ) v ).toString() );
		throw new IllegalArgumentException( "Typ: " + v.getClass().getCanonicalName() );
		
	}
	
	// JSON-Builder END
	private X509Certificate extractCertificate( final String domain, final InputStream inputStream ) throws Exception
	{
		final CertificateFactory cf = CertificateFactory.getInstance( "X.509" );
		final X509Certificate certificate = ( X509Certificate ) cf.generateCertificate( inputStream );
		final Key key = keyStore.getKey( domain, Acme.KS_PASS );
		
		keyStore.setKeyEntry( domain, key, Acme.KS_PASS, new Certificate[] {certificate} );
		
		try (
			OutputStream s = new FileOutputStream( new File( "le-keystore." + KeyStore.getDefaultType() ), false ) )
		{
			keyStore.store( s, Acme.KS_PASS );
			s.flush();
		}
		
		return certificate;
	}
	
	private String getAuthorizationRequest( final String nextNonce, final String domain ) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, CertificateException, NoSuchProviderException, IOException
	{
		final Map protect = new TreeMap<>();
		final Map header = new TreeMap<>();
		final Map payload = new TreeMap<>();
		header.put( JSON_WEB_KEY, getWebKey( uk.getPublic() ) );
		protect.put( NONCE_KEY, nextNonce );
		payload.put( RESOURCE_KEY, "new-authz" );
		final Map identifier = new TreeMap<>();
		
		identifier.put( TYPE_KEY, TYPE_DNS );
		identifier.put( VALUE_KEY, domain );
		payload.put( IDENTIFIER_KEY, identifier );
		return signWith( SIG_ALG, uk.getPrivate(), header, protect, payload );
		
	}
	
	public X509Certificate getCertificate( final String domain, final String challengeURI ) throws Exception
	{
		if ( nextNonce == null )
			getInitialNonce();
		
		// Step 1: Get initial Nonce
		verifyChallenge( challengeURI );
		
		// Step 4: Ask CA to verify challenge
		waitForVerification( challengeURI, domain );
		
		// Step 5: Waiting for challenge verification
		final PKCS10 csr = generateCSR( domain, getKeyPair( keyStore, "le.rsa." + domain, domain, Acme.KS_PASS ) );
		
		// Step 6: Generate CSR0
		return getCertificate( domain, csr );
		
		// Step 7: Ask for new certificate
	}
	
	/** Step 7: Ask for new certificate. Return URL or X509Certificate */
	private X509Certificate getCertificate( final String domain, final sun.security.pkcs10.PKCS10 csr ) throws Exception
	{
		final Map protect = new TreeMap<>();
		final Map header = new TreeMap<>();
		final Map payload = new TreeMap<>();
		header.put( JSON_WEB_KEY, getWebKey( uk.getPublic() ) );
		
		protect.put( NONCE_KEY, nextNonce );
		payload.put( RESOURCE_KEY, "new-cert" );
		payload.put( CSR_KEY, base64UrlEncode( csr.getEncoded() ) );
		final String body = signWith( SIG_ALG, uk.getPrivate(), header, protect, payload );
		http( "POST", urlNewCert, APPLICATION_JSON, body, APPLICATION_JSON );
		if ( status == CREATED )
		{
			if ( getLength() > 0 )
				return extractCertificate( domain, getEntity() );
			final String certificateURL = getHeaderString( LOCATION );
			// Step 8: Fetch new certificate (if not already returned)
			int downloadRetryCount = 12;
			while ( downloadRetryCount-- > 0 )
			{
				Thread.sleep( 10000L );
				http( "GET", certificateURL, "*/*", null, null );
				if ( status == CREATED )
				{
					if ( getLength() > 0 )
						return extractCertificate( domain, getEntity() );
					
				}
				else
					throw new Exception( "Failed to download certificate. " + this );
				
			}
			throw new Exception( "Failed to download certificate. Timeout." );
			
		}
		else
			throw new Exception( "Failed to download certificate. " + this );
		
	}
	
	/** Step 3: Ask CA a challenge for our domain */
	private String getChallengeUri( final String agreement, final String[] contacts, final String domain, final String registrationURI ) throws Exception
	{
		http( "POST", urlNewAuthz, APPLICATION_JSON, getAuthorizationRequest( nextNonce, domain ), APPLICATION_JSON );
		
		String challengeURI = null;
		nextNonce = getHeaderString( HEADER_REPLAY_NONCE );
		if ( status == FORBIDDEN )
		{
			if ( agreement != null )
			{
				// Step 3b: sign new agreement
				final Map protect = new TreeMap<>();
				final Map header = new TreeMap<>();
				final Map payload = new TreeMap<>();
				header.put( JSON_WEB_KEY, getWebKey( uk.getPublic() ) );
				
				protect.put( NONCE_KEY, nextNonce );
				payload.put( RESOURCE_KEY, RESOURCE_UPDATE_REGISTRATION );
				if ( contacts != null && contacts.length > 0 )
					payload.put( CONTACT_KEY, contacts );
				final String body = signWith( SIG_ALG, uk.getPrivate(), header, protect, payload );
				http( "POST", registrationURI, APPLICATION_JSON, body, APPLICATION_JSON );
				nextNonce = getHeaderString( HEADER_REPLAY_NONCE );
				if ( status != ACCEPTED )
					throw new Exception( "Registration failed. " + this );
				// Step 3c: Ask CA a challenge for our domain after agreement update
				http( "POST", urlNewAuthz, APPLICATION_JSON, getAuthorizationRequest( nextNonce, domain ), APPLICATION_JSON );
				nextNonce = getHeaderString( HEADER_REPLAY_NONCE );
				if ( status != CREATED )
					throw new Exception( "Client unautorized. May need to accept new terms. " + this );
			}
			else
				throw new Exception( "Client unautorized. May need to accept new terms. " + this );
		}
		else if ( status != CREATED )
			throw new Exception( "Failed to create new authorization request. " + this );
		final String json = new String( resBody );
		final Matcher m = p0.matcher( json );
		if ( !m.matches() )
			throw new IllegalArgumentException( json );
		// final String type = m.group(1);
		
		// final String value = m.group(2);
		
		// final String status = m.group(3);
		
		// final String expires = m.group(4);
		
		final String[] challenges = m.group( 5 ).split( "},\\{" );
		
		// "type":"simpleHttp","status":"pending","uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/M397oZJhPcvU4VCpt5eIOWI5gIa5V0PhLZ_aR0PPKok/412748","token":"wLPIyGHhvVawt_6Z1L3m8zvhKOPN5Qk-tki0rxU_zwQ","tls":true},{"type":"dvsni","status":"pending","uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/M397oZJhPcvU4VCpt5eIOWI5gIa5V0PhLZ_aR0PPKok/412749","token":"tKzQHPQlw97uCbP1gY0inZMIl8CKfhxLpGYA21y7W4w"},{"type":"http-01","status":"pending","uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/M397oZJhPcvU4VCpt5eIOWI5gIa5V0PhLZ_aR0PPKok/412750","token":"e_flaSvqs4psKgwSRNheDKOvNLU8R6FIntDqeBfXByc"},{"type":"tls-sni-01","status":"pending","uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/M397oZJhPcvU4VCpt5eIOWI5gIa5V0PhLZ_aR0PPKok/412751","token":"n1V3EvE0VhYX-J5R30Mh17kzxP60x9SFZ-lW6kQuCrU"
		// final String[] combinations = m.group(6).split("],\\[");
		
		// 0],[1],[2],[3
		for ( final String c : challenges )
		{
			
			final Matcher m1 = p1.matcher( c );
			
			if ( m1.matches() )
			{
				
				final String cType = m1.group( 1 );
				final String cStatus = m1.group( 2 );
				if ( !"pending".equals( cStatus ) )
					continue;
				final String cUri = m1.group( 3 );
				final String cToken = m1.group( 4 );
				final String cTls = m1.group( 5 );
				System.out.println( cType + " , " + cStatus + " , " + cUri + " , " + cToken + " , " + cTls );
				if ( handleChallenge( domain, cType, cToken, cUri, Boolean.valueOf( cTls ) ) )
				{
					challengeURI = cUri;
					break;
				}
				continue;
				
			}
			final Matcher m2 = p2.matcher( c );
			if ( m2.matches() )
			{
				final String cType = m2.group( 1 );
				final String cStatus = m2.group( 2 );
				if ( !"pending".equals( cStatus ) )
					continue;
				final String cUri = m2.group( 3 );
				final String cToken = m2.group( 4 );
				
				System.out.println( cType + " , " + cStatus + " , " + cUri + " , " + cToken );
				if ( handleChallenge( domain, cType, cToken, cUri, false ) )
				{
					challengeURI = cUri;
					break;
					
				}
				continue;
				
			}
			
		}
		if ( challengeURI == null )
			throw new Exception( "No challenge completed." );
		return challengeURI;
	}
	
	
	public String getChallengeURI( final String domain, final String agreement, final String[] contacts ) throws Exception
	{
		getKeyPair( keyStore, "le.rsa." + domain, domain, Acme.KS_PASS );
		if ( nextNonce == null )
			getInitialNonce();
		// Step 1: Get initial Nonce
		
		final String registrationURI = getRegistrationURI( agreement, contacts );
		// Step 2: Register a new account with CA
		
		return getChallengeUri( agreement, contacts, domain, registrationURI );
		// Step 3: Ask CA a challenge for our domain
	}
	
	private InputStream getEntity()
	{
		return new ByteArrayInputStream( resBody );
		
	}
	
	private String getHeaderString( final String key )
	{
		for ( final Map.Entry<String, List<String>> e : responseHeader.entrySet() )
			if ( key.equalsIgnoreCase( e.getKey() ) )
				return e.getValue() == null || e.getValue().size() == 0 ? null : e.getValue().get( 0 );
		return null;
		
	}
	
	/** Step 1: Get initial Nonce */
	private void getInitialNonce() throws Exception
	{
		http( "HEAD", urlNewReg, APPLICATION_JSON, null, null );
		nextNonce = getHeaderString( HEADER_REPLAY_NONCE );
		if ( nextNonce == null )
			throw new NullPointerException( "nonce=null " + responseHeader );
	}
	
	private int getLength()
	{
		return resBody.length;
		
	}
	
	/** Step 2: Register a new account with CA */
	private String getRegistrationURI( final String agreement, final String[] contacts ) throws Exception
	{
		if ( nextNonce == null )
			throw new NullPointerException( "nonce=null" );
		if ( nextNonce == null )
			throw new NullPointerException( "nonce=null" );
		final Map protect = new TreeMap<>();
		final Map header = new TreeMap<>();
		final Map payload = new TreeMap<>();
		header.put( JSON_WEB_KEY, getWebKey( uk.getPublic() ) );
		protect.put( NONCE_KEY, nextNonce );
		payload.put( RESOURCE_KEY, "new-reg" );
		if ( contacts != null && contacts.length > 0 )
			payload.put( CONTACT_KEY, contacts );
		if ( agreement != null )
			payload.put( AGREEMENT_KEY, agreement );
		final String body = signWith( SIG_ALG, uk.getPrivate(), header, protect, payload );
		http( "POST", urlNewReg, APPLICATION_JSON, body, APPLICATION_JSON );
		nextNonce = getHeaderString( HEADER_REPLAY_NONCE );
		if ( status != CREATED && status != CONFLICT )
			throw new Exception( "Registration failed. " + this );
		return getHeaderString( LOCATION );
	}
	
	private SSLContext getTrustAllCertificateSSLContext() throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException
	{
		final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager()
		{
			@Override
			public void checkClientTrusted( final X509Certificate[] certs, final String authType )
			{
			}
			
			@Override
			public void checkServerTrusted( final X509Certificate[] certs, final String authType )
			{
			}
			
			@Override
			public X509Certificate[] getAcceptedIssuers()
			{
				return new X509Certificate[0];
			}
			
		}};
		
		final KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
		kmf.init( keyStore, Acme.KS_PASS );
		final SSLContext sc = SSLContext.getInstance( "SSL" );
		
		sc.init( kmf.getKeyManagers(), trustAllCerts, new SecureRandom() );
		return sc;
		
	}
	
	private boolean handleChallenge( final String domain, final String challengeType, final String token, final String challengeURI, final boolean tls ) throws KeyManagementException, UnrecoverableKeyException, KeyStoreException, Exception
	{
		switch ( challengeType )
		{
			case CHALLENGE_SIMPLE_HTTP:
				final Map protect = new TreeMap<>();
				final Map header = new TreeMap<>();
				final Map payload = new TreeMap<>();
				payload.put( TYPE_KEY, CHALLENGE_SIMPLE_HTTP );
				payload.put( TOKEN_KEY, token );
				payload.put( CHALLENGE_TLS_KEY, true );
				final String body = signWith( SIG_ALG, uk.getPrivate(), header, protect, payload );
				return challengeListener.challengeSimpleHTTP( domain, token, challengeURI, body, tls, sslCtx() );
			case "dvsni":
				return challengeListener.challengeDVSNI( domain, token, challengeURI, "TODO" );
			case "http-01":
				return challengeListener.challengeHTTP01( domain, token, challengeURI, "TODO" );
			case "tls-sni-01":
				return challengeListener.challengeTlsSni01( domain, token, challengeURI, "TODO" );
			default:
				System.out.println( "Unsupported Challenge: " + challengeType );
				return false;
		}
	}
	
	// static final Proxy PROXY = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("HE100001.emea1.cds.t-internal.com", 8080));
	
	private void http( final String method, final String target, final String accept, final String body, final String contentType ) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, IOException
	{
		try
		{
			final URL u = new URL( target );
			final HttpURLConnection c = ( HttpURLConnection ) u.openConnection();
			// final HttpURLConnection c = ( HttpURLConnection ) u.openConnection( PROXY );
			if ( c instanceof HttpsURLConnection )
				( ( HttpsURLConnection ) c ).setSSLSocketFactory( sslCtx().getSocketFactory() );
			c.setRequestMethod( method );
			c.setRequestProperty( "Accept", accept );
			if ( "POST".equals( method ) )
			{
				
				final byte[] bytes = body.getBytes();
				c.setDoOutput( true );
				c.setRequestProperty( "Content-Type", contentType );
				c.setRequestProperty( "Content-Length", Integer.toString( bytes.length ) );
				c.setFixedLengthStreamingMode( bytes.length );
				try (
					OutputStream s = c.getOutputStream() )
				{
					s.write( bytes, 0, bytes.length );
					s.flush();
					
				}
				
			}
			
			status = c.getResponseCode();
			responseHeader = c.getHeaderFields();
			try (
				InputStream s = status < 400 ? c.getInputStream() : c.getErrorStream() )
			{
				if ( s == null )
					resBody = new byte[0];
				else
				{
					resBody = new byte[c.getContentLength()];
					s.read( resBody );
					
				}
				
			}
			
			System.out.println( method + " " + target + " HTTP-" + status + " " + c.getResponseMessage() + " " + new String( resBody ) );
			
		}
		catch ( final Throwable t )
		{
			final IOException i = new IOException( method + " " + target + " =>" + t );
			i.setStackTrace( t.getStackTrace() );
			throw i;
			
		}
		
	}
	
	private SSLContext sslCtx() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException
	{
		if ( sslCtx0 == null )
			sslCtx0 = ( trustAllCertificate ) ? getTrustAllCertificateSSLContext() : SSLContext.getInstance( "TLSv1.2" );
		return sslCtx0;
		
	}
	
	/** Step 4: Ask CA to verify challenge */
	private void verifyChallenge( final String challengeURI ) throws Exception
	{
		final Map protect = new TreeMap<>();
		final Map header = new TreeMap<>();
		final Map payload = new TreeMap<>();
		header.put( JSON_WEB_KEY, getWebKey( uk.getPublic() ) );
		protect.put( NONCE_KEY, nextNonce );
		payload.put( RESOURCE_KEY, RESOURCE_CHALLENGE );
		payload.put( TYPE_KEY, CHALLENGE_SIMPLE_HTTP );
		
		payload.put( CHALLENGE_TLS_KEY, true );
		final String body = signWith( SIG_ALG, uk.getPrivate(), header, protect, payload );
		http( "POST", challengeURI, APPLICATION_JSON, body, APPLICATION_JSON );
		nextNonce = getHeaderString( HEADER_REPLAY_NONCE );
		if ( status != ACCEPTED )
			throw new Exception( "Failed to post challenge. " + this );
		
	}
	
	/** Step 5: Waiting for challenge verification * @throws Exception */
	private void waitForVerification( final String challengeURI, final String domain ) throws Exception
	{
		int validateChallengeRetryCount = 12;
		while ( --validateChallengeRetryCount > 0 )
		{
			Thread.sleep( 10000L );
			http( "GET", challengeURI, APPLICATION_JSON, null, null );
			if ( status == ACCEPTED )
			{
				final String json = new String( resBody );
				final Matcher m3 = p3.matcher( json );
				if ( !m3.matches() )
					throw new UnsupportedOperationException( "JSON: " + new String( resBody ) );
				
				// final String type = m3.group(1);
				
				final String status = m3.group( 2 );
				
				final String eType = m3.group( 3 );
				
				final String eDetail = m3.group( 4 );
				
				if ( status.equals( STATUS_VALID ) )
					validateChallengeRetryCount = -1;
				else if ( !status.equals( STATUS_PENDING ) )
				{
					challengeListener.challengeFailed( domain, eType + " : " + eDetail );
					throw new Exception( "Failed verify challenge. Status: " + status + this );
					
				}
				
			}
			else
			{
				challengeListener.challengeFailed( domain, "HTTP-" + status );
				
				throw new Exception( "Failed verify challenge." + this );
				
			}
			
		}
		if ( validateChallengeRetryCount == 0 )
		{
			challengeListener.challengeFailed( domain, "Failed verify challenge. Timeout." );
			throw new Exception( "Failed verify challenge. Timeout." );
			
		}
		challengeListener.challengeCompleted( domain );
		
	}
}
