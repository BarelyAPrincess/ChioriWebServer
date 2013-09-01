package com.chiorichan.framework;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.QuercusErrorException;
import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.QuercusLineRuntimeException;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LargeStringBuilderValue;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.chiorichan.Loader;
import com.chiorichan.database.SqlConnector;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.event.server.ServerVars;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.plugin.PluginManager;

public class Framework
{
	protected static SqlConnector sql = new SqlConnector();
	protected static SiteManager _sites = new SiteManager( sql );
	
	protected Map<String, String> rewriteGlobals = new HashMap<String, String>();
	
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected FilterChain chain;
	protected QuercusContext _quercus;
	protected ServletContext _servletContext;
	
	protected FrameworkServer _server;
	protected FrameworkConfigurationManager _config;
	protected FrameworkDatabaseEngine _db;
	protected FrameworkUserService _usr;
	protected FrameworkFunctions _fun;
	protected FrameworkImageUtils _img;
	
	protected Env env = null;
	protected ByteArrayOutputStream out = new ByteArrayOutputStream();
	protected boolean continueNormally = true;
	protected String alternateOutput = "An Unknown Error Has Risen!";
	protected int httpStatus = 200;
	
	protected String siteId, siteTitle, siteDomain, siteSubDomain, requestId;
	protected Site currentSite;
	
	public Framework(HttpServletRequest request0, HttpServletResponse response0, FilterChain chain0, ServletContext servletContext)
	{
		request = request0;
		response = response0;
		chain = chain0;
		_servletContext = servletContext;
	}
	
