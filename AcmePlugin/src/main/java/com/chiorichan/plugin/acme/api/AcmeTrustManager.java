package com.chiorichan.plugin.acme.api;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AcmeTrustManager implements X509TrustManager
{
	static final TrustManager INSTANCE = new AcmeTrustManager();

	public static SSLContext getAcmeTrustManager() throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException
	{
		final TrustManager[] trustAllCerts = new TrustManager[] {INSTANCE};

		final KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );

		// kmf.init( keyStore, Acme.KS_PASS );

		final SSLContext sc = SSLContext.getInstance( "SSL" );

		sc.init( kmf.getKeyManagers(), trustAllCerts, new SecureRandom() );
		return sc;
	}

	private AcmeTrustManager()
	{

	}

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
}
