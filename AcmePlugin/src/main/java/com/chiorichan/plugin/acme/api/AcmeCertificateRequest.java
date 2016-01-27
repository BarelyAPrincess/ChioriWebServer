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
import java.util.Collection;
import java.util.TreeMap;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.configuration.file.YamlConfiguration;
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
	private final Collection<String> domains;

	private int callBackTask = 0;
	private String certificateUrl;

	private final PKCS10CertificationRequest signingRequest;
	private X509Certificate certificate;

	protected AcmeCertificateRequest( AcmeProtocol proto, Collection<String> domains ) throws AcmeException, IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, StreamParsingException
	{
		this.proto = proto;
		this.domains = domains;

		// KeyPair domainKey = proto.getAcmeStorage().domainPrivateKey( domains );
		// TODO Use server private key if site does not have one of it's own
		// TODO Config option to force the generation of missing site private keys

		YamlConfiguration conf = AcmePlugin.INSTANCE.getConfig();
		KeyPair domainKey = proto.getAcmeStorage().domainPrivateKey();
		signingRequest = AcmeUtils.createCertificationRequest( domainKey, domains, conf.getString( "config.additional.country" ), conf.getString( "config.additional.state" ), conf.getString( "config.additional.city" ), conf.getString( "config.additional.organization" ) );
	}

	public X509Certificate certificate()
	{
		return certificate;
	}

	public void doCallback( boolean replace, Runnable runnable )
	{
		if ( hasCallBack() )
			if ( replace )
				TaskManager.INSTANCE.cancelTask( callBackTask );
			else
				throw new IllegalStateException( "Can't schedule a challenge callback because one is already active" );

		if ( state == AcmeState.CREATED )
			verify();

		try
		{
			AcmePlugin plugin = ( AcmePlugin ) PluginManager.INSTANCE.getPluginByClass( AcmePlugin.class );

			callBackTask = TaskManager.INSTANCE.scheduleAsyncRepeatingTask( plugin, Ticks.SECOND_5, Ticks.SECOND, new Runnable()
			{
				@Override
				public void run()
				{
					verify();

					if ( getState() != AcmeState.PENDING )
					{
						TaskManager.INSTANCE.cancelTask( callBackTask );
						runnable.run();
					}
				}
			} );

			if ( callBackTask == -1 )
			{
				callBackTask = 0;
				throw new IllegalStateException( "Failed to schedule the callback task" );
			}
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

	public Collection<String> getDomains()
	{
		return domains;
	}

	public AcmeState getState()
	{
		return state;
	}

	public String getUri()
	{
		return certificateUrl;
	}

	private boolean handleRequest( HttpResponse response ) throws AcmeException, StreamParsingException
	{
		proto.nonce( response.getHeaderString( "Replay-Nonce" ) );

		if ( response.getStatus() == HttpCode.HTTP_CREATED )
		{
			if ( response.getBody().length > 0 )
			{
				certificate = AcmeUtils.extractCertificate( response.getBody() );

				state = AcmeState.SUCCESS;
				lastMessage = "Certificate was successfully received and downloaded";

				return true;
			}
			else
			{
				state = AcmeState.PENDING;
				lastMessage = "The certificate is pending!";
			}
		}
		else if ( response.getStatus() == HttpCode.HTTP_TOO_MANY_REQUESTS )
		{
			state = AcmeState.FAILED;
			lastMessage = "Too many certificates already issued!";
		}
		else
		{
			response.debug();

			state = AcmeState.FAILED;
			lastMessage = "Failed to download certificate";
		}

		return false;
	}

	public boolean hasCallBack()
	{
		return callBackTask > 0;
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

		parentDir.mkdirs();
		proto.getAcmeStorage().saveCertificate( parentDir, certificate );
		proto.getAcmeStorage().saveCertificationRequest( parentDir, signingRequest );
	}

	public void saveCSR( File parentDir ) throws AcmeException
	{
		parentDir.mkdirs();
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
