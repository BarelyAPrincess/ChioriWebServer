package com.chiorichan.framework;

import com.chiorichan.ConsoleLogManager;
import com.chiorichan.Loader;
import com.chiorichan.database.DatabaseEngine;
import com.chiorichan.file.YamlConfiguration;
import com.chiorichan.http.HttpCode;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;
import com.chiorichan.http.PersistenceManager;
import com.chiorichan.http.PersistentSession;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.util.Versioning;

public class Framework
{
	// protected boolean continueNormally = true;
	// protected String alternateOutput = "An Unknown Error Has Risen!";
	
	protected final PersistentSession sess;
	
	public Framework(PersistentSession _sess)
	{
		sess = _sess;
	}
	
	public UserServiceWrapper getUserService()
	{
		return new UserServiceWrapper( sess.getCurrentUser() );
	}
	
	public ConfigurationManagerWrapper getConfigurationManager()
	{
		return new ConfigurationManagerWrapper( sess );
	}
	
	@SuppressWarnings( "deprecation" )
	public ServerUtilsWrapper getServerUtils()
	{
		return new ServerUtilsWrapper( sess );
	}
	
	public HttpUtilsWrapper getHttpUtils()
	{
		return new HttpUtilsWrapper( sess );
	}
	
	public DatabaseEngine getServerDatabase()
	{
		return new DatabaseEngine( Loader.getPersistenceManager().getSql() );
	}
	
	public DatabaseEngine getSiteDatabase()
	{
		return new DatabaseEngine( sess.getRequest().getSite().getDatabase() );
	}
	
	public PersistentSession getSession()
	{
		return sess;
	}
	
	public HttpRequest getRequest()
	{
		return sess.getRequest();
	}
	
	public HttpResponse getResponse()
	{
		return sess.getResponse();
	}
	
	public Site getSite()
	{
		return sess.getRequest().getSite();
	}
	
	public String getProduct()
	{
		return Versioning.getProduct();
	}
	
	public String getVersion()
	{
		return Versioning.getVersion();
	}
	
	public String getCopyright()
	{
		return Versioning.getCopyright();
	}
	
	public PluginManager getPluginManager()
	{
		return Loader.getPluginManager();
	}
	
	public ConsoleLogManager getLogger()
	{
		return Loader.getLogger();
	}
	
	public String getStatusDescription( int errNo )
	{
		return HttpCode.msg( errNo );
	}
	
	public PersistenceManager getPersistenceManager()
	{
		return Loader.getPersistenceManager();
	}
	
