package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.EventException;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.Versioning;

public class Framework
{
	protected HttpRequest request;
	protected HttpResponse response;
	
	protected FrameworkServer _server;
	protected FrameworkConfigurationManager _config;
	protected FrameworkDatabaseEngine _db;
	protected FrameworkFunctions _fun;
	protected FrameworkImageUtils _img;
	
	protected Enviro env = null;
	protected ByteArrayOutputStream out = new ByteArrayOutputStream();
	protected boolean continueNormally = true;
	protected String alternateOutput = "An Unknown Error Has Risen!";
	protected int httpStatus = 200;
	
	public Evaling eval;
	
	protected String uid;
	
	public String getUid()
	{
		return uid;
	}
	
	public Framework(HttpRequest _request, HttpResponse _response)
	{
		request = _request;
		response = _response;
		
		uid = request.getSession().getId();
		
		env = new Enviro( this );
	}
	
	/*
	 * public void loadPageInternal( String theme, String view, String title, String file, String html, String reqPerm )
	 * throws IOException { try { Site currentSite = request.getSite();
	 * 
	 * if ( currentSite == null ) return;
	 * 
	 * if ( html.isEmpty() && file.isEmpty() ) { response.sendError( 500,
	 * "There was a problem handling your request. Try again later." ); return; }
	 * 
	 * File requestFile = null; if ( !file.isEmpty() ) { if ( file.startsWith( "/" ) ) file = file.substring( 1 );
	 * 
	 * if ( currentSite.protectCheck( file ) ) { Loader.getLogger().warning( "Loading of page '" + file +
	 * "' is not allowed since its hard protected in the site configs." ); response.sendError( 401,
	 * "Loading of this page is not allowed since its hard protected in the site configs." ); return; }
	 * 
	 * if ( Loader.webroot.isEmpty() ) Loader.webroot = "webroot";
	 * 
	 * File siteRoot = new File( Loader.webroot, currentSite.siteId );
	 * 
	 * if ( !siteRoot.exists() ) siteRoot.mkdirs();
	 * 
	 * requestFile = new File( siteRoot, file );
	 * 
	 * Loader.getLogger().info( "Requesting file: " + requestFile.getAbsolutePath() );
	 * 
	 * if ( !requestFile.exists() ) { Loader.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath()
	 * + "'" ); response.sendError( 404 ); return; }
	 * 
	 * if ( requestFile.isDirectory() ) { if ( new File( requestFile, "index.groovy" ).exists() ) { requestFile = new
	 * File( requestFile, "index.groovy" ); } else if ( new File( requestFile, "index.html" ).exists() ) { requestFile =
	 * new File( requestFile, "index.html" ); } else { response.sendError( 500,
	 * "There was a problem finding the index page." ); return; } }
	 * 
	 * if ( !requestFile.exists() ) { Loader.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath()
	 * + "'" ); response.sendError( HttpServletResponse.SC_NOT_FOUND ); return; } }
	 * 
	 * eval = env.newEval();
	 * 
	 * serverVars.put( ServerVars.DOCUMENT_ROOT, new File( Loader.getConfig().getString( "settings.webroot", "webroot" ),
	 * currentSite.getWebRoot( currentSite.getSubDomain() ) ).getAbsolutePath() );
	 * 
	 * Map<String, Object> $server = new HashMap<String, Object>();
	 * 
	 * for ( Entry<ServerVars, Object> en : serverVars.entrySet() ) { $server.put( en.getKey().getName().toLowerCase(),
	 * en.getValue() ); }
	 * 
	 * env.set( "_SERVER", $server ); env.set( "_REQUEST", request.getRequestMap() ); env.set( "_POST",
	 * request.getPostMap() ); env.set( "_GET", request.getGetMap() ); env.set( "_REWRITE", rewriteVars );
	 * 
	 * if ( getUserService().initalize( reqPerm ) ) { if ( !html.isEmpty() ) eval.evalCode( html );
	 * 
	 * if ( requestFile != null ) eval.evalFile( requestFile ); }
	 * 
	 * String source = eval.reset();
	 * 
	 * /* String source; if ( continueNormally ) { source = eval.reset(); } else { currentSite =
	 * Loader.getPersistenceManager().getSiteManager().getSiteById( "framework" );
	 * 
	 * if ( currentSite instanceof FrameworkSite ) ( (FrameworkSite) currentSite ).setDatabase(
	 * Loader.getPersistenceManager().getSql() );
	 * 
	 * source = alternateOutput; theme = "com.chiorichan.themes.error"; view = ""; }
	 * 
	 * 
	 * RenderEvent event = new RenderEvent( this, source );
	 * 
	 * event.theme = theme; event.view = view; event.title = title;
	 * 
	 * try { Loader.getPluginManager().callEventWithException( event );
	 * 
	 * if ( event.sourceChanged() ) source = event.getSource();
	 * 
	 * response.getOutput().write( source.getBytes() ); } catch ( EventException ex ) { ex.printStackTrace();
	 * response.getOutput().write( errorPage( ex.getCause() ).getBytes() ); } } catch ( Throwable e ) {
	 * e.printStackTrace(); response.getOutput().write( errorPage( e ).getBytes() ); } }
	 */
	
