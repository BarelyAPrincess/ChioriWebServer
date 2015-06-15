package com.chiorichan.plugin.template;

import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.chiorichan.Loader;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.HttpExceptionEvent;
import com.chiorichan.event.server.RenderEvent;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalFactoryResult;
import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.lang.ErrorReporting;
import com.chiorichan.lang.EvalException;
import com.chiorichan.lang.EvalMultipleException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.WebFunc;

/**
 * Chiori-chan's Web Server Template Plugin
 * 
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class Template extends Plugin implements Listener
{
	public void onEnable()
	{
		saveDefaultConfig();
		EventBus.INSTANCE.registerEvents( this, this );
	}
	
	public void onDisable()
	{
		
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onHttpExceptionEvent( HttpExceptionEvent event )
	{
		try
		{
			// We check if this exception was thrown from inside our plugin
			if ( ExceptionUtils.indexOfThrowable( event.getThrowable(), Template.class ) > -1 )
				return;
			
			EvalFactory factory = event.getRequest().getEvalFactory();
			
			// We initialize a temporary EvalFactory if the request did not contain one
			if ( factory == null )
				factory = EvalFactory.create( event.getRequest() );
			
			event.setErrorHtml( generateExceptionPage( event.getThrowable(), factory ) );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}
	
	public String generateExceptionPage( Throwable t, EvalFactory factory ) throws EvalException, EvalMultipleException, IOException
	{
		Validate.notNull( t );
		Validate.notNull( factory );
		
		StringBuilder ob = new StringBuilder();
		
		String fileName = "";
		int lineNum = -1;
		int colNum = -1;
		String className = null;
		
		String codeSample = "";
		ScriptTraceElement[] scriptTrace = null;
		
		if ( t instanceof EvalException )
		{
			scriptTrace = ( ( EvalException ) t ).getScriptTrace();
			
			if ( t.getCause() != null )
				t = t.getCause();
			
			if ( scriptTrace.length > 0 )
			{
				ScriptTraceElement ste = scriptTrace[0];
				
				lineNum = ste.getLineNumber();
				colNum = ste.getColumnNumber();
				
				className = ste.getClassName();
				String methodName = ste.getMethodName();
				
				if ( methodName != null && !methodName.isEmpty() )
					className += "." + methodName;
				
				if ( className.isEmpty() )
					className = null;
				
				EvalMetaData metaData = ste.getMetaData();
				fileName = metaData.fileName;
				
				if ( lineNum > -1 )
				{
					if ( metaData.source != null && !metaData.source.isEmpty() )
						codeSample += "<p>Pre-evaluated Code:</p><pre>" + TemplateUtils.generateCodePreview( metaData.source, lineNum, colNum ) + "</pre>";
					
					codeSample += "<p>Source Code:</p><pre>" + TemplateUtils.generateCodePreview( ste ) + "</pre>";
				}
			}
		}
		else
		{
			StackTraceElement ele;
			if ( t.getCause() == null )
				ele = t.getStackTrace()[0];
			else
				ele = t.getCause().getStackTrace()[0];
			
			fileName = ele.getFileName();
			lineNum = ele.getLineNumber();
			className = ele.getClassName() + "." + ele.getMethodName();
		}
		
		ob.append( "<h1>Exception Thrown</h1>\n" );
		ob.append( "<p class=\"message\">\n" );
		ob.append( t.getClass().getName() + ": " + t.getMessage() + "\n" );
		ob.append( "</p>\n" );
		ob.append( "\n" );
		ob.append( "<div class=\"source\">\n" );
		
		ob.append( "<p class=\"file\">" + fileName + ( ( lineNum > -1 ) ? "(" + lineNum + ( ( colNum > -1 ) ? ":" + colNum : "" ) + ")" : "" ) + ( ( className != null ) ? ": <strong>" + className + "</strong>" : "" ) + "</p>\n" );
		
		ob.append( "\n" );
		ob.append( "<div class=\"code\">\n" );
		if ( codeSample != null && !codeSample.isEmpty() )
			ob.append( codeSample + "\n" );
		ob.append( "</div>\n" );
		ob.append( "</div>\n" );
		ob.append( "\n" );
		ob.append( "<div class=\"traces\">\n" );
		ob.append( "<h2>Stack Trace</h2>\n" );
		ob.append( "<table style=\"width:100%;\">\n" );
		ob.append( TemplateUtils.formatStackTrace( t.getStackTrace(), scriptTrace ) + "\n" );
		ob.append( "</table>\n" );
		ob.append( "</div>\n" );
		ob.append( "\n" );
		ob.append( "<div class=\"version\">Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + "<br />" + Versioning.getCopyright() + "</div>\n" );
		
		EvalFactoryResult result = TemplateUtils.wrapAndEval( factory, ob.toString() );
		
		if ( result.hasExceptions() )
			ErrorReporting.throwExceptions( result.getExceptions() );
		
		return result.getString();
	}
	
	@EventHandler( priority = EventPriority.NORMAL )
	public void onRenderEvent( RenderEvent event ) throws IOException
	{
		try
		{
			Site site = event.getSite();
			Map<String, String> fwVals = event.getParams();
			
			if ( site == null )
				site = SiteManager.INSTANCE.getDefaultSite();
			
			if ( fwVals.get( "themeless" ) != null && StringFunc.isTrue( fwVals.get( "themeless" ) ) )
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
			
			// TODO return if the request is for a none text contentType
			
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
			
			String siteTitle;
			if ( site.getTitle() == null || site.getTitle().isEmpty() )
				siteTitle = Loader.getConfig().getString( "framework.sites.defaultTitle", "Unnamed Site" );
			else
				siteTitle = site.getTitle();
			
			if ( title == null || title.isEmpty() )
				ob.append( "<title>" + siteTitle + "</title>\n" );
			else if ( title.startsWith( "!" ) )
				ob.append( "<title>" + title.substring( 1 ) + "</title>\n" );
			else
				ob.append( "<title>" + title + " - " + siteTitle + "</title>\n" );
			
			for ( String tag : site.getMetatags() )
				ob.append( tag + "\n" );
			
			boolean showCommons = !getConfig().getBoolean( "config.noCommons" );
			
			if ( fwVals.get( "commons" ) != null )
				showCommons = StringFunc.isTrue( fwVals.get( "commons" ) );
			
			// Allow pages to disable the inclusion of common header
			if ( showCommons )
				ob.append( doInclude( domainToPackage( site.getDomain() ) + ".includes.common", event ) + "\n" );
			
			ob.append( doInclude( domainToPackage( site.getDomain() ) + ".includes." + getPackageName( theme ), event ) + "\n" );
			
			if ( fwVals.get( "header" ) != null && !fwVals.get( "header" ).isEmpty() )
				ob.append( doInclude( fwVals.get( "header" ), event ) + "\n" );
			
			ob.append( "</head>\n" );
			
			String pageMark = "<!-- " + getConfig().getString( "config.defaultTag", "PAGE DATA" ) + " -->";
			String pageData = "";
			String viewData = "";
			Map<String, String> params = new HashMap<String, String>( fwVals );
			
			if ( !theme.isEmpty() )
			{
				EvalFactoryResult result = doInclude0( theme, event );
				if ( result.isSuccessful() )
				{
					pageData = result.getString();
					params.putAll( result.getMeta().getParamStrings() );
				}
			}
			
			if ( !view.isEmpty() )
			{
				EvalFactoryResult result = doInclude0( view, event );
				if ( result.isSuccessful() )
				{
					viewData = result.getString();
					params.putAll( result.getMeta().getParamStrings() );
				}
			}
			
			ob.append( "<body" + ( ( params == null ) ? " " + params.get( "bodyArgs" ) : "" ) + ">\n" );
			
			if ( viewData != null && !viewData.isEmpty() )
				if ( pageData.indexOf( pageMark ) < 0 )
					pageData = pageData + viewData;
				else
					pageData = pageData.replace( pageMark, viewData );
			
			if ( pageData.indexOf( pageMark ) < 0 )
				pageData = pageData + StringFunc.byteBuf2String( event.getSource(), event.getEncoding() );
			else
				pageData = pageData.replace( pageMark, StringFunc.byteBuf2String( event.getSource(), event.getEncoding() ) );
			
			ob.append( pageData + "\n" );
			
			if ( fwVals.get( "footer" ) != null && !fwVals.get( "footer" ).isEmpty() )
				ob.append( doInclude( fwVals.get( "footer" ), event ) + "\n" );
			
			ob.append( "</body>\n" );
			ob.append( "</html>\n" );
			
			event.setSource( Unpooled.buffer().writeBytes( ob.toString().getBytes() ) );
		}
		catch ( IOException | EvalException e )
		{
			try
			{
				event.setSource( Unpooled.buffer().writeBytes( generateExceptionPage( e, event.getRequest().getEvalFactory() ).getBytes() ) );
			}
			catch ( EvalException | EvalMultipleException e1 )
			{
				event.getResponse().sendException( e1 );
			}
		}
	}
	
	private String doInclude( String pack, RenderEvent event ) throws IOException, EvalException
	{
		EvalFactoryResult result = doInclude0( pack, event );
		if ( result.isSuccessful() )
			return result.getString();
		return "";
	}
	
	private EvalFactoryResult doInclude0( String pack, RenderEvent event ) throws IOException, EvalException
	{
		EvalFactory factory = event.getRequest().getEvalFactory();
		
		if ( getConfig().getBoolean( "config.ignoreFileNotFound" ) )
			return WebFunc.evalPackage( factory, event.getSite(), pack );
		
		return WebFunc.evalPackageWithException( factory, event.getSite(), pack );
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