	/*
	 * public String codeSampleEval( String code, int line )
	 * {
	 * return codeSampleEval( code, line, -1 );
	 * }
	 * public String codeSampleEval( String code, int line, int col )
	 * {
	 * try
	 * {
	 * StringBuilder sb = new StringBuilder();
	 * if ( code.isEmpty() )
	 * return "";
	 * int cLine = 0;
	 * for ( String l : code.split( "\n" ) )
	 * {
	 * cLine++;
	 * if ( cLine > line - 5 && cLine < line + 5 )
	 * {
	 * if ( cLine == line )
	 * {
	 * if ( l.length() >= col )
	 * {
	 * if ( col > -1 )
	 * {
	 * if ( col - 1 > -1 )
	 * col--;
	 * l = escapeHTML( l.substring( 0, col ) ) + "<span style=\"background-color: red; font-weight: bolder;\">" + l.substring( col, col + 1 ) + "</span>" + escapeHTML( l.substring( col + 1 ) );
	 * }
	 * else
	 * l = escapeHTML( l );
	 * sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
	 * }
	 * else
	 * sb.append( "<span class=\"ln\">" + cLine + "</span> " + escapeHTML( l ) + "\n" ); // XXX: Fix It.
	 * // Why does
	 * // this happen?
	 * }
	 * else
	 * {
	 * sb.append( "<span class=\"ln\">" + cLine + "</span> " + escapeHTML( l ) + "\n" );
	 * }
	 * }
	 * }
	 * return sb.toString().substring( 0, sb.toString().length() - 1 );
	 * }
	 * catch ( Exception e )
	 * {
	 * return "Am exception was thrown while preparing code preview: " + e.getMessage();
	 * }
	 * }
	 * public String codeSample( String file, int line )
	 * {
	 * return codeSample( file, line, -1 );
	 * }
	 * public String codeSample( String file, int line, int col )
	 * {
	 * if ( !file.isEmpty() )
	 * {
	 * FileInputStream is;
	 * try
	 * {
	 * is = new FileInputStream( file );
	 * }
	 * catch ( FileNotFoundException e )
	 * {
	 * return e.getMessage();
	 * }
	 * StringBuilder sb = new StringBuilder();
	 * try
	 * {
	 * BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
	 * int cLine = 0;
	 * String l;
	 * while ( ( l = br.readLine() ) != null )
	 * {
	 * l = escapeHTML( l ) + "\n";
	 * cLine++;
	 * if ( cLine > line - 5 && cLine < line + 5 )
	 * {
	 * if ( cLine == line )
	 * {
	 * sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
	 * }
	 * else
	 * {
	 * sb.append( "<span class=\"ln\">" + cLine + "</span> " + l );
	 * }
	 * }
	 * }
	 * is.close();
	 * }
	 * catch ( IOException e )
	 * {
	 * e.printStackTrace();
	 * }
	 * return sb.toString().substring( 0, sb.toString().length() - 1 );
	 * }
	 * return "";
	 * }
	 * public String javaException( StackTraceElement[] ste )
	 * {
	 * if ( ste == null || ste.length < 1 )
	 * return "";
	 * int l = 0;
	 * StringBuilder sb = new StringBuilder();
	 * for ( StackTraceElement e : ste )
	 * {
	 * String file = ( e.getFileName() == null ) ? "eval()" : e.getFileName() + "(" + e.getLineNumber() + ")";
	 * sb.append( "<tr class=\"trace " + ( ( e.getClassName().startsWith( "com.chiori" ) || e.getClassName().startsWith( "org.eclipse" ) || e.getClassName().startsWith( "java" ) ) ? "core" : "app" ) + " collapsed\">\n" );
	 * sb.append( "	<td class=\"number\">#" + l + "</td>\n" );
	 * sb.append( "	<td class=\"content\">\n" );
	 * sb.append( "		<div class=\"trace-file\">\n" );
	 * sb.append( "			<div class=\"plus\">+</div>\n" );
	 * sb.append( "			<div class=\"minus\">–</div>\n" );
	 * sb.append( "			" + file + ": <strong>" + e.getClassName() + "." + e.getMethodName() + "</strong>\n" );
	 * sb.append( "		</div>\n" );
	 * sb.append( "		<div class=\"code\">\n" );
	 * sb.append( "			<pre>Sorry, Code previews are not currently available.</pre>\n" );
	 * // sb.append( "			<pre><? codeSamp($trc["file"], $trc["line"]); ?></pre>\n" );
	 * sb.append( "		</div>\n" );
	 * sb.append( "	</td>\n" );
	 * sb.append( "</tr>\n" );
	 * l++;
	 * }
	 * return sb.toString();
	 * }
	 * public String errorPage( Throwable t )
	 * {
	 * // TODO: Make this whole thing better. Optional fancy error pages.
	 * if ( !response.isCommitted() )
	 * {
	 * Site currentSite = Loader.getPersistenceManager().getSiteManager().getSiteById( "framework" );
	 * if ( currentSite instanceof FrameworkSite )
	 * ( (FrameworkSite) currentSite ).setDatabase( Loader.getPersistenceManager().getSql() );
	 * try
	 * {
	 * env.set( "stackTrace", t );
	 * env.set( "codeSample", "Sorry, No code preview is available at this time." );
	 * if ( t instanceof CodeParsingException )
	 * {
	 * if ( ( (CodeParsingException) t ).getLineNumber() > -1 )
	 * env.set( "codeSample", codeSampleEval( ( (CodeParsingException) t ).getSourceCode(), ( (CodeParsingException) t ).getLineNumber(), ( (CodeParsingException) t ).getColumnNumber() ) );
	 * }
	 * env.set( "stackTraceHTML", javaException( t.getStackTrace() ) );
	 * // String page = ? "/panic.php" : "/notfound.php";
	 * return loadPage( "com.chiorichan.themes.error", "", "Critical Server Exception", "/panic.php", "", "-1" );
	 * }
	 * catch ( Exception e1 )
	 * {
	 * e1.printStackTrace();
	 * response.sendError( 500, "Critical Server Exception while loading the Framework Fancy Error Page: " + e1.getMessage() );
	 * }
	 * }
	 * return t.getMessage();
	 * }
	 * public String loadPage( String theme, String view, String title, String file, String html, String reqPerm ) throws IOException, CodeParsingException
	 * {
	 * if ( html == null )
	 * html = "";
	 * if ( file == null )
	 * file = "";
	 * // TODO: Check reqlevel
	 * String source = html;
	 * Site currentSite = request.getSite();
	 * if ( !file.isEmpty() )
	 * {
	 * if ( file.startsWith( "/" ) )
	 * file = file.substring( 1 );
	 * if ( currentSite.protectCheck( file ) )
	 * throw new IOException( "Loading of this page is not allowed since its hard protected in the site configs." );
	 * if ( Loader.webroot.isEmpty() )
	 * Loader.webroot = "webroot";
	 * File siteRoot = new File( Loader.webroot, currentSite.siteId );
	 * if ( !siteRoot.exists() )
	 * siteRoot.mkdirs();
	 * File requestFile = new File( siteRoot, file );
	 * Loader.getLogger().info( "Requesting file: " + requestFile.getAbsolutePath() );
	 * if ( !requestFile.exists() )
	 * throw new IOException( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
	 * if ( requestFile.isDirectory() )
	 * if ( new File( requestFile, "index.php" ).exists() )
	 * requestFile = new File( requestFile, "index.php" );
	 * else if ( new File( requestFile, "index.html" ).exists() )
	 * requestFile = new File( requestFile, "index.html" );
	 * else
	 * throw new IOException( "There was a problem finding the index page." );
	 * if ( !requestFile.exists() )
	 * {
	 * throw new IOException( "Could not load file '" + requestFile.getAbsolutePath() + "'" );
	 * }
	 * source = getFileContents( requestFile.getAbsolutePath() );
	 * source = getServer().executeCode( source );
	 * }
	 * /*
	 * RenderEvent event = new RenderEvent( this, source );
	 * event.theme = theme;
	 * event.view = view;
	 * event.title = title;
	 * Loader.getPluginManager().callEvent( event );
	 * if ( event.sourceChanged() )
	 * source = event.getSource();
	 * return source;
	 * }
	 * public void generateError( String errStr )
	 * {
	 * generateError( 500, errStr );
	 * }
	 * public void generateError( Throwable t )
	 * {
	 * StringBuilder sb = new StringBuilder();
	 * for ( StackTraceElement s : t.getStackTrace() )
	 * {
	 * sb.append( s + "\n" );
	 * }
	 * env.set( "stackTrace", sb.toString() );
	 * Loader.getLogger().warning( t.getMessage() );
	 * generateError( 500, t.getMessage() );
	 * }
	 * public void generateError( int errNo, String reason )
	 * {
	 * Loader.getLogger().warning( "Generating Error (" + errNo + "): " + reason );
	 * continueNormally = false;
	 * StringBuilder op = new StringBuilder();
	 * response.setStatus( errNo );
	 * op.append( "<h1>" + getServer().getStatusDescription( errNo ) + "</h1>\n" );
	 * op.append( "<p class=\"warning show\">" + reason + "</p>\n" );
	 * alternateOutput = op.toString();
	 * httpStatus = errNo;
	 * }
	 */
}
