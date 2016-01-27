package com.chiorichan.plugin.acme.api;

public enum AcmeChallengeType
{
	NULL( null ), HTTP_01( "http-01" ), TLS_SNI_01( "tls-sni-01" ), DNS_01( "dns-01" );

	public static AcmeChallengeType get( String key )
	{
		if ( key != null )
			for ( AcmeChallengeType act : values() )
				if ( act.key != null && act.key.equals( key ) )
					return act;
		return null;
	}

	final String key;

	AcmeChallengeType( String key )
	{
		this.key = key;
	}
}
