package com.chiorichan.plugin.acme.api;

import io.jsonwebtoken.impl.TextCodec;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.TreeMap;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.http.HttpCode;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.plugin.lang.PluginNotFoundException;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.tasks.Ticks;

@SuppressWarnings( "serial" )
public class AcmeCertificateRequest
{
	private AcmeState state = AcmeState.CREATED;
	private String lastMessage = null;

	private final AcmeProtocol proto;
	private final List<String> domains;

	private String certificateUrl;

	private final PKCS10CertificationRequest signingRequest;
	private X509Certificate certificate;

	protected AcmeCertificateRequest( AcmeProtocol proto, List<String> domains ) throws AcmeException, IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, StreamParsingException
	{
		this.proto = proto;
		this.domains = domains;

		KeyPair domainKey = proto.getAcmeStorage().domainPrivateKey( domains );
		signingRequest = AcmeUtils.createCertificationRequest( domainKey, domains );
	}

	public X509Certificate certificate()
	{
		return certificate;
	}

	public void doCallback( Runnable runnable )
	{
		if ( state == AcmeState.CREATED )
			verify();

		try
		{
			AcmePlugin plugin = ( AcmePlugin ) PluginManager.INSTANCE.getPluginByClass( AcmePlugin.class );

			TaskManager.INSTANCE.scheduleAsyncRepeatingTask( plugin, Ticks.SECOND_5, Ticks.SECOND, new Runnable()
			{
				@Override
				public void run()
				{
					verify();

					if ( getState() != AcmeState.PENDING )
					{
						TaskManager.INSTANCE.cancelTask( this );
						runnable.run();
					}
				}
			} );
		}
		catch ( PluginNotFoundException e )
		{
			Loader.getLogger().severe( "There was a severe internal plugin error", e );
		}
	}

	public PKCS10CertificationRequest getCertificationRequest()
	{
		return signingRequest;
	}

	public List<String> getDomains()
	{
		return domains;
	}

	public AcmeState getState()
	{
		return state;
	}

	private boolean handleRequest( HttpResponse response ) throws AcmeException, StreamParsingException
	{
		response.debug();

		proto.nonce( response.getHeaderString( "Replay-Nonce" ) );

		if ( response.getStatus() == HttpCode.HTTP_CREATED )
		{
			if ( response.getBody().length > 0 )
			{
				certificate = AcmeUtils.extractCertificate( response.getBody() );

				state = AcmeState.SUCCESS;
				lastMessage = "Certificate was successfully received.";

				return true;
			}
			else
			{
				state = AcmeState.PENDING;
				lastMessage = "There was an unknown cause.";
			}
		}
		else if ( response.getStatus() == HttpCode.HTTP_TOO_MANY_REQUESTS )
		{
			state = AcmeState.PENDING;
			lastMessage = "You are rate limited.";
		}
		else
		{
			state = AcmeState.FAILED;
			lastMessage = "Failed to download certificate.";
		}

		return false;
	}

	public boolean hasFailed()
	{
		return state == AcmeState.INVALID || state == AcmeState.FAILED;
	}

	public String lastMessage()
	{
		if ( lastMessage == null )
			lastMessage = "No Message Available";
		return lastMessage;
	}

	public void save() throws AcmeException
	{
		if ( state != AcmeState.SUCCESS )
			throw new IllegalStateException( "Can't save until Certificate Request was successful!" );

		proto.getAcmeStorage().saveCertificate( domains, certificate );
		proto.getAcmeStorage().saveCertificationRequest( domains, signingRequest );
	}

	public void save( File parentDir ) throws AcmeException
	{
		if ( state != AcmeState.SUCCESS )
			throw new IllegalStateException( "Can't save until Certificate Request was successful!" );

		proto.getAcmeStorage().saveCertificate( parentDir, certificate );
		proto.getAcmeStorage().saveCertificationRequest( parentDir, signingRequest );
	}

	public boolean verify()
	{
		try
		{
			return verifyWithException();
		}
		catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | AcmeException | StreamParsingException | IOException e )
		{
			return false;
		}
	}

	public boolean verifyWithException() throws AcmeException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, StreamParsingException, IOException
	{
		try
		{
			if ( state == AcmeState.SUCCESS )
				return true;
			else if ( hasFailed() )
				return false;
			else if ( state == AcmeState.CREATED )
			{
				String body = proto.newJwt( new TreeMap<String, Object>()
				{
					{
						put( "resource", "new-cert" );
						put( "csr", TextCodec.BASE64URL.encode( signingRequest.getEncoded() ) );
					}
				} );

				HttpResponse response = AcmeUtils.post( "POST", proto.urlNewCert, "application/json", body, "application/json" );
				certificateUrl = response.getHeaderString( "Location" );

				return handleRequest( response );
			}
			else if ( state == AcmeState.PENDING )
			{
				HttpResponse response = AcmeUtils.get( certificateUrl, "application/json" );
				return handleRequest( response );
			}
		}
		catch ( AcmeException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e )
		{
			lastMessage = e.getMessage();
			state = AcmeState.FAILED;
			throw e;
		}

		return false;
	}
}