	public static String escapeHTML( String l )
	{
		return StringUtils.replaceEach( l, new String[] { "&", "\"", "<", ">" }, new String[] { "&amp;", "&quot;", "&lt;", "&gt;" } );
	}
	
	public String codeSampleEval( String code, int line )
	{
		return codeSampleEval( code, line, -1 );
	}
	
	public String codeSampleEval( String code, int line, int col )
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			
			if ( code.isEmpty() )
				return "";
			
			int cLine = 0;
			for ( String l : code.split( "\n" ) )
			{
				cLine++;
				
				if ( cLine > line - 5 && cLine < line + 5 )
				{
					if ( cLine == line )
					{
						if ( l.length() >= col )
						{
							if ( col > -1 )
							{
								if ( col - 1 > -1 )
									col--;
								
								l = escapeHTML( l.substring( 0, col ) ) + "<span style=\"background-color: red; font-weight: bolder;\">" + l.substring( col, col + 1 ) + "</span>" + escapeHTML( l.substring( col + 1 ) );
							}
							else
								l = escapeHTML( l );
							
							sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
						}
						else
							sb.append( "<span class=\"ln\">" + cLine + "</span> " + escapeHTML( l ) + "\n" ); // XXX: Fix It.
																																			// Why does
																																			// this happen?
					}
					else
					{
						sb.append( "<span class=\"ln\">" + cLine + "</span> " + escapeHTML( l ) + "\n" );
					}
				}
			}
			
