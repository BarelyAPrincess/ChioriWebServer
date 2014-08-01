package com.chiorichan.framework;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import com.chiorichan.Loader;
import com.chiorichan.http.PersistentSession;
import com.sun.jersey.core.util.Base64;

public class HttpUtilsWrapper extends WebUtils
{
	PersistentSession sess;
	
	public HttpUtilsWrapper(PersistentSession _sess)
	{
		sess = _sess;
	}
	
	protected String findPackagePath( String pack )
	{
		try
		{
			Site site = sess.getSite();
			return findPackagePathWithException( pack, site );
		}
		catch ( FileNotFoundException e )
		{
			try
			{
				Site site = Loader.getSiteManager().getSiteById( "framework" );
				return findPackagePathWithException( pack, site );
			}
			catch ( FileNotFoundException e1 )
			{
				Loader.getLogger().warning( e1.getMessage() );
				return "";
			}
		}
	}
	
	public String readPackage( String pack ) throws IOException
	{
		return readPackage( pack, sess.getSite() );
	}
	
	public String evalPackage( String pack ) throws IOException, CodeParsingException
	{
		Evaling eval = sess.getEvaling();
		
		return evalPackage( eval, pack, sess.getSite() );
	}
	
	public String evalFile( File file ) throws IOException, CodeParsingException
	{
		return evalFile( file.getAbsolutePath() );
	}
	
	public String evalFile( String absoluteFile ) throws IOException, CodeParsingException
	{
		Evaling eval = sess.getEvaling();
		return evalFile( eval, absoluteFile );
	}
	
	public String evalGroovy( String source ) throws IOException, CodeParsingException
	{
		return evalGroovy( source, "" );
	}
	
	public String evalGroovy( String source, String filePath ) throws IOException, CodeParsingException
	{
		Evaling eval = sess.getEvaling();
		
		return evalGroovy( eval, source, filePath );
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
			String basicAuth = "Basic " + new String( new Base64().encode( userpass.getBytes() ) );
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
