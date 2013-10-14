package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.chiorichan.Loader;
import com.chiorichan.event.server.ServerVars;

public class FrameworkServer
{
	protected Framework fw;
	
	public FrameworkServer(Framework fw0)
	{
		fw = fw0;
	}
	
	public void sendRedirect( String target )
	{
		sendRedirect( target, 307, true );
	}
	
	public void sendRedirect( String target, int httpStatus )
	{
		sendRedirect( target, httpStatus, true );
	}
	
	public void sendRedirect( String target, int httpStatus, boolean autoRedirect )
	{
		if ( autoRedirect )
		{
			try
			{
				fw.getResponse().setHeader( "HTTP/1.1", httpStatus + "" );
				fw.getResponse().sendRedirect( target );
				
				// request.getRequestDispatcher( target ).forward( request, response );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		else
		{
			// TODO: Send client a redirection page.
			// "The Request URL has been relocated to: " . $StrURL .
			// "<br />Please change any bookmarks to reference this new location."
		}
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
		
		applyAlias( result, fw.getCurrentSite().getAliases() );
		
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
		Evaling eval = fw.getEnv().newEval();
		
		try
		{
			File root = getTemplateRoot( fw.getCurrentSite() );
			
			String file = getPackage( root, pack );
			
			//System.out.println( "File: " + file );
			
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
	
	public void panic( int i, String string )
	{
		try
		{
			if ( !fw.getResponse().isCommitted() )
				executeCode( "<h1>" + string + "</h1>" );
			// fw.getResponse().sendError( i, string );
		}
		catch ( IOException | CodeParsingException e )
		{
			e.printStackTrace();
		}
	}
	
	public void dummyRedirect( String string )
	{
		try
		{
			if ( !fw.getResponse().isCommitted() )
			{
				fw.getResponse().sendRedirect( fw.getResponse().encodeRedirectURL( string ).toString() );
				return;
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		try
		{
			fw.getResponse().getWriter().println( "<script>window.location = '" + string + "';</script>" );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	public String getRequest( String key )
	{
		return getRequest( key, "" );
	}
	
	public String getRequest( String key, String def )
	{
		return getRequest( key, "", false );
	}
	
	/*
	 * boolean rtnNull - Return null if not set.
	 */
	public String getRequest( String key, String def, boolean rtnNull )
	{
		Map<ServerVars, Object> request = fw.getServerVars();
		String val = (String) request.get( ServerVars.parse( key ) );
		
		if ( val == null && rtnNull )
			return null;
		
		if ( val == null || val.isEmpty() )
			return def;
		
		return val.trim();
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
	
	@Deprecated
	public void initSession()
	{
		
	}
	
	@Deprecated
	public String getSessionString( String key )
	{
		return fw.getUserService().getSessionString( key );
	}
	
	@Deprecated
	public String getSessionString( String key, String def )
	{
		return fw.getUserService().getSessionString( key, def );
	}
	
	@Deprecated
	public boolean setSessionString( String key )
	{
		return fw.getUserService().setSessionString( key );
	}
	
	@Deprecated
	public boolean setSessionString( String key, String value )
	{
		return fw.getUserService().setSessionString( key, value );
	}
	
	@Deprecated
	public void setCookieExpiry( int valid )
	{
		fw.getUserService().setCookieExpiry( valid );
	}
	
	@Deprecated
	public void destroySession()
	{
		fw.getUserService().destroySession();
	}
	
	public String getStatusDescription( int errNo )
	{
		Map<Integer, String> statusCodes = new LinkedHashMap<Integer, String>();
		
		statusCodes.put( 202, "Accepted" );
		statusCodes.put( 208, "Already Reported" );
		statusCodes.put( 502, "Bad Gateway" );
		statusCodes.put( 400, "Bad Request" );
		statusCodes.put( 409, "Conflict" );
		statusCodes.put( 100, "Continue" );
		statusCodes.put( 201, "Created" );
		statusCodes.put( 421, "Destination Locked" );
		statusCodes.put( 417, "Expectation Failed" );
		statusCodes.put( 424, "Failed Dependency" );
		statusCodes.put( 403, "Forbidden" );
		statusCodes.put( 302, "Found" );
		statusCodes.put( 504, "Gateway Timeout" );
		statusCodes.put( 410, "Gone" );
		statusCodes.put( 505, "HTTP Version Not Supported" );
		statusCodes.put( 226, "IM Used" );
		statusCodes.put( 419, "Insufficient Space on Resource" );
		statusCodes.put( 507, "Insufficient Storage" );
		statusCodes.put( 500, "Internal Server Error" );
		statusCodes.put( 411, "Length Required" );
		statusCodes.put( 423, "Locked" );
		statusCodes.put( 508, "Loop Detected" );
		statusCodes.put( 420, "Method Failure" );
		statusCodes.put( 405, "Method Not Allowed" );
		statusCodes.put( 301, "Moved Permanently" );
		statusCodes.put( 302, "Moved Temporarily" );
		statusCodes.put( 207, "Multi-Status" );
		statusCodes.put( 300, "Multiple Choices" );
		statusCodes.put( 204, "No Content" );
		statusCodes.put( 203, "Non-Authoritative Information" );
		statusCodes.put( 406, "Not Acceptable" );
		statusCodes.put( 510, "Not Extended" );
		statusCodes.put( 404, "Not Found" );
		statusCodes.put( 501, "Not Implemented" );
		statusCodes.put( 304, "Not Modified" );
		statusCodes.put( 200, "OK" );
		statusCodes.put( 206, "Partial Content" );
		statusCodes.put( 402, "Payment Required" );
		statusCodes.put( 412, "Precondition failed" );
		statusCodes.put( 102, "Processing" );
		statusCodes.put( 407, "Proxy Authentication Required" );
		statusCodes.put( 413, "Request Entity Too Large" );
		statusCodes.put( 408, "Request Timeout" );
		statusCodes.put( 414, "Request-URI Too Long" );
		statusCodes.put( 416, "Requested Range Not Satisfiable" );
		statusCodes.put( 205, "Reset Content" );
		statusCodes.put( 303, "See Other" );
		statusCodes.put( 503, "Service Unavailable." );
		statusCodes.put( 101, "Switching Protocols" );
		statusCodes.put( 307, "Temporary Redirect" );
		statusCodes.put( 401, "Unauthorized" );
		statusCodes.put( 422, "Unprocessable Entity" );
		statusCodes.put( 415, "Unsupported Media Type" );
		statusCodes.put( 426, "Upgrade Required" );
		statusCodes.put( 305, "Use Proxy" );
		statusCodes.put( 506, "Variant Also Negotiates" );
		statusCodes.put( 418, "I'm a Madman With A Blue Box!" );
		
		return statusCodes.get( errNo );
	}
}