			return sb.toString().substring( 0, sb.toString().length() - 1 );
		}
		catch ( Exception e )
		{
			return "Am exception was thrown while preparing code preview: " + e.getMessage();
		}
	}
	
	public String codeSample( String file, int line )
	{
		return codeSample( file, line, -1 );
	}
	
	public String codeSample( String file, int line, int col )
	{
		if ( !file.isEmpty() )
		{
			FileInputStream is;
			try
			{
				is = new FileInputStream( file );
			}
			catch ( FileNotFoundException e )
			{
				return e.getMessage();
			}
			
			StringBuilder sb = new StringBuilder();
			try
			{
				BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
				
				int cLine = 0;
				String l;
				while ( ( l = br.readLine() ) != null )
				{
					l = escapeHTML( l ) + "\n";
					
					cLine++;
					
					if ( cLine > line - 5 && cLine < line + 5 )
					{
						if ( cLine == line )
						{
							sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
						}
						else
						{
							sb.append( "<span class=\"ln\">" + cLine + "</span> " + l );
						}
					}
				}
				
				is.close();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
			
			return sb.toString().substring( 0, sb.toString().length() - 1 );
		}
		return "";
	}
	
	public String javaException( StackTraceElement[] ste )
	{
		if ( ste == null || ste.length < 1 )
			return "";
		
		int l = 0;
		
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement e : ste )
		{
			String file = ( e.getFileName() == null ) ? "eval()" : e.getFileName() + "(" + e.getLineNumber() + ")";
			
			sb.append( "<tr class=\"trace " + ( ( e.getClassName().startsWith( "com.chiori" ) || e.getClassName().startsWith( "org.eclipse" ) || e.getClassName().startsWith( "java" ) ) ? "core" : "app" ) + " collapsed\">\n" );
			sb.append( "	<td class=\"number\">#" + l + "</td>\n" );
			sb.append( "	<td class=\"content\">\n" );
			sb.append( "		<div class=\"trace-file\">\n" );
			sb.append( "			<div class=\"plus\">+</div>\n" );
			sb.append( "			<div class=\"minus\">–</div>\n" );
			sb.append( "			" + file + ": <strong>" + e.getClassName() + "." + e.getMethodName() + "</strong>\n" );
			sb.append( "		</div>\n" );
			sb.append( "		<div class=\"code\">\n" );
			sb.append( "			<pre>Sorry, Code previews are not currently available.</pre>\n" );
			// sb.append( "			<pre><? codeSamp($trc["file"], $trc["line"]); ?></pre>\n" );
			sb.append( "		</div>\n" );
			sb.append( "	</td>\n" );
			sb.append( "</tr>\n" );
			
			l++;
		}
		
		return sb.toString();
	}
	
	public String errorPage( Throwable t )
	{
		// TODO: Make this whole thing better. Optional fancy error pages.
		
		if ( !response.isCommitted() )
		{
			Site currentSite = Loader.getPersistenceManager().getSiteManager().getSiteById( "framework" );
			
			if ( currentSite instanceof FrameworkSite )
				( (FrameworkSite) currentSite ).setDatabase( Loader.getPersistenceManager().getSql() );
			
			try
			{
				env.set( "stackTrace", t );
				env.set( "codeSample", "Sorry, No code preview is available at this time." );
				
				if ( t instanceof CodeParsingException )
				{
					if ( ( (CodeParsingException) t ).getLineNumber() > -1 )
						env.set( "codeSample", codeSampleEval( ( (CodeParsingException) t ).getSourceCode(), ( (CodeParsingException) t ).getLineNumber(), ( (CodeParsingException) t ).getColumnNumber() ) );
				}
				
				env.set( "stackTraceHTML", javaException( t.getStackTrace() ) );
				
				// String page = ? "/panic.php" : "/notfound.php";
				return loadPage( "com.chiorichan.themes.error", "", "Critical Server Exception", "/panic.php", "", "-1" );
			}
			catch ( Exception e1 )
			{
				e1.printStackTrace();
				response.sendError( 500, "Critical Server Exception while loading the Framework Fancy Error Page: " + e1.getMessage() );
			}
		}
		
		return t.getMessage();
	}
	
	public String loadPage( String theme, String view, String title, String file, String html, String reqPerm ) throws IOException, CodeParsingException
	{
		if ( html == null )
			html = "";
		
		if ( file == null )
			file = "";
		
		// TODO: Check reqlevel
		
		String source = html;
		Site currentSite = request.getSite();
		
		if ( !file.isEmpty() )
		{
			if ( file.startsWith( "/" ) )
				file = file.substring( 1 );
			
			if ( currentSite.protectCheck( file ) )
				throw new IOException( "Loading of this page is not allowed since its hard protected in the site configs." );
			
			if ( Loader.webroot.isEmpty() )
				Loader.webroot = "webroot";
			
			File siteRoot = new File( Loader.webroot, currentSite.siteId );
			
			if ( !siteRoot.exists() )
				siteRoot.mkdirs();
			
			File requestFile = new File( siteRoot, file );
			
			Loader.getLogger().info( "Requesting file: " + requestFile.getAbsolutePath() );
			
			if ( !requestFile.exists() )
				throw new IOException( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
			
			if ( requestFile.isDirectory() )
				if ( new File( requestFile, "index.php" ).exists() )
					requestFile = new File( requestFile, "index.php" );
				else if ( new File( requestFile, "index.html" ).exists() )
					requestFile = new File( requestFile, "index.html" );
				else
					throw new IOException( "There was a problem finding the index page." );
			
			if ( !requestFile.exists() )
			{
				throw new IOException( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
			}
			
			source = getFileContents( requestFile.getAbsolutePath() );
			source = getServer().executeCode( source );
		}
		
		/*
		RenderEvent event = new RenderEvent( this, source );
		
		event.theme = theme;
		event.view = view;
		event.title = title;
		
		Loader.getPluginManager().callEvent( event );
		
		if ( event.sourceChanged() )
			source = event.getSource();
			*/
		
		return source;
	}
	
	private String getFileContents( String path )
	{
		FileInputStream is;
		try
		{
			is = new FileInputStream( path );
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
	
	public FrameworkServer getServer()
	{
		if ( _server == null )
			_server = new FrameworkServer( this );
		
		return _server;
	}
	
	public PluginManager getPluginManager()
	{
		return Loader.getPluginManager();
	}
	
	public FrameworkConfigurationManager getConfigurationManager()
	{
		if ( _config == null )
			_config = new FrameworkConfigurationManager( this );
		
		return _config;
	}
	
	public FrameworkDatabaseEngine getDatabaseEngine()
	{
		if ( _db == null )
			_db = new FrameworkDatabaseEngine( this );
		
		return _db;
	}
	
	public FrameworkFunctions getFunctions()
	{
		if ( _fun == null )
			_fun = new FrameworkFunctions( this );
		
		return _fun;
	}
	
	public FrameworkImageUtils getImageUtils()
	{
		if ( _img == null )
			_img = new FrameworkImageUtils( this );
		
		return _img;
	}
	
	public HttpResponse getResponse()
	{
		return response;
	}
	
	public HttpRequest getRequest()
	{
		return request;
	}
	
	public String getProduct()
	{
		return "Chiori Web Server (implementing Chiori Framework API)";
	}
	
	public String getVersion()
	{
		return Loader.getVersion();
	}
	
	public Enviro getEnv()
	{
		return env;
	}
	
	public ByteArrayOutputStream getOutputStream()
	{
		return out;
	}
	
	public static SqlConnector getDatabase()
	{
		return Loader.getPersistenceManager().getSql();
	}
	
	public PersistenceManager getPersistenceManager()
	{
		return Loader.getPersistenceManager();
	}
	
	public void generateError( String errStr )
	{
		generateError( 500, errStr );
	}
	
	public void generateError( Throwable t )
	{
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement s : t.getStackTrace() )
		{
			sb.append( s + "\n" );
		}
		
		env.set( "stackTrace", sb.toString() );
		
		Loader.getLogger().warning( t.getMessage() );
		
		generateError( 500, t.getMessage() );
	}
	
	public void setStatus( int status )
	{
		response.setStatus( status );
	}
	
	public void generateError( int errNo, String reason )
	{
		Loader.getLogger().warning( "Generating Error (" + errNo + "): " + reason );
		
		continueNormally = false;
		
		StringBuilder op = new StringBuilder();
		
		response.setStatus( errNo );
		
		op.append( "<h1>" + getServer().getStatusDescription( errNo ) + "</h1>\n" );
		op.append( "<p class=\"warning show\">" + reason + "</p>\n" );
		
		alternateOutput = op.toString();
		httpStatus = errNo;
	}
	
	public PersistentSession getSession()
	{
		return request.getSession();
	}
	
	public void setRequest( HttpRequest var1 )
	{
		request = var1;
	}
	
	public void setResponse( HttpResponse var1 )
	{
		response = var1;
	}
}
