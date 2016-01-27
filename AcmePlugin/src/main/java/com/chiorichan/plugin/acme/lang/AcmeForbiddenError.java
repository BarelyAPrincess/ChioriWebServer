package com.chiorichan.plugin.acme.lang;


public class AcmeForbiddenError extends AcmeException
{
	private static final long serialVersionUID = 853786586412519841L;

	public AcmeForbiddenError()
	{
		super( "Received a forbidden response" );
	}
}
