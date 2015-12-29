package com.chiorichan.plugin.acme.api;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.chiorichan.Loader;

public class HttpResponse
{
	private final String target;
	private final int status;
	private final Map<String, List<String>> responseHeaders;
	private final byte[] body;

	public HttpResponse( String target, int status, Map<String, List<String>> responseHeaders, byte[] body )
	{
		this.target = target;
		this.status = status;
		this.responseHeaders = responseHeaders;
		this.body = body;
	}

	public void debug()
	{
		Loader.getLogger().info( "Debug HttpResponse:" );

		Loader.getLogger().info( "\tTaget: " + target );
		Loader.getLogger().info( "\tStatus: " + status );
		Loader.getLogger().info( "\tResponse: " + new String( body ) );
		Loader.getLogger().info( "\tHeaders:" );

		for ( String key : headerKeys() )
			Loader.getLogger().info( "\t\t" + key + ": " + getHeaderString( key ) );
	}

	public byte[] getBody()
	{
		return body;
	}

	public String getHeaderString( final String key )
	{
		for ( final Map.Entry<String, List<String>> e : responseHeaders.entrySet() )
			if ( key == null && e.getKey() == null || key != null && key.equalsIgnoreCase( e.getKey() ) )
				return e.getValue() == null || e.getValue().size() == 0 ? null : e.getValue().get( 0 );
		return null;
	}

	public int getStatus()
	{
		return status;
	}

	public String getTarget()
	{
		return target;
	}

	public Set<String> headerKeys()
	{
		return responseHeaders.keySet();
	}

	public String headersString()
	{
		StringBuilder sb = new StringBuilder();
		boolean first1 = true;

		for ( Entry<String, List<String>> e : responseHeaders.entrySet() )
		{
			if ( !first1 )
				sb.append( "," );
			first1 = false;

			if ( e.getKey() != null )
				sb.append( "`" + e.getKey() + "`=" );

			if ( e.getValue() == null )
				sb.append( "null" );
			else
			{
				boolean first2 = true;
				sb.append( "[" );
				for ( String s : e.getValue() )
					if ( s != null )
					{
						if ( !first2 )
							sb.append( "," );
						sb.append( s );
						first2 = false;
					}
				sb.append( "]" );
			}
		}

		return sb.toString();
	}

	@Override
	public String toString()
	{
		return String.format( "target='%s',status='%s',body='%s',headers='%s'", target, status, new String( body ), headersString() );
	}
}
