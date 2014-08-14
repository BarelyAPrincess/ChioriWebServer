package com.chiorichan.plugin.builtin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.chiorichan.Loader;
import com.chiorichan.bus.events.EventHandler;
import com.chiorichan.bus.events.EventPriority;
import com.chiorichan.bus.events.Listener;
import com.chiorichan.bus.events.http.HttpExceptionEvent;
import com.chiorichan.bus.events.server.RenderEvent;
import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.framework.Site;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.plugin.java.JavaPlugin;
import com.chiorichan.util.StringUtil;

public class Template extends JavaPlugin implements Listener
{
	public void onEnable()
	{
		saveDefaultConfig();
		
		Loader.getEventBus().registerEvents( this, this );
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler( priority = EventPriority.HIGHEST )
	public void onHttpExceptionEvent( HttpExceptionEvent event )
	{
		event.setErrorHtml( ExceptionPageUtils.makeExceptionPage( event.getThrowable(), event.getRequest().getSession().getCodeFactory() ) );
	}
	
	@EventHandler( priority = EventPriority.LOWEST )
	public void onRenderEvent( RenderEvent event )
	{
		try
		{
			Site site = event.getSite();
			Map<String, String> fwVals = event.getPageData();
			
			if ( site == null )
				site = Loader.getSiteManager().getFrameworkSite();
			
			if ( fwVals.get( "themeless" ) != null && StringUtil.isTrue( fwVals.get( "themeless" ) ) )
				return;
			
			String theme = fwVals.get( "theme" );
			String view = fwVals.get( "view" );
			String title = fwVals.get( "title" );
			
			if ( theme == null )
				theme = "";
			
			if ( view == null )
				view = "";
			
			if ( theme.isEmpty() && view.isEmpty() && !getConfig().getBoolean( "config.alwaysRender" ) )
				return;
			
			if ( theme.isEmpty() )
				theme = "com.chiorichan.themes.default";
			
			StringBuilder ob = new StringBuilder();
			
			String docType = getConfig().getString( "config.defaultDocType", "html" );
			
			if ( fwVals.get( "docType" ) != null && !fwVals.get( "docType" ).isEmpty() )
				docType = fwVals.get( "docType" );
			
			ob.append( "<!DOCTYPE " + docType + ">\n" );
			ob.append( "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" );
			ob.append( "<head>\n" );
			ob.append( "<meta charset=\"utf-8\">\n" );
			
			if ( site == null && site.title == null )
				site.title = Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Chiori Framework Site" );
			
			if ( title == null || title.isEmpty() )
				ob.append( "<title>" + site.title + "</title>\n" );
			else
				ob.append( "<title>" + title + " - " + site.title + "</title>\n" );
			
			for ( String tag : site.getMetatags() )
				ob.append( tag + "\n" );
			
			// Allow pages to disable the inclusion of common header
			if ( ( fwVals.get( "noCommons" ) == null || !StringUtil.isTrue( fwVals.get( "noCommons" ) ) ) && !getConfig().getBoolean( "config.noCommons" ) )
			{
				ob.append( doInclude( domainToPackage( site.domain ) + ".includes.common", event ) + "\n" );
				ob.append( doInclude( domainToPackage( site.domain ) + ".includes." + getPackageName( theme ), event ) + "\n" );
			}
			
			if ( fwVals.get( "header" ) != null && !fwVals.get( "header" ).isEmpty() )
				ob.append( doInclude( fwVals.get( "header" ), event ) + "\n" );
			
			ob.append( "</head>\n" );
			ob.append( "<body>\n" );
			
			String pageMark = "<!-- " + getConfig().getString( "config.defaultTag", "PAGE DATA" ) + " -->";
			
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
			
			event.setSource( ob.toString() );
		}
		catch ( ShellExecuteException e )
		{
			event.setSource( ExceptionPageUtils.makeExceptionPage( e, event.getSession().getCodeFactory() ) );
			event.getResponse().setStatus( 500 );
		}
	}
	
	private String doInclude( String pack, RenderEvent event ) throws ShellExecuteException
	{
		try
		{
			CodeEvalFactory factory = event.getSession().getCodeFactory();
			
			return WebUtils.evalPackage( factory, event.getSite(), pack );
		}
		catch ( IOException ex )
		{
			Loader.getLogger().warning( "Exception encountered during include of package `" + pack + "`, unknown fault.", ex );
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
