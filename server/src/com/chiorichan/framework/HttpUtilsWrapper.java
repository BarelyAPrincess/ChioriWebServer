package com.chiorichan.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.chiorichan.Loader;
import com.chiorichan.http.PersistentSession;

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
}
