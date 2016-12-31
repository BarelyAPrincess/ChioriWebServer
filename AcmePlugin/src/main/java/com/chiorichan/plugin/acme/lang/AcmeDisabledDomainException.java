package com.chiorichan.plugin.acme.lang;

/**
 * Thrown when client attempts to check a domain that is disabled per configuration
 */
public class AcmeDisabledDomainException extends AcmeException
{
	public AcmeDisabledDomainException( String message )
	{
		super( message );
	}
}
