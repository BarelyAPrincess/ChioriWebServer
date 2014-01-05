package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.http.HttpCode;

public class FrameworkServer
{
	protected Framework fw;
	
	public FrameworkServer(Framework fw0)
	{
		fw = fw0;
	}
	
	public String fileReader( String file )
	{
		if ( file == null || file.isEmpty() )
			return "";
		
		Loader.getLogger().info( "Reading file: " + file );
		
		FileInputStream is;
		try
		{
			is = new FileInputStream( file );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		String result = "";
		
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
			
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				sb.append( l + "\n" );
			}
			
			is.close();
			
			result = executeCode( sb.toString() );
		}
		catch ( IOException | CodeParsingException e )
		{
			e.printStackTrace();
			return "";
		}
		
		applyAlias( result, fw.getRequest().getSite().getAliases() );
		
		return result;
	}
	
	public String applyAlias( String source, Map<String, String> aliases )
	{
		if ( aliases == null || aliases.size() < 1 )
			return source;
		
		for ( Entry<String, String> entry : aliases.entrySet() )
		{
			source = source.replace( "%" + entry.getKey() + "%", entry.getValue() );
		}
		
		return source;
	}
	
	public String includePackage( String pack )
	{
		try
		{
			Evaling eval = fw.getSession().getEvaling();
			
			File root = getTemplateRoot( fw.getRequest().getSite() );
			
			String file = getPackage( root, pack );
			
			if ( !file.isEmpty() )
				eval.evalFile( file );
			
			return eval.reset();
		}
		catch ( CodeParsingException | IOException e )
		{
			e.printStackTrace();
			return e.getMessage();
		}
	}
	
	public String executeCode( String source ) throws IOException, CodeParsingException
	{
		Evaling eval = fw.getEnv().newEval();
		
		if ( !source.isEmpty() )
			eval.evalCode( source );
		
		return eval.reset();
	}
	
	public String includeCode( String source ) throws IOException, CodeParsingException
	{
		Evaling eval = fw.getEnv().newEval();
		
		if ( !source.isEmpty() )
			eval.evalCode( source );
		
		return eval.reset();
	}
	
	public File getTemplateRoot( Site site )
	{
		File templateRoot = site.getAbsoluteRoot( null );
		templateRoot = new File( templateRoot.getAbsolutePath() + ".template" );
		
		if ( templateRoot.isFile() )
			templateRoot.delete();
		
		if ( !templateRoot.exists() )
			templateRoot.mkdirs();
		
		return templateRoot;
	}
	
	public String getPackage( File root, String pack )
	{
		if ( pack == null || pack.isEmpty() )
			return "";
		
		pack = pack.replace( ".", System.getProperty( "file.separator" ) );
		
		File file = new File( root, pack + ".php" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".inc.php" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".groovy" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".inc.groovy" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".chi" );
		
		if ( !file.exists() )
			file = new File( root, pack );
		
		if ( !file.exists() )
		{
			Loader.getLogger().info( "Could not find the file " + file.getAbsolutePath() );
			return "";
		}
		
		return file.getAbsolutePath();
	}
	
	public String getPackageSource( File root, String pack )
	{
		if ( pack == null || pack.isEmpty() )
			return "";
		
		pack = pack.replace( ".", System.getProperty( "file.separator" ) );
		
		File file = new File( root, pack + ".php" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".inc.php" );
		
		if ( !file.exists() )
			file = new File( root, pack );
		
		if ( !file.exists() )
		{
			Loader.getLogger().info( "Could not find the file " + file.getAbsolutePath() );
			return "";
		}
		
		Loader.getLogger().info( "Retriving File: " + file.getAbsolutePath() );
		
		FileInputStream is;
		try
		{
			is = new FileInputStream( file );
		}
		catch ( FileNotFoundException e )
		{
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
			
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				sb.append( l );
				sb.append( '\n' );
			}
			
			is.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		return sb.toString();
	}
	
	@Deprecated
	public void panic( int i, String string )
	{
		if ( !fw.getResponse().isCommitted() )
			fw.generateError( i, string );
	}
	
	/**
	 * @deprecated Use getRequest().sendRedirect() instead.
	 */
	@Deprecated
	public void dummyRedirect( String string )
	{
		dummyRedirect( string, 302 );
	}
	
	/**
	 * @deprecated Use getRequest().sendRedirect() instead.
	 */
	@Deprecated
	public void dummyRedirect( String var1, int reasonCode )
	{
		Loader.getLogger().info( "The server is sending a page redirect (" + reasonCode + "): " + var1 );
		
		if ( !fw.getResponse().isCommitted() )
		{
			fw.getResponse().setStatus( reasonCode );
			fw.getResponse().sendRedirect( var1 );
			return;
		}
		
		try
		{
			fw.getResponse().println( "<script>window.location = '" + var1 + "';</script>" );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	@Deprecated
	public String getRequest( String key )
	{
		return getRequest( key, "" );
	}
	
	@Deprecated
	public String getRequest( String key, String def )
	{
		return getRequest( key, "", false );
	}
	
	@Deprecated
	public String getRequest( String key, String def, boolean rtnNull )
	{
		return fw.getRequest().getArgument( key, def, rtnNull );
	}
	
	@Deprecated
	public void Error( String msg )
	{
		Loader.getLogger().warning( msg );
	}
	
	@Deprecated
	public void Warning( String msg )
	{
		Loader.getLogger().warning( msg );
	}
	
	@Deprecated
	public void Info( String msg )
	{
		Loader.getLogger().info( msg );
	}
	
	@Deprecated
	public void Debug( String msg )
	{
		Loader.getLogger().fine( msg );
	}
	
	@Deprecated
	public void Debug1( String msg )
	{
		Loader.getLogger().fine( msg );
	}
	
	@Deprecated
	public void Debug2( String msg )
	{
		Loader.getLogger().fine( msg );
	}
	
	@Deprecated
	public void Debug3( String msg )
	{
		Loader.getLogger().fine( msg );
	}
	
	/**
	 * @deprecated Use HttpCode.msg( errNo ) instead
	 */
	@Deprecated
	public String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo );
	}
}
