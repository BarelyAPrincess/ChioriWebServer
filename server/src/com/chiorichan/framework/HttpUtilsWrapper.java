package com.chiorichan.framework;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.chiorichan.http.PersistentSession;
import com.sun.jersey.core.util.Base64;

public class HttpUtilsWrapper extends WebUtils
{
	PersistentSession sess;
	
	public HttpUtilsWrapper(PersistentSession _sess)
	{
		sess = _sess;
	}
	
	public byte[] readUrl( String url ) throws IOException
	{
		return readUrl( url, null, null );
	}
	
	public byte[] readUrl( String surl, String user, String pass ) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		URL url = new URL( surl );
		URLConnection uc = url.openConnection();
		if ( user != null || pass != null )
		{
			String userpass = user + ":" + pass;
			String basicAuth = "Basic " + new String( Base64.encode( userpass.getBytes() ) );
			uc.setRequestProperty( "Authorization", basicAuth );
		}
		InputStream is = uc.getInputStream();
		
		byte[] byteChunk = new byte[4096];
		int n;
		
		while ( ( n = is.read( byteChunk ) ) > 0 )
		{
			out.write( byteChunk, 0, n );
		}
		
		is.close();
		
		return out.toByteArray();
	}
}
