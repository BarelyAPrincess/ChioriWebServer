package com.chiorichan.plugin.acme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import com.chiorichan.Loader;
import com.chiorichan.configuration.file.FileConfiguration;
import com.chiorichan.net.NetworkManager;
import com.chiorichan.plugin.acme.api.AcmeChallenge;
import com.chiorichan.plugin.acme.api.AcmeProtocol;
import com.chiorichan.plugin.acme.api.SingleAcmeChallenge;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeForbiddenError;
import com.chiorichan.plugin.lang.PluginException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;

public class AcmePlugin extends Plugin
{
	protected static final String URL_TESTING = "https://acme-staging.api.letsencrypt.org/directory";
	protected static final String URL_PRODUCTION = "https://acme-v01.api.letsencrypt.org/directory";

	private AcmeProtocol client;
	private File privateKey;
	private KeyPair keyPair;

	private String CertAuthority;

	protected KeyPair generatePrivateKey() throws NoSuchAlgorithmException, IOException
	{
		Validate.notNull( privateKey );

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "RSA" );
		keyGen.initialize( 4096 );
		KeyPair keyPair = keyGen.generateKeyPair();

		OutputStream stream = new FileOutputStream( privateKey );

		try ( JcaPEMWriter writer = new JcaPEMWriter( new PrintWriter( stream ) ) )
		{
			writer.writeObject( keyPair );
			writer.close();
		}
		finally
		{
			stream.close();
		}

		return keyPair;
	}

	private KeyPair loadPrivateKey() throws IOException
	{
		InputStream stream = new FileInputStream( privateKey );

		try ( PEMParser pemParser = new PEMParser( new InputStreamReader( stream ) ) )
		{
			PEMKeyPair keyPair = ( PEMKeyPair ) pemParser.readObject();
			return new JcaPEMKeyConverter().getKeyPair( keyPair );
		}
		finally
		{
			stream.close();
		}
	}

	@Override
	public void onDisable() throws PluginException
	{

	}

	@Override
	public void onEnable() throws PluginException
	{
		if ( !getConfigFile().exists() )
			saveDefaultConfig();

		FileConfiguration yaml = getConfig();

		if ( !NetworkManager.isHttpsRunning() )
			throw new PluginException( getName() + " requires HTTPS to be enabled and running, see documentation to enable. The server will generate a temporary self signed certificate if none exists." );

		if ( !"letsencrypt".equals( yaml.getString( "config.ca" ) ) )
			throw new PluginException( getName() + " currently only supports the Let's Encrypt Certificate Authory but config option is set to '" + yaml.getString( "config.ca" ) + "'" );

		if ( !yaml.getBoolean( "config.accept-agreement" ) )
			throw new PluginException( "Let's Encrypt requires you to accept their agreement before they will issue certificates. Read 'https://letsencrypt.org/documents/LE-SA-v1.0.1-July-27-2015.pdf', then change config value 'config.accept-agreement' to true." );

		if ( yaml.get( "config.email" ) == null || yaml.getString( "config.email" ).length() == 0 )
			throw new PluginException( "Let's Encrypt requires a valid e-mail address to issue certificates, see config value 'config.email'." );

		if ( !yaml.getBoolean( "config.production" ) )
			getLogger().warning( getName() + " is running in testing-mode, the issued certificates will have no real-world value, see config value 'config.production'." );

		File data = getDataFolder();
		FileFunc.patchDirectory( data );

		privateKey = new File( data, "private.key" );

		try
		{
			if ( privateKey.exists() )
				keyPair = loadPrivateKey();
			else
				keyPair = generatePrivateKey();
		}
		catch ( NoSuchAlgorithmException | IOException e )
		{
			throw new PluginException( "We failed to generate a new private key" );
		}

		client = new AcmeProtocol( yaml.getBoolean( "config.production", false ) ? URL_PRODUCTION : URL_TESTING, yaml.getString( "config.agreement" ), keyPair );

		if ( yaml.getBoolean( "enabled.server" ) )
		{

		}

		String[] contacts = new String[] {"mailto:chiorigreene@gmail.com"};

		Site site = SiteManager.INSTANCE.getSiteById( "penoaks" );

		try
		{
			String registrationUrl = client.newRegistration();
			AcmeChallenge challenge = client.newChallenge();

			try
			{
				challenge.add( site.getDomain() );
			}
			catch ( AcmeForbiddenError e )
			{
				if ( client.signAgreement( registrationUrl, contacts ) )
					challenge.add( site.getDomain() );
				else
					throw new AcmeException( "Failed New Challenge" );
			}

			for ( SingleAcmeChallenge sac : challenge.getChallenges() )
				if ( sac.getDomain().equals( site.getDomain() ) )
				{
					// TODO Delete Acme Challenge URL

					File acmeChallengeFile = new File( site.publicDirectory(), FileFunc.buildPath( ".well-known", "acme-challenge", sac.getChallengeToken() ) );

					FileUtils.writeStringToFile( acmeChallengeFile, sac.getChallengeContent() );

					sac.verify();

					try
					{
						Thread.sleep( 1000L );
					}
					catch ( InterruptedException e )
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					sac.verify();

					getLogger().info( "Domain Challenge: " + sac.lastMessage() );
				}
				else
					Loader.getLogger().debug( "Error with Domain" );
		}
		catch ( InvalidKeyException | KeyManagementException | UnrecoverableKeyException | SignatureException | NoSuchAlgorithmException | KeyStoreException | AcmeException | IOException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onLoad() throws PluginException
	{

	}
}
