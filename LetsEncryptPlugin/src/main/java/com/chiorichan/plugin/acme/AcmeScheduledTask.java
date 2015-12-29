package com.chiorichan.plugin.acme;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import com.chiorichan.Loader;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.acme.api.AcmeCertificateRequest;
import com.chiorichan.plugin.acme.api.AcmeChallenge;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.api.SingleAcmeChallenge;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeForbiddenError;
import com.chiorichan.plugin.acme.lang.AcmeState;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.tasks.TaskManager;
import com.chiorichan.util.FileFunc;

public class AcmeScheduledTask implements Runnable
{
	private AcmePlugin plugin;

	public AcmeScheduledTask( AcmePlugin plugin )
	{
		this.plugin = plugin;
	}

	@Override
	public void run()
	{
		if ( !plugin.isEnabled() )
		{
			TaskManager.INSTANCE.cancelTask( this );

			throw new IllegalStateException( "The Acme Plugin is disabled, can't manage certificates until plugin is enabled!" );
		}

		if ( !NetworkManager.isHttpsRunning() )
		{
			PluginManager.INSTANCE.disablePlugin( plugin );
			TaskManager.INSTANCE.cancelTask( this );

			throw new IllegalStateException( "The HTTPS server is disabled, Acme Plugin can't manage certificates without it enabled." );
		}

		AcmeProtocol client = plugin.getClient();
		AcmeChallenge challenge = client.newChallenge();

		/*
		 * YamlConfiguration siteYaml = site.getConfig();
		 * boolean enabled;
		 *
		 * if ( siteYaml.get( "site.acme" ) == null )
		 * {
		 * siteYaml.set( "site.acme", false );
		 * enabled = false;
		 * }
		 * else
		 * enabled = siteYaml.getBoolean( "site.acme" );
		 *
		 * if ( enabled )
		 * {
		 *
		 * plugin.getLogger().info( String.format( "Acme Plugin is enabled for site '%s', site SSL configuration will be overridden.", site.getSiteId() ) );
		 */

		// We will start by verifying all domains and subdomains assigned to this server, this allows for quicker issuance later and is used for the server wide certificate.

		//for ( Site site : SiteManager.INSTANCE.getSites() )

		{
			Site site = SiteManager.INSTANCE.getSiteById( "penoaks" );
			if ( site != null && site != SiteManager.INSTANCE.getDefaultSite() )
				try
				{
					for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
					{
						challenge.add( e.getKey() );
						for ( String subdomain : e.getValue() )
							challenge.add( e.getKey(), subdomain );
					}
				}
				catch ( AcmeForbiddenError e1 )
				{
					try
					{
						if ( client.signAgreement( plugin.getRegistrationUrl(), plugin.getContacts() ) )
							for ( Entry<String, Set<String>> e : site.getDomains().entrySet() )
							{
								challenge.add( e.getKey() );
								for ( String subdomain : e.getValue() )
									challenge.add( e.getKey(), subdomain );
							}
						else
							throw new AcmeException( "Failed to Sign Agreement on Users Behalf" );
					}
					catch ( InvalidKeyException | KeyManagementException | UnrecoverableKeyException | SignatureException | NoSuchAlgorithmException | KeyStoreException | AcmeException ee )
					{
						ee.printStackTrace();
					}
				}
				catch ( InvalidKeyException | KeyManagementException | UnrecoverableKeyException | SignatureException | NoSuchAlgorithmException | KeyStoreException | AcmeException e )
				{
					e.printStackTrace();
				}
		}

		for ( SingleAcmeChallenge sac : challenge.getChallenges() )
			try
			{
				Site site = SiteManager.INSTANCE.getSiteByDomain( sac.getRootDomain() );

				if ( site == null )
					throw new IllegalStateException( "There was a problem with the site domain" );

				File acmeChallengeFile = new File( site.getSubdomain( sac.getSubDomain() ).directory(), FileFunc.buildPath( ".well-known", "acme-challenge", sac.getChallengeToken() ) );

				FileUtils.writeStringToFile( acmeChallengeFile, sac.getChallengeContent() );

				sac.doCallback( new Runnable()
				{
					@Override
					public void run()
					{
						plugin.getLogger().debug( "Domain Challenge: " + sac.lastMessage() );

						acmeChallengeFile.delete();

						if ( sac.getState() != AcmeState.SUCCESS )
						{
							plugin.getLogger().debug( "Domain Challenge Failed on domain " + sac.getFullDomain() + " for reason " + sac.lastMessage() );
							challenge.remove( sac );
						}
					}
				} );
			}
			catch ( IOException e1 )
			{
				e1.printStackTrace();
			}

		try
		{
			if ( challenge.challengesComplete() )
			{
				AcmeCertificateRequest signingRequest = client.newSigningRequest( new ArrayList<String>( challenge.getDomains() ) );
				signingRequest.doCallback( new Runnable()
				{
					@Override
					public void run()
					{
						if ( signingRequest.getState() == AcmeState.SUCCESS )
							try
							{
								signingRequest.save( Loader.getServerRoot() );
							}
							catch ( AcmeException e )
							{
								e.printStackTrace();
							}
						else
							plugin.getLogger().severe( "Failed Certificate Request for reason " + signingRequest.lastMessage() );
					}
				} );
			}
			else
				Loader.getLogger().debug( "Acme Certificate Failed" );
		}
		catch ( KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | OperatorCreationException | StreamParsingException | AcmeException | IOException e )
		{
			e.printStackTrace();
		}

	}
}
