package com.chiorichan.plugin.acme.lang;

public class AcmeException extends Exception
{
	private static final long serialVersionUID = -8734552201324294559L;

	public AcmeException( String message )
	{
		super( message );
	}

	public AcmeException( String message, Throwable cause )
	{
		super( message, cause );
	}

	public AcmeException( Throwable cause )
	{
		super( cause );
	}
}
