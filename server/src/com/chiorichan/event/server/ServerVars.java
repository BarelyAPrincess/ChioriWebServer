package com.chiorichan.event.server;

public enum ServerVars
{
	SERVER_ADDR( "SERVER_ADDR", 0 ),
	SERVER_NAME( "SERVER_NAME", 1 ),
	SERVER_ID( "SERVER_ID", 2 ),
	SERVER_SOFTWARE( "SERVER_SOFTWARE", 3 ),
	SERVER_PROTOCAL( "", 4 ),
	REQUEST_METHOD( "REQUEST_METHOD", 5 ),
	REQUEST_TIME( "REQUEST_TIME", 6 ),
	REQUEST_URI( "REQUEST_URI", 7 ),
	QUERY_STRING( "QUERY_STRING", 8 ),
	DOCUMENT_ROOT( "DOCUMENT_ROOT", 9 ),
	HTTP_ACCEPT( "HTTP_ACCEPT", 10 ),
	HTTP_ACCEPT_CHARSET( "HTTP_ACCEPT_CHARSET", 11 ),
	HTTP_ACCEPT_ENCODING( "HTTP_ACCEPT_ENCODING", 12 ),
	HTTP_ACCEPT_LANGUAGE( "HTTP_ACCEPT_LANGUAGE", 13 ),
	HTTP_CONNECTION( "HTTP_CONNECTION", 14 ),
	HTTP_HOST( "HTTP_HOST", 15 ),
	HTTP_USER_AGENT( "HTTP_USER_AGENT", 16 ),
	HTTPS( "HTTPS", 17 ),
	REMOTE_ADDR( "REMOTE_ADDR", 18 ),
	REMOTE_HOST( "REMOTE_HOST", 19 ),
	REMOTE_PORT( "REMOTE_PORT", 20 ),
	REMOTE_USER( "REMOTE_USER", 21 ),
	SERVER_ADMIN( "SERVER_ADMIN", 22 ),
	SERVER_PORT( "SERVER_PORT", 23 ),
	SERVER_SIGNATURE( "SERVER_SIGNATURE", 24 ),
	AUTH_DIGEST( "AUTH_DIGEST", 25 ),
	AUTH_USER( "AUTH_USER", 26 ),
	AUTH_PW( "AUTH_PW", 27 ),
	AUTH_TYPE( "AUTH_TYPE", 28 ),
	CONTENT_LENGTH( "CONTENT_LENGTH", 29 ),
	SESSION( "SESSION", 30 ),
	PHP_SELF( "PHP_SELF", 31 ),
	HTTP_X_REQUESTED_WITH( "HTTP_X_REQUESTED_WITH", 32 );
	
	private final int id;
	private final String name;
	
	private ServerVars(String name, int id)
	{
		this.id = id;
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getId()
	{
		return id;
	}

	public static ServerVars parse( String key )
	{
		for ( ServerVars sv : ServerVars.values() )
			if ( sv.getName().equalsIgnoreCase( key ) )
				return sv;
		
		return null;
	}
}
