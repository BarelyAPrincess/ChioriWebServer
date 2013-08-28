package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

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
		
		return source;
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
				fw.getResponse().sendError( i, string );
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
}
