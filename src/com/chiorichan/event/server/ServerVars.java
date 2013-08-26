package com.chiorichan.event.server;

public enum ServerVars
{
	SERVER_ADDR( 0 ),
	SERVER_NAME( 1 ),
	SERVER_ID( 2 ),
	SERVER_SOFTWARE( 3 ),
	SERVER_PROTOCAL( 4 ),
	REQUEST_METHOD( 5 ),
	REQUEST_TIME( 6 ),
	REQUEST_URI( 7 ),
	QUERY_STRING( 8 ),
	DOCUMENT_ROOT( 9 ),
	HTTP_ACCEPT( 10 ),
	HTTP_ACCEPT_CHARSET( 11 ),
	HTTP_ACCEPT_ENCODING( 12 ),
	HTTP_ACCEPT_LANGUAGE( 13 ),
	HTTP_CONNECTION( 14 ),
	HTTP_HOST( 15 ),
	HTTP_USER_AGENT( 16 ),
	HTTPS( 17 ),
	REMOTE_ADDR( 18 ),
	REMOTE_HOST( 19 ),
	REMOTE_PORT( 20 ),
	REMOTE_USER( 21 ),
	SERVER_ADMIN( 22 ),
	SERVER_PORT( 23 ),
	SERVER_SIGNATURE( 24 ),
	AUTH_DIGEST( 25 ),
	AUTH_USER( 26 ),
	AUTH_PW( 27 ),
	AUTH_TYPE( 28 ),
	CONTENT_LENGTH( 29 ),
	SESSION( 30 ),
	PHP_SELF( 31 );
	
	private final int id;
	
	private ServerVars(int id)
	{
		this.id = id;
	}
	
	public int getId()
	{
		return id;
	}
}
