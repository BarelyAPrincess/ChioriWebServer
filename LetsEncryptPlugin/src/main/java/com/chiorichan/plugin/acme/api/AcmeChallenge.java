package com.chiorichan.plugin.acme.api;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.chiorichan.APILogger;
import com.chiorichan.http.HttpCode;
import com.chiorichan.plugin.acme.AcmePlugin;
import com.chiorichan.plugin.acme.lang.AcmeException;
import com.chiorichan.plugin.acme.lang.AcmeForbiddenError;
import com.chiorichan.plugin.loader.Plugin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

public class AcmeChallenge
{
	private final List<SingleAcmeChallenge> challenges = Lists.newArrayList();
	private AcmeProtocol proto;

	protected AcmeChallenge( AcmeProtocol proto ) throws AcmeException, JsonProcessingException, IOException
	{
		Validate.notNull( proto );
		this.proto = proto;
	}

	public List<SingleAcmeChallenge> add( String... domains ) throws InvalidKeyException, KeyManagementException, UnrecoverableKeyException, SignatureException, NoSuchAlgorithmException, KeyStoreException, AcmeException, IOException
	{
		for ( String domain : domains )
		{
			HttpResponse response = proto.newChallenge0( domain );

			if ( response.getStatus() == HttpCode.HTTP_FORBIDDEN )
				throw new AcmeForbiddenError();

			if ( response.getStatus() != HttpCode.HTTP_CREATED )
				throw new AcmeException( "Http code '" + response.getStatus() + "' '" + HttpCode.msg( response.getStatus() ) + "' was returned when we expected '201' 'CREATED'." );

			JsonNode json = new ObjectMapper().readTree( response.getBody() );

			for ( JsonNode challange : json.get( "challenges" ) )
			{
				String type = challange.get( "type" ).asText();
				String challengeToken = challange.get( "token" ).asText();
				String challengeUri = challange.get( "uri" ).asText();

				switch ( type )
				{
					case "tls-sni-01":
						// Challenge Not Implemented!
						break;
					case "http-01":
						challenges.add( new SingleAcmeChallenge( proto, AcmeChallengeType.HTTP_01, challengeToken, challengeUri, domain ) );
						break;
					case "dns-01":
						// Challenge Not Implemented!
						break;
					default:
						getLogger().warning( "Unsupported challenge type received, '" + type + "'" );
				}
			}

			if ( challenges.size() == 0 )
				throw new AcmeException( "No supoorted challenges received" );
		}

		/*
		 * {
		 * "identifier":
		 * {
		 * "type":"dns",
		 * "value":"penoaks.com"
		 * },
		 * "status":"pending",
		 * "expires":"2015-12-18T03:25:53.827339252Z",
		 * "challenges":[
		 * {
		 * "type":"http-01",
		 * "status":"pending",
		 * "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/Em0pH-z9EEPVUeEtYQzXHv0Vep6V0IXBvzPDQ_xLudY/851773",
		 * "token":"IR8AY8RJoub2siLyjb2apidshNCvHH_9oGBje_nmupo"
		 * },
		 * {
		 * "type":"dns-01",
		 * "status":"pending",
		 * "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/Em0pH-z9EEPVUeEtYQzXHv0Vep6V0IXBvzPDQ_xLudY/851774",
		 * "token":"4x39y4kLL6kK2lOkHs0t9V81OikYVxhnyKT7HeRwPsA"
		 * },
		 * {
		 * "type":"tls-sni-01",
		 * "status":"pending",
		 * "uri":"https://acme-staging.api.letsencrypt.org/acme/challenge/Em0pH-z9EEPVUeEtYQzXHv0Vep6V0IXBvzPDQ_xLudY/851775",
		 * "token":"XS3U_tawnTnFvrafgVS7c2ryENlq2I2_N6X8sosPBho"
		 * }
		 * ]
		 * }
		 */


		return getChallenges();
	}

	public List<SingleAcmeChallenge> getChallenges()
	{
		return Collections.unmodifiableList( challenges );
	}

	private APILogger getLogger()
	{
		return Plugin.getPlugin( AcmePlugin.class ).getLogger();
	}
}
