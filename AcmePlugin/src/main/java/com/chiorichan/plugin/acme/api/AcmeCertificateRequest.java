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

import com.chiorichan.configuration.types.yaml.YamlConfiguration;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeState;
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
	private CertificateDownloader downloader = null;

	protected AcmeCertificateRequest( AcmeProtocol proto, Collection<String> domains, File keyFile ) throws AcmeException, IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, OperatorCreationException, StreamParsingException
	{
		this.proto = proto;
		this.domains = domains;

		// KeyPair domainKey = proto.getAcmeStorage().domainPrivateKey( domains );
		// TODO Use server private key if site does not have one of it's own
		// TODO Config option to force the generation of missing site private keys

		YamlConfiguration conf = AcmePlugin.instance().getConfig();
		KeyPair key = keyFile == null ? proto.getAcmeStorage().domainPrivateKey() : proto.getAcmeStorage().privateKey( keyFile, 4096 );
		signingRequest = AcmeUtils.createCertificationRequest( key, domains, conf.getString( "config.additional.country" ), conf.getString( "config.additional.state" ), conf.getString( "config.additional.city" ), conf.getString( "config.additional.organization" ) );
	}

	public X509Certificate certificate()
	{
		return downloader == null ? null : downloader.getCertificate();
	}

	public void doCallback( boolean replace, Runnable runnable )
	{
		if ( hasCallBack() )
			if ( replace )
				TaskManager.instance().cancelTask( callBackTask );
			else
				throw new IllegalStateException( "Can't schedule a challenge callback because one is already active" );

		if ( state == AcmeState.CREATED )
			verify();

		try
		{
			AcmePlugin plugin = ( AcmePlugin ) PluginManager.instance().getPluginByClass( AcmePlugin.class );

			callBackTask = TaskManager.instance().scheduleAsyncRepeatingTask( plugin, Ticks.SECOND_5, Ticks.SECOND, new Runnable()
			{
				@Override
				public void run()
				{
					verify();

					if ( getState() != AcmeState.PENDING )
					{
						TaskManager.instance().cancelTask( callBackTask );
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
			Log.get().severe( "There was a severe internal plugin error", e );
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

	public CertificateDownloader getDownloader()
	{
		return downloader;
	}

	public AcmeState getState()
	{
		return state;
	}

	public String getUri()
	{
		return certificateUrl;
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

	public void saveCSR( File parentDir ) throws AcmeException
	{
		parentDir.mkdirs();
		AcmePlugin.instance().getClient().getAcmeStorage().saveCertificationRequest( parentDir, signingRequest );
	}

	public void setState( AcmeState state )
	{
		this.state = state;
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

				HttpResponse response = AcmeUtils.post( "POST", proto.getUrlNewCert(), "application/json", body, "application/json" );
				proto.nonce( response.getHeaderString( "Replay-Nonce" ) );

				downloader = new CertificateDownloader( this, response );
				return downloader.isDownloaded();
			}
			else if ( state == AcmeState.PENDING )
			{
				downloader = new CertificateDownloader( this, certificateUrl );
				return downloader.isDownloaded();
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