	public void init() throws IOException, ServletException
	{
		String uri = request.getRequestURI();
		String domain = request.getServerName();
		String site = "";
		
		request.setCharacterEncoding( "ISO-8859-1" );
		response.setCharacterEncoding( "ISO-8859-1" );
		
		if ( uri.startsWith( "/" ) )
			uri = uri.substring( 1 );
		
		if ( domain.equalsIgnoreCase( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) | domain.equalsIgnoreCase( request.getLocalAddr() ) )
			domain = "accounts.applebloom.co"; // domain = "";
			
		if ( domain.split( "\\." ).length > 2 )
		{
			String[] r = domain.split( "\\.", 2 );
			site = r[0];
			domain = r[1];
		}
		
		currentSite = _sites.getSiteByDomain( domain );
		
		// TODO: Generate a blank site and save it based on the information known
		if ( currentSite == null )
			currentSite = new Site( "default", Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" ), domain );
		
		siteDomain = domain;
		siteSubDomain = site;
		
		// TODO: Fix this so requests are limited within the same domain level but will be overridable by the site config.
		response.addHeader( "Access-Control-Allow-Origin", "*" );
		
		requestId = DigestUtils.md5Hex( request.getSession().getId() + request.getRemoteAddr() + uri );
		
		Map<ServerVars, Object> _server = new HashMap<ServerVars, Object>();
		// _server.put( ServerVars.PHP_SELF, requestFile.getPath() );
		_server.put( ServerVars.DOCUMENT_ROOT, Loader.getConfig().getString( "settings.webroot", "webroot" ) );
		_server.put( ServerVars.HTTP_ACCEPT, request.getHeader( "Accept" ) );
		_server.put( ServerVars.HTTP_USER_AGENT, request.getHeader( "User-Agent" ) );
		_server.put( ServerVars.HTTP_CONNECTION, request.getHeader( "Connection" ) );
		_server.put( ServerVars.HTTP_HOST, request.getHeader( "Host" ) );
		_server.put( ServerVars.HTTP_ACCEPT_ENCODING, request.getHeader( "Accept-Encoding" ) );
		_server.put( ServerVars.HTTP_ACCEPT_LANGUAGE, request.getHeader( "Accept-Language" ) );
		_server.put( ServerVars.REMOTE_HOST, request.getRemoteHost() );
		_server.put( ServerVars.REMOTE_ADDR, request.getRemoteAddr() );
		_server.put( ServerVars.REMOTE_PORT, request.getRemotePort() );
		_server.put( ServerVars.REQUEST_TIME, System.currentTimeMillis() );
		_server.put( ServerVars.REQUEST_URI, request.getRequestURI() );
		_server.put( ServerVars.CONTENT_LENGTH, request.getContentLength() );
		_server.put( ServerVars.AUTH_TYPE, request.getAuthType() );
		_server.put( ServerVars.SERVER_NAME, request.getServerName() );
		_server.put( ServerVars.SERVER_PORT, request.getServerPort() );
		_server.put( ServerVars.HTTPS, request.isSecure() );
		_server.put( ServerVars.SESSION, request.getSession() );
		_server.put( ServerVars.SERVER_SOFTWARE, "Chiori Web Server" );
		_server.put( ServerVars.SERVER_ADMIN, Loader.getConfig().getString( "server.admin", "webmaster@" + request.getServerName() ) );
		_server.put( ServerVars.SERVER_ID, Loader.getConfig().getString( "server.id", "applebloom" ) );
		_server.put( ServerVars.SERVER_SIGNATURE, "Chiori Web Server Version " + Loader.getVersion() );
		
		RequestEvent event = new RequestEvent( _server );
		Loader.getPluginManager().callEvent( event );
		
		if ( event.isCancelled() )
		{
			Loader.getLogger().warning( "Navigation was cancelled by a plugin for ip '" + request.getRemoteAddr() + "' '" + request.getHeader( "Host" ) + request.getRequestURI() + "'" );
			
			int status = event.getStatus();
			String reason = event.getReason();
			
			if ( status < 400 && status > 599 )
			{
				status = 502;
				reason = "Navigation Cancelled by Internal Event";
			}
			
			response.sendError( status, reason );
			return;
		}
		
		if ( !rewriteVirtual( siteDomain, siteSubDomain, uri ) )
		{
			File siteRoot = new File( Loader.webroot, currentSite.siteId );
			
			if ( !siteRoot.exists() )
				siteRoot.mkdirs();
			
			if ( uri.isEmpty() )
				uri = "/";
			
			File dest = new File( siteRoot, uri );
			
			if ( uri.endsWith( ".php" ) || uri.endsWith( ".php5" ) || ( dest.isDirectory() && new File( dest, "index.php" ).exists() ) )
			{
				this.loadPageInternal( "", "", "", uri, "", "-1" );
			}
			else
			{
				String target = currentSite.getWebRoot( siteSubDomain ) + "/" + uri;
				Loader.getLogger().fine( "Forwarding request to the site (" + currentSite.siteId + ") webroot at '" + target + "'" );
				request.getRequestDispatcher( target ).forward( request, response );
			}
		}
	}
	
	public String replaceAt( String par, int at, String rep )
	{
		StringBuilder sb = new StringBuilder( par );
		sb.setCharAt( at, rep.toCharArray()[0] );
		return sb.toString();
	}
	
	public boolean rewriteVirtual( String domain, String subdomain, String uri )
	{
		try
		{
			// TODO: Fix the select issue with black subdomains. It's not suppose to be 1111 but it is to prevent the
			// redirect loop.
			ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '1111') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '" + subdomain + "' OR site = '1111') AND domain = '';" );
			
			if ( sql.getRowCount( rs ) > 0 )
			{
				Map<String, HashMap<String, Object>> candy = new TreeMap<String, HashMap<String, Object>>();
				
				do
				{
					HashMap<String, Object> data = new HashMap<String, Object>();
					
					String prop = rs.getString( "page" );
					
					if ( prop.startsWith( "/" ) )
						prop = prop.substring( 1 );
					
					data.put( "page", prop );
					
					String[] props = prop.split( "[.//]" );
					String[] uris = uri.split( "[.//]" );
					
					String weight = StringUtils.repeat( "?", Math.max( props.length, uris.length ) );
					
					boolean whole_match = true;
					for ( int i = 0; i < Math.max( props.length, uris.length ); i++ )
					{
						try
						{
							Loader.getLogger().fine( prop + " --> " + props[i] + " == " + uris[i] );
							
							if ( props[i].matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
							{
								weight = replaceAt( weight, i, "Z" );
								
								String key = props[i].replaceAll( "[\\[\\]=]", "" );
								String value = uris[i];
								
								rewriteGlobals.put( key, value );
								
								// PREP MATCH
								
								Loader.getLogger().fine( "Found a PREG match to " + rs.getString( "page" ) );
							}
							else if ( props[i].equals( uris[i] ) )
							{
								weight = replaceAt( weight, i, "A" );
								
								Loader.getLogger().fine( "Found a match to " + rs.getString( "page" ) );
								// MATCH
							}
							else
							{
								whole_match = false;
								Loader.getLogger().fine( "Found no match to " + rs.getString( "page" ) );
								break;
								// NO MATCH
							}
						}
						catch ( ArrayIndexOutOfBoundsException e )
						{
							whole_match = false;
							break;
						}
						catch ( Exception e )
						{
							e.printStackTrace();
							whole_match = false;
							break;
						}
					}
					
					if ( whole_match )
					{
						data.put( "page", rs.getString( "page" ) );
						data.put( "title", rs.getString( "title" ) );
						data.put( "reqlevel", rs.getString( "reqlevel" ) );
						data.put( "theme", rs.getString( "theme" ) );
						data.put( "view", rs.getString( "view" ) );
						data.put( "html", rs.getString( "html" ) );
						data.put( "file", rs.getString( "file" ) );
						
						candy.put( weight, data );
					}
				}
				while ( rs.next() );
				
				if ( candy.size() > 0 )
				{
					HashMap<String, Object> data = (HashMap) candy.values().toArray()[0];
					
					Loader.getLogger().info( "Rewriting page request to " + data );
					
					try
					{
						loadPageInternal( (String) data.get( "theme" ), (String) data.get( "view" ), (String) data.get( "title" ), (String) data.get( "file" ), (String) data.get( "html" ), (String) data.get( "reqlevel" ) );
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}
					
					return true;
				}
				
				Loader.getLogger().fine( "Failed to find a page redirect for Framework Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
				return false;
			}
			else
			{
				Loader.getLogger().fine( "Failed to find a page redirect for Framework Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
				return false;
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace(); // TODO: Better this catch
		}
		
		return true;
	}
	
	private void executeCodeSimple( Env env, String code )
	{
		StringValue sv = new LargeStringBuilderValue();
		sv.append( "?>" + code );
		
		try
		{
			env.evalCode( sv );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	// TODO: Create a loadPage for framework use
	protected void loadPageInternal( String theme, String view, String title, String file, String html, String reqPerm ) throws IOException
	{
		QuercusContext quercus = getQuercus();
		
		WriteStream ws = null;
		env = null;
		
		try
		{
			if ( currentSite == null )
				return;
			
			if ( html.isEmpty() && file.isEmpty() )
			{
				response.sendError( 500, "There was a problem handling your request. Try again later." );
				return;
			}
			
			File requestFile = null;
			if ( !file.isEmpty() )
			{
				if ( file.startsWith( "/" ) )
					file = file.substring( 1 );
				
				if ( currentSite.protectCheck( file ) )
				{
					Loader.getLogger().warning( "Loading of page '" + file + "' is not allowed since its hard protected in the site configs." );
					response.sendError( 401, "Loading of this page is not allowed since its hard protected in the site configs." );
					return;
				}
				
				if ( Loader.webroot.isEmpty() )
					Loader.webroot = "webroot";
				
				File siteRoot = new File( Loader.webroot, currentSite.siteId );
				
				if ( !siteRoot.exists() )
					siteRoot.mkdirs();
				
				requestFile = new File( siteRoot, file );
				
				Loader.getLogger().info( "Requesting file: " + requestFile.getAbsolutePath() );
				
				if ( !requestFile.exists() )
				{
					Loader.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
					response.sendError( 404 );
					return;
				}
				
				if ( requestFile.isDirectory() )
				{
					if ( new File( requestFile, "index.php" ).exists() )
					{
						requestFile = new File( requestFile, "index.php" );
					}
					else if ( new File( requestFile, "index.html" ).exists() )
					{
						requestFile = new File( requestFile, "index.html" );
					}
					else
					{
						response.sendError( 500, "There was a problem finding the index page." );
						return;
					}
				}
				
				if ( !requestFile.exists() )
				{
					Loader.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
					response.sendError( HttpServletResponse.SC_NOT_FOUND );
					return;
				}
			}
			
			try
			{
				QuercusPage page = null;
				Path requestPath = null;
				if ( ( requestFile != null ) )
				{
					requestPath = new FilePath( requestFile.getAbsolutePath() );
					page = quercus.parse( requestPath );
				}
				
				ws = Vfs.openWrite( out );
				ws.setDisableCloseSource( true );
				ws.setNewlineString( "\n" );
				
				env = quercus.createEnv( page, ws, request, response );
				env.setPwd( ( requestPath == null ) ? new FilePath( new File( Loader.webroot ).getAbsolutePath() ) : requestPath.getParent() );
				
				env.start();
				
				env.setGlobalValue( "chiori", env.wrapJava( this ) );
				
				executeCodeSimple( env, "<?php function getFramework(){return $GLOBALS[\"chiori\"];} ?>" );
				
				executeCodeSimple( env, "<?php $_SERVER[\"DOCUMENT_ROOT\"] = \"" + new File( Loader.getConfig().getString( "settings.webroot", "webroot" ), currentSite.getWebRoot( siteSubDomain ) ).getAbsolutePath() + "\"; ?>" );
				
				if ( rewriteGlobals != null && rewriteGlobals.size() > 0 )
				{
					for ( Entry<String, String> entry : rewriteGlobals.entrySet() )
					{
						StringValue sv = new LargeStringBuilderValue();
						sv.append( "?>" + entry.getValue() );
						env.setGlobalValue( entry.getKey(), sv );
						executeCodeSimple( env, "<?php $_REQUEST[" + entry.getKey() + "] = \"" + entry.getValue() + "\"; $_POST[" + entry.getKey() + "] = \"" + entry.getValue() + "\"; $_GET[" + entry.getKey() + "] = \"" + entry.getValue() + "\"; ?>" );
					}
				}
				
				if ( getUserService().initalize( reqPerm ) )
				{
					if ( !html.isEmpty() )
						executeCodeSimple( env, html );
					
					if ( requestFile != null )
						try
						{
							env.executeTop();
						}
						catch ( QuercusExitException e )
						{
							throw e;
						}
						catch ( QuercusErrorException e )
						{
							throw e;
						}
						catch ( Throwable e )
						{
							// XXX: Also catch Quercus Exceptions
							generateError( e );
							e.printStackTrace();
						}
				}
				
				String source;
				if ( continueNormally )
				{
					env.flush();
					ws.flush();
					
					source = new String( out.toByteArray(), "ISO-8859-1" );
					out.reset();
				}
				else
				{
					currentSite = _sites.getSiteById( "framework" );
					
					if ( currentSite instanceof FrameworkSite )
						( (FrameworkSite) currentSite ).setDatabase( sql );
					
					source = alternateOutput;
					theme = "com.chiorichan.themes.error";
					view = "";
					
					env.flush();
					ws.flush();
					out.reset();
				}
				
				RenderEvent event = new RenderEvent( this, source );
				
				event.theme = theme;
				event.view = view;
				event.title = title;
				
				Loader.getPluginManager().callEvent( event );
				
				if ( event.sourceChanged() )
					source = event.getSource();
				
				response.setContentLength( source.getBytes("ISO-8859-1").length );
				response.getOutputStream().write( source.getBytes( "ISO-8859-1" ) );
				
				getUserService().saveSession();
			}
			catch ( QuercusExitException e )
			{
				throw e;
			}
			catch ( QuercusErrorException e )
			{
				throw e;
			}
			catch ( QuercusLineRuntimeException e )
			{
				Loader.getLogger().log( Level.FINE, e.toString(), e );
				response.sendError( 500, e.getMessage() );
			}
			catch ( QuercusValueException e )
			{
				Loader.getLogger().log( Level.FINE, e.toString(), e );
				response.sendError( 500, e.getMessage() );
			}
			catch ( Throwable e )
			{
				e.printStackTrace();
				
				if ( !response.isCommitted() )
					e.printStackTrace( ws.getPrintWriter() );
				
				if ( !response.isCommitted() )
					response.sendError( 500 );
			}
		}
		catch ( QuercusDieException e )
		{
			// normal exit
			Loader.getLogger().log( Level.FINE, e.toString(), e );
		}
		catch ( QuercusExitException e )
		{
			// normal exit
			Loader.getLogger().log( Level.FINE, e.toString(), e );
		}
		catch ( QuercusErrorException e )
		{
			// error exit
			Loader.getLogger().log( Level.FINE, e.toString(), e );
			response.sendError( 500, e.getMessage() );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			if ( env != null )
				env.close();
			
			if ( ws != null && env != null && env.getDuplex() == null )
				ws.close();
		}
	}
	
	public String loadPage( String theme, String view, String title, String file, String html, String reqPerm ) throws IOException
	{
		if ( html == null )
			html = "";
		
		if ( file == null )
			file = "";
		
		// TODO: Check reqlevel
		
		String source = html;
		
		if ( !file.isEmpty() )
		{
			if ( file.startsWith( "/" ) )
				file = file.substring( 1 );
			
			if ( currentSite.protectCheck( file ) )
				throw new IOException( "Loading of this page is not allowed since its hard protected in the site configs." );
			
			File requestFile = new File( currentSite.getWebRoot( siteSubDomain ), file );
			
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
		
		RenderEvent event = new RenderEvent( this, source );
		
		event.theme = theme;
		event.view = view;
		event.title = title;
		
		Loader.getPluginManager().callEvent( event );
		
		if ( event.sourceChanged() )
			source = event.getSource();
		
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

	public static void initalizeFramework()
	{
		YamlConfiguration config = Loader.getConfig();
		
		switch ( config.getString( "framework-database.type", "mysql" ) )
		{
			case "mysql":
				String host = config.getString( "framework.database.host", "localhost" );
				String port = config.getString( "framework.database.port", "3306" );
				String database = config.getString( "framework.database.database", "chiorifw" );
				String username = config.getString( "framework.database.user", "fwuser" );
				String password = config.getString( "framework.database.pass", "fwpass" );
				
				try
				{
					sql.init( database, username, password, host, port );
				}
				catch ( SQLException e )
				{
					e.printStackTrace();
					Loader.getLogger().severe( e.getMessage() );
					System.exit( 1 );
				}
				catch ( ClassNotFoundException e )
				{
					Loader.getLogger().severe( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
					System.exit( 1 );
				}
				catch ( ConnectException e )
				{
					e.printStackTrace();
					Loader.getLogger().severe( "We had a problem connecting to the database host '" + host + "'" );
					System.exit( 1 );
				}
				
				break;
			default:
				Loader.getLogger().severe( "The Framework Database can not be anything other then mySql at the moment. Please change 'framework-database.type' to 'mysql' in 'chiori.yml'" );
				System.exit( 1 );
		}
		
		_sites.loadSites();
	}
	
	protected Path getPath( HttpServletRequest req )
	{
		Path pwd = getQuercus().getPwd().copy();
		
		String servletPath = QuercusRequestAdapter.getPageServletPath( req );
		
		if ( servletPath.startsWith( "/" ) )
			servletPath = servletPath.substring( 1 );
		
		Path path = pwd.lookupChild( servletPath );
		
		if ( path.isFile() )
			return path;
		
		StringBuilder sb = new StringBuilder();
		
		sb.append( servletPath );
		
		String pathInfo = QuercusRequestAdapter.getPagePathInfo( req );
		
		if ( pathInfo != null )
		{
			sb.append( pathInfo );
		}
		
		String scriptPath = sb.toString();
		
		path = pwd.lookupChild( scriptPath );
		
		return path;
	}
	
	protected WriteStream openWrite( HttpServletResponse response ) throws IOException
	{
		WriteStream ws;
		OutputStream out = response.getOutputStream();
		ws = Vfs.openWrite( out );
		return ws;
	}
	
	protected QuercusContext getQuercus()
	{
		if ( _quercus == null )
		{
			_quercus = new QuercusContext();
			
			_quercus.setServletContext( _servletContext );
			
			Path pwd = new FilePath( _servletContext.getRealPath( "/" ) );
			Path webInfDir = new FilePath( _servletContext.getRealPath( "/WEB-INF" ) );
			
			_quercus.setPwd( pwd );
			_quercus.setWebInfDir( webInfDir );
			
			_quercus.init();
			_quercus.start();
		}
		
		return _quercus;
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
	
	public FrameworkUserService getUserService()
	{
		if ( _usr == null )
			_usr = new FrameworkUserService( this );
		
		return _usr;
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
	
	public Site getCurrentSite()
	{
		return currentSite;
	}
	
	public String getRequestId()
	{
		return requestId;
	}
	
	public HttpServletResponse getResponse()
	{
		return response;
	}
	
	public HttpServletRequest getRequest()
	{
		return request;
	}
	
	public FilterChain getChain()
	{
		return chain;
	}
	
	public String getProduct()
	{
		return "Chiori Web Server (implementing Chiori Framework API)";
	}
	
	public String getVersion()
	{
		return Loader.getVersion();
	}
	
	public Env getEnv()
	{
		return env;
	}
	
	public ByteArrayOutputStream getOutputStream()
	{
		return out;
	}
	
	public static SqlConnector getDatabase()
	{
		return sql;
	}
	
	public void echo( String string )
	{
		executeCodeSimple( env, string );
	}
	
	public void generateError( String errStr )
	{
		generateError( 500, errStr );
	}
	
	public void generateError( Throwable t )
	{
		StringValue sv = new LargeStringBuilderValue();
		
		for ( StackTraceElement s : t.getStackTrace() )
		{
			sv.append( s + "\n" );
		}
		
		env.setGlobalValue( "stackTrace", sv );
		
		Loader.getLogger().warning( t.getMessage() );
		
		generateError( 500, t.getMessage() );
	}
	
	public void generateError( int errNo, String reason )
	{
		continueNormally = false;
		
		StringBuilder op = new StringBuilder();
		
		response.setStatus( errNo );
		
		op.append( "<h1>" + getServer().getStatusDescription( errNo ) + "</h1>\n" );
		op.append( "<p class=\"warning show\">" + reason + "</p>\n" );
		
		alternateOutput = op.toString();
		httpStatus = errNo;
	}
}
