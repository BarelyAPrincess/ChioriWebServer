package com.chiorichan.http;

import java.sql.SQLException;

public class SessionException extends Exception
{
	public SessionException(String string)
	{
		super( string );
	}

	public SessionException(SQLException e)
	{
		super( e );
	}

	private static final long serialVersionUID = -1665918782123029882L;
}
