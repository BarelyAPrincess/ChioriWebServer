package com.chiorichan.plugin.builtin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.event.server.RequestEvent;
import com.chiorichan.framework.CodeParsingException;
import com.chiorichan.framework.Site;
import com.chiorichan.plugin.java.JavaPlugin;
import com.chiorichan.util.StringUtil;

public class Template extends JavaPlugin implements Listener
{
	String docType = "html";
	String pageMark = "<!-- PAGE DATA -->";
	
	public void onEnable()
	{
		Loader.getEventBus().registerEvents( this, this );
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler( priority = EventPriority.HIGHEST )
	public void onRequestEvent( RequestEvent event )
	{
		// event.setStatus( 418, "I'm a teapot!" );
		// event.setCancelled( true );
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
			file = new File( root, pack + ".chi" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".inc.groovy" );
		
		if ( !file.exists() )
			file = new File( root, pack + ".groovy" );
		
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
	
	@EventHandler( priority = EventPriority.LOWEST )
	public void onRenderEvent( RenderEvent event )
	{
		Site site = event.getSite();
		Map<String, String> fwVals = event.getPageData();
		
		if ( fwVals.get( "themeless" ) != null && fwVals.get( "themeless" ).equals( "true" ) )
			return;
		
		String theme = fwVals.get( "theme" );
		String view = fwVals.get( "view" );
		String title = fwVals.get( "title" );
		
		if ( theme == null )
			theme = "";
		
		if ( view == null )
			view = "";
		
		if ( theme.isEmpty() && view.isEmpty() )
			return;
		
		if ( theme.isEmpty() )
			theme = "com.chiorichan.themes.default";
		
		StringBuilder ob = new StringBuilder();
		
		ob.append( "<!DOCTYPE " + docType + ">\n" );
		ob.append( "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" );
		ob.append( "<head>\n" );
		ob.append( "<meta charset=\"utf-8\">\n" );
		
		if ( site.title == null )
			site.title = Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" );
		
		if ( title == null || title.isEmpty() )
			ob.append( "<title>" + site.title + "</title>\n" );
		else
			ob.append( "<title>" + title + " - " + site.title + "</title>\n" );
		
		for ( String tag : site.getMetatags() )
			ob.append( tag + "\n" );
		
		// Allow pages to disable the inclusion of common header
		if ( fwVals.get( "noCommons" ) == null || !StringUtil.isTrue( fwVals.get( "noCommons" ) ) )
		{
			ob.append( doInclude( domainToPackage( site.domain ) + ".includes.common", event ) + "\n" );
			ob.append( doInclude( domainToPackage( site.domain ) + ".includes." + getPackageName( theme ), event ) + "\n" );
		}
		
		if ( fwVals.get( "header" ) != null && !fwVals.get( "header" ).isEmpty() )
			ob.append( doInclude( fwVals.get( "header" ), event ) + "\n" );
		
		ob.append( "</head>\n" );
		ob.append( "<body>\n" );
		
		String pageData = ( theme.isEmpty() ) ? pageMark : doInclude( theme, event );
		String viewData = ( view.isEmpty() ) ? pageMark : doInclude( view, event );
		
		if ( pageData.indexOf( pageMark ) < 0 )
			pageData = pageData + viewData;
		else
			pageData = pageData.replace( pageMark, viewData );
		
		if ( pageData.indexOf( pageMark ) < 0 )
			pageData = pageData + event.getSource();
		else
			pageData = pageData.replace( pageMark, event.getSource() );
		
		ob.append( pageData + "\n" );
		
		if ( fwVals.get( "footer" ) != null && !fwVals.get( "footer" ).isEmpty() )
			ob.append( doInclude( fwVals.get( "footer" ), event ) + "\n" );
		
		ob.append( "</body>\n" );
		ob.append( "</html>\n" );
		
		event.setSource( event.getSite().applyAlias( ob.toString() ) );
	}
	
	private String doInclude( String pack, RenderEvent event )
	{
		try
		{
			String source = event.getFramework().getHttpUtils().evalPackage( pack );
			try
			{
				source = event.getSession().getEvaling().parseForIncludes( source, event.getSite() );
			}
			catch ( CodeParsingException ex )
			{
				Loader.getLogger().warning( "Exception encountered during parsing for text based includes, unknown fault.", ex );
			}
			return source;
		}
		catch ( IOException | CodeParsingException e )
		{
			// TODO Improved error handling needed here.
			Loader.getLogger().warning( e.getMessage() );
		}
		
		return "";
	}
	
	public String getPackageParent( String pack )
	{
		if ( pack.indexOf( "." ) < 0 )
			return pack;
		
		String[] packs = pack.split( "\\.(?=[^.]*$)" );
		
		return packs[0];
	}
	
	public String getPackageName( String pack )
	{
		if ( pack.indexOf( "." ) < 0 )
			return pack;
		
		String[] packs = pack.split( "\\.(?=[^.]*$)" );
		
		return packs[1];
	}
	
	public String domainToPackage( String domain )
	{
		if ( domain == null || domain.isEmpty() )
			return "";
		
		String[] packs = domain.split( "\\." );
		
		List<String> lst = Arrays.asList( packs );
		Collections.reverse( lst );
		
		String pack = "";
		for ( String s : lst )
		{
			pack += "." + s;
		}
		
		return pack.substring( 1 );
	}
}
