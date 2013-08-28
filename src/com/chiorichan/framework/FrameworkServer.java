package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

import com.caucho.quercus.QuercusErrorException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LargeStringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.parser.QuercusParseException;
import com.chiorichan.Loader;

public class FrameworkServer
{
	protected Framework fw;
	
	public FrameworkServer(Framework fw0)
	{
		fw = fw0;
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
		String result = "";
		
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
			
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				sb.append( l );
				sb.append( '\n' );
			}
			
			is.close();
			
			result = executeCode( sb.toString() );
		}
		catch ( IOException e )
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
		try
		{
			File root = getTemplateRoot( fw.getCurrentSite() );
			return executeCode( getPackageSource( root, pack ) );
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
		{
			e.printStackTrace();
			return e.getMessage();
		}
	}
	
	public String executeCode( String source ) throws IOException, QuercusParseException, QuercusErrorException
	{
		StringValue sv = new LargeStringBuilderValue();
		sv.append( "?> " + source );
		
		Env env = fw.getEnv();
		ByteArrayOutputStream out = fw.getOutputStream();
		
		env.evalCode( sv );
		
		env.getOut().flush();
		source = new String( out.toByteArray() );
		out.reset();
		
		return source.trim();
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
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
			
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
				//fw.getResponse().sendError( i, string );
		}
		catch ( IOException e )
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

	public void initSession()
	{
		
		fw.getEnv().setIni( "session.cookie_domain", ".applebloom.co" );
		
		try
		{
			executeCode( "<? session_set_cookie_params( null, \"/\", \"." + fw.siteDomain + "\" ) ?>" );
			executeCode( "<? session_name( \"ChioriSessionId\" ); ?>" );
			executeCode( "<? session_start(); ?>" );
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
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
		try
		{
			String val = executeCode( "<? echo $_REQUEST[\"" + key + "\"]; ?>" );
			
			if ( val == null || val.isEmpty() )
				return def;
			
			return val.trim();
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
		{
			e.printStackTrace();
			return "";
		}
	}
	
	public String getSessionString ( String key )
	{
		return getSessionString( key, "" );
	}
	
	public String getSessionString ( String key, String def )
	{
		// fw.getRequest().getSession().getAttribute( key );
		
		try
		{
			String val = executeCode( "<?echo$_SESSION[\"" + key + "\"];?>" );
			
			if ( val == null || val.isEmpty() )
				return def;
			
			return val;
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
		{
			e.printStackTrace();
			return "";
		}
	}
	
	public boolean setSessionString ( String key )
	{
		return setSessionString( key, "" );
	}
	
	public boolean setSessionString ( String key, String value )
	{
		if ( value == null )
			value = "";
		
		//fw.getRequest().getSession().setAttribute( key, value );
		
		try
		{
			executeCode( "<? $_SESSION[\"" + key + "\"] = \"" + value + "\"; ?>" );
			return true;
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public void setCookieExpiry ( int valid )
	{
		if ( valid < 1 )
			valid = 604800;
		
		fw.getRequest().getSession().setMaxInactiveInterval( valid );
		
		try
		{
			executeCode( "<? session_set_cookie_params( time() + " + valid + ", \"/\", \"." + fw.siteDomain + "\" ); if ( isset( $_COOKIE[ \"ChioriSessionId\" ] ) ) setcookie( \"ChioriSessionId\", $_COOKIE[ \"ChioriSessionId\" ], time() + " + valid + ", \"/\", \"." + fw.siteDomain + "\" ); ?>" );
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
		{
			e.printStackTrace();
		}
	}
	
	public void destroySession ()
	{
		try
		{
			executeCode( "<? session_destroy(); ?>" );
		}
		catch ( QuercusErrorException | QuercusParseException | IOException e )
		{
			e.printStackTrace();
		}
	}
}
