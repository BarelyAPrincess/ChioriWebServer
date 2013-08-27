package com.chiorichan.framework;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
import com.caucho.quercus.parser.QuercusParseException;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.chiorichan.Main;
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
	
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected FilterChain chain;
	protected QuercusContext quercus;
	protected ServletContext _servletContext;
	
	protected FrameworkServer _server;
	protected FrameworkConfigurationManager _config;
	
	protected Env env = null;
	protected ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	protected String siteId, siteTitle, siteDomain, siteSubDomain, requestId;
	protected Site currentSite;
	
	public Framework(HttpServletRequest request0, HttpServletResponse response0, FilterChain chain0, QuercusContext quercus0, ServletContext servletContext)
	{
		request = request0;
		response = response0;
		chain = chain0;
		quercus = quercus0;
		_servletContext = servletContext;
	}
	
	public void generateError( int errNo, String reason )
	{
		// TODO: Generate error temp from framework resources. ie. Templates
		/*
		 * $this->server->Error("&4" . $e->getMessage()); $GLOBALS["lasterr"] = $e; //$GLOBALS["stackTrace"] =
		 * $e->stackTrace();
		 * 
		 * $plugin = $this->createPlugin( "com.chiorichan.plugin.Template" ); $plugin->loadPage (
		 * "com.chiorichan.themes.error", "", "", "/panic.php" );
		 */
		
		try
		{
			response.sendError( errNo, reason );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	public void init() throws IOException, ServletException
	{
		String uri = request.getRequestURI();
		String domain = request.getServerName();
		String site = "";
		
		if ( uri.startsWith( "/" ) )
			uri = uri.substring( 1 );
		
		if ( domain.equalsIgnoreCase( "localhost" ) || domain.equalsIgnoreCase( "127.0.0.1" ) | domain.equalsIgnoreCase( request.getLocalAddr() ) )
			domain = "web.applebloom.co"; // domain = "";
			
		if ( domain.split( "\\." ).length > 2 )
		{
			String[] r = domain.split( "\\.", 2 );
			site = r[0];
			domain = r[1];
		}
		
		currentSite = _sites.getSiteByDomain( domain );
		
		// TODO: Generate a blank site and save it based on the information known
		if ( currentSite == null )
			currentSite = new Site( "default", Main.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" ), domain );
		
		siteDomain = domain;
		siteSubDomain = site;
		
		requestId = DigestUtils.md5Hex( request.getSession().getId() + request.getRemoteAddr() + uri );
		
		Map<ServerVars, Object> _server = new HashMap<ServerVars, Object>();
		// _server.put( ServerVars.PHP_SELF, requestFile.getPath() );
		_server.put( ServerVars.DOCUMENT_ROOT, Main.getConfig().getString( "settings.webroot", "webroot" ) );
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
		_server.put( ServerVars.SERVER_ADMIN, Main.getConfig().getString( "server.admin", "webmaster@" + request.getServerName() ) );
		_server.put( ServerVars.SERVER_ID, Main.getConfig().getString( "server.id", "applebloom" ) );
		_server.put( ServerVars.SERVER_SIGNATURE, "Chiori Web Server Version " + Main.getVersion() );
		
		RequestEvent event = new RequestEvent( _server );
		Main.getPluginManager().callEvent( event );
		
		if ( event.isCancelled() )
		{
			Main.getLogger().warning( "Navigation was cancelled by a plugin for ip '" + request.getRemoteAddr() + "' '" + request.getHeader( "Host" ) + request.getRequestURI() + "'" );
			
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
			String target = currentSite.getWebRoot( siteSubDomain ) + "/" + uri;
			
			Main.getLogger().info( "Forwarding request to the site (" + currentSite.siteId + ") webroot at '" + target + "'" );
			
			request.getRequestDispatcher( target ).forward( request, response );
			
			// chain.doFilter( request, response );
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
			ResultSet rs = sql.query( "SELECT * FROM `pages` WHERE (site = '' OR site = '" + subdomain + "') AND domain = '" + domain + "' UNION SELECT * FROM `pages` WHERE (site = '' OR site = '" + subdomain + "') AND domain = '';" );
			
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
							Main.getLogger().fine( prop + " --> " + props[i] + " == " + uris[i] );
							
							if ( props[i].matches( "\\[([a-zA-Z0-9]+)=\\]" ) )
							{
								weight = replaceAt( weight, i, "Z" );
								
								String key = props[i].replaceAll( "[\\[\\]=]", "" );
								String value = uris[i];
								
								// TODO: Tell framework to set these values.
								
								// PREP MATCH
								
								Main.getLogger().fine( "Found a PREG match to " + rs.getString( "page" ) );
							}
							else if ( props[i].equals( uris[i] ) )
							{
								weight = replaceAt( weight, i, "A" );
								
								Main.getLogger().fine( "Found a match to " + rs.getString( "page" ) );
								// MATCH
							}
							else
							{
								whole_match = false;
								Main.getLogger().fine( "Found no match to " + rs.getString( "page" ) );
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
					
					Main.getLogger().info( "Rewriting page request to " + data );
					
					try
					{
						loadPage( (String) data.get( "theme" ), (String) data.get( "view" ), (String) data.get( "title" ), (String) data.get( "file" ), (String) data.get( "html" ), (String) data.get( "reqlevel" ) );
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}
					
					return true;
				}
				
				Main.getLogger().warning( "Failed to find a page redirect for Framework Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
				return false;
			}
			else
			{
				Main.getLogger().warning( "Failed to find a page redirect for Framework Rewrite... '" + subdomain + "." + domain + "' '" + uri + "'" );
				return false;
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace(); // TODO: Better this catch
		}
		
		return true;
	}
	
	private void loadPage( String theme, String view, String title, String file, String html, String reqlevel ) throws IOException
	{
		quercus.setServletContext( _servletContext );
		
		WriteStream ws = null;
		
		try
		{
			if ( currentSite == null )
				return;
			
			//boolean authorized = true; // Set according to the results of checking the reqlevel.
			
			Path requestPath = null;
			
			if ( html.isEmpty() && file.isEmpty() )
			{
				response.sendError( 500 );
				return;
			}
			
			File requestFile = null;
			if ( html.isEmpty() && !file.isEmpty() )
			{
				if ( file.startsWith( "/" ) )
					file = file.substring( 1 );
				
				if ( currentSite.protectCheck( file ) )
				{
					Main.getLogger().warning( "Loading of page '" + file + "' is not allowed since its hard protected in the site configs." );
					response.sendError( 401, "Loading of this page is not allowed since its hard protected in the site configs." );
					return;
				}
				
				if ( Main.webroot.isEmpty() )
					Main.webroot = "webroot";
				
				File siteRoot = new File( Main.webroot, currentSite.siteId );
				
				if ( !siteRoot.exists() )
					siteRoot.mkdirs();
				
				Main.getLogger().info( "Requesting file: " + new File( siteRoot, file ).getAbsolutePath() );
				
				requestFile = new File( siteRoot, file );
				
				if ( !requestFile.exists() )
				{
					response.sendError( 500 );
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
						response.sendError( 500 );
						return;
					}
				}
				
				if ( !requestFile.exists() )
				{
					Main.getLogger().warning( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
					response.sendError( HttpServletResponse.SC_NOT_FOUND );
					return;
				}
			}
			
			try
			{
				Path path = new FilePath( requestFile.getAbsolutePath() );
				QuercusPage page = ( requestFile != null ) ? quercus.parse( path ) : null;
				
				ws = Vfs.openWrite( out );
				ws.setDisableCloseSource( true );
				ws.setNewlineString( "\n" );
				
				env = quercus.createEnv( page, ws, request, response );
				env.setPwd( ( requestPath == null ) ? new FilePath( new File( Main.webroot ).getAbsolutePath() ) : requestPath.getParent() );
				
				env.start();
				
				env.setGlobalValue( "chiori", env.wrapJava( this ) );
				
				String source = "<?php function getFramework(){return $GLOBALS[\"chiori\"];} ?>";
				
				if ( !html.isEmpty() )
					source += html;
				
				StringValue sv = new LargeStringBuilderValue();
				sv.append( "?> " + source );
				
				try
				{
					env.evalCode( sv );
				}
				catch ( IOException | QuercusParseException e )
				{
					e.printStackTrace();
				}
				
				if ( requestFile != null )
					env.executeTop();
				
				ws.flush();
				source = new String( out.toByteArray() );
				out.reset();
				
				RenderEvent event = new RenderEvent( this, source );
				
				event.theme = theme;
				event.view = view;
				event.title = title;
				
				Main.getPluginManager().callEvent( event );
				
				if ( event.sourceChanged() )
					source = event.getSource();
				
				response.getOutputStream().write( source.getBytes() );
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
				Main.getLogger().log( Level.FINE, e.toString(), e );
				response.sendError( 500, e.getMessage() );
			}
			catch ( QuercusValueException e )
			{
				Main.getLogger().log( Level.FINE, e.toString(), e );
				response.sendError( 500, e.getMessage() );
			}
			catch ( Throwable e )
			{
				e.printStackTrace();
				
				if ( !response.isCommitted() )
					e.printStackTrace( ws.getPrintWriter() );
				
				response.sendError( 500 );
			}
		}
		catch ( QuercusDieException e )
		{
			// normal exit
			Main.getLogger().log( Level.FINE, e.toString(), e );
		}
		catch ( QuercusExitException e )
		{
			// normal exit
			Main.getLogger().log( Level.FINE, e.toString(), e );
		}
		catch ( QuercusErrorException e )
		{
			// error exit
			Main.getLogger().log( Level.FINE, e.toString(), e );
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
	
	public static void initalizeFramework()
	{
		YamlConfiguration config = Main.getConfig();
		
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
					Main.getLogger().severe( e.getMessage() );
					System.exit( 1 );
				}
				catch ( ClassNotFoundException e )
				{
					Main.getLogger().severe( "We could not locate the 'com.mysql.jdbc.Driver' library regardless that its suppose to be included. If your running from source code be sure to have this library in your build path." );
					System.exit( 1 );
				}
				catch ( ConnectException e )
				{
					e.printStackTrace();
					Main.getLogger().severe( "We had a problem connecting to the database host '" + host + "'" );
					System.exit( 1 );
				}
				
				break;
			default:
				Main.getLogger().severe( "The Framework Database can not be anything other then mySql at the moment. Please change 'framework-database.type' to 'mysql' in 'chiori.yml'" );
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
		return quercus;
	}
	
	public FrameworkServer getServer()
	{
		if ( _server == null )
			_server = new FrameworkServer( this );
		
		return _server;
	}
	
	public PluginManager getPluginManager()
	{
		return Main.getPluginManager();
	}
	
	public FrameworkConfigurationManager getConfigurationManager()
	{
		if ( _config == null )
			_config = new FrameworkConfigurationManager( request, response, chain, requestId, currentSite );
		
		return _config;
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
		return "Chiori Web Server (implementing Chiori Framework)";
	}
	
	public String getVersion()
	{
		return Main.getVersion();
	}

	public Env getEnv()
	{
		return env;
	}

	public ByteArrayOutputStream getOutputStream()
	{
		return out;
	}
}