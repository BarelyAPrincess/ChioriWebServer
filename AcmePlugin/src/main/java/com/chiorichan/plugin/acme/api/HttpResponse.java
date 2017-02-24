package com.chiorichan.plugin.acme.api;

import com.chiorichan.logger.Log;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpResponse
{
	private final String target;
	private final int status;
	private final Map<String, List<String>> responseHeaders;
	private final ByteBuf body;

	public HttpResponse( String target, int status, Map<String, List<String>> responseHeaders, ByteBuf body )
	{
		this.target = target;
		this.status = status;
		this.responseHeaders = responseHeaders;
		this.body = body;
	}

	public void debug()
	{
		Log.get().info( "Debug HttpResponse:" );

		Log.get().info( "\tTarget: " + target );
		Log.get().info( "\tStatus: " + status );
		Log.get().info( "\tResponse: " + body.toString( Charset.defaultCharset() ) );
		Log.get().info( "\tHeaders:" );

		for ( String key : headerKeys() )
			Log.get().info( "\t\t" + key + ": " + getHeaderString( key ) );
	}

	public String getBodyString()
	{
		return body.toString( Charset.defaultCharset() );
	}

	public ByteBuf getBody()
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
		return String.format( "target='%s',status='%s',body='%s',headers='%s'", target, status, body.toString( Charset.defaultCharset() ), headersString() );
	}
}
