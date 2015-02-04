package com.chiorichan.https;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.chiorichan.Loader;

public class SslContextFactory
{
	private static final String PROTOCOL = "TLS";
	private static final SSLContext SERVER_CONTEXT;
	private static final SSLContext CLIENT_CONTEXT;
	
	static
	{
		String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );
		if ( algorithm == null )
		{
			algorithm = "SunX509";
		}
		
		SSLContext serverContext;
		SSLContext clientContext;
		try
		{
			final File sslCert = new File( Loader.getRoot(), Loader.getConfig().getString( "server.httpsKeystone", "server.keystone" ) );
			// byte[] bytes = new byte[932];
			// IOUtils.readFully( new FileInputStream( sslCert ), bytes );
			// pkcs12Base64 = Base64Coder.encodeLines( bytes );
			
			KeyStore ks = KeyStore.getInstance( "JKS" );
			ks.load( new FileInputStream( sslCert ), Loader.getConfig().getString( "server.httpsSecret", "abcd1234" ).toCharArray() );
			
			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance( algorithm );
			kmf.init( ks, Loader.getConfig().getString( "server.httpsSecret", "abcd1234" ).toCharArray() );
			
			// Initialize the SSLContext to work with our key managers.
			serverContext = SSLContext.getInstance( PROTOCOL );
			serverContext.init( kmf.getKeyManagers(), null, null );
		}
		catch( Exception e )
		{
			throw new Error( "Failed to initialize the server-side SSLContext", e );
		}
		
		try
		{
			clientContext = SSLContext.getInstance( PROTOCOL );
			clientContext.init( null, TrustManagerFactory.getTrustManagers(), null );
		}
		catch( Exception e )
		{
			throw new Error( "Failed to initialize the client-side SSLContext", e );
		}
		
		SERVER_CONTEXT = serverContext;
		CLIENT_CONTEXT = clientContext;
	}
	
	public static SSLContext getServerContext()
	{
		return SERVER_CONTEXT;
	}
	
	public static SSLContext getClientContext()
	{
		return CLIENT_CONTEXT;
	}
	
	private SslContextFactory()
	{
		
	}
}
