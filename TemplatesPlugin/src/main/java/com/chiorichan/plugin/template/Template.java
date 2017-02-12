package com.chiorichan.plugin.template;

import com.chiorichan.AppConfig;
import com.chiorichan.Versioning;
import com.chiorichan.event.EventBus;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.EventPriority;
import com.chiorichan.event.Listener;
import com.chiorichan.event.http.HttpExceptionEvent;
import com.chiorichan.event.http.RenderEvent;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.helpers.Namespace;
import com.chiorichan.lang.ExceptionReport;
import com.chiorichan.lang.MultipleException;
import com.chiorichan.lang.PluginException;
import com.chiorichan.lang.ScriptingException;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.site.Site;
import com.chiorichan.site.SiteManager;
import com.chiorichan.zutils.ServerFunc;
import com.chiorichan.zutils.ZObjects;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chiori-chan's Web Server Template Plugin
 */
public class Template extends Plugin implements Listener
{
	private String generateExceptionPage( Throwable t, ScriptingFactory factory ) throws Exception
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

		if ( t instanceof ScriptingException )
		{
			scriptTrace = ( ( ScriptingException ) t ).getScriptTrace();

			if ( t.getCause() != null )
				t = t.getCause();

			if ( scriptTrace != null && scriptTrace.length > 0 )
			{
				ScriptTraceElement ste = scriptTrace[0];

				lineNum = ste.getLineNumber();
				colNum = ste.getColumnNumber();

				if ( lineNum < 0 && t.getMessage() != null )
				{
					/*
					 * Sometimes the last ScriptTrace never gets properly formatted usually due to the exception being a parse problem
					 */
					ste.examineMessage( t.getMessage() );
					lineNum = ste.getLineNumber();
					colNum = ste.getColumnNumber();
				}

				className = ste.getClassName();
				String methodName = ste.getMethodName();

				if ( methodName != null && !methodName.isEmpty() )
					className += "." + methodName;

				if ( className.isEmpty() )
					className = null;

				ScriptingContext context = ste.context();
				Validate.notNull( context );

				fileName = context.filename();

				if ( lineNum > -1 )
				{
					String preview = TemplateUtils.generateCodePreview( ste );
					codeSample += "<p>Source Code:</p><pre>" + preview + "</pre>";

					if ( context.baseSource() != null && !context.baseSource().isEmpty() && !context.baseSource().equals( preview ) )
						codeSample += "<p>Parsed Source Code:</p><pre>" + TemplateUtils.generateCodePreview( context.baseSource(), lineNum, colNum ) + "</pre>";
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

		ob.append( "<p class=\"file\">" + fileName + ( lineNum > -1 ? "(" + lineNum + ( colNum > -1 ? ":" + colNum : "" ) + ")" : "" ) + ( className != null ? ": <strong>" + className + "</strong>" : "" ) + "</p>\n" );

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

		ScriptingResult result = TemplateUtils.wrapAndEval( factory, ob.toString() );

		if ( result.hasExceptions() )
			ExceptionReport.throwExceptions( result.getExceptions() );

		return result.getString();
	}

	/*
	 * private String getPackageName( String pack )
	 * {
	 * if ( pack.indexOf( "." ) < 0 )
	 * return pack;
	 *
	 * String[] packs = pack.split( "\\.(?=[^.]*$)" );
	 *
	 * return packs[1];
	 * }
	 */

	@Override
	public void onDisable() throws PluginException
	{

	}

	@Override
	public void onEnable() throws PluginException
	{
		saveDefaultConfig();
		EventBus.instance().registerEvents( this, this );
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onHttpExceptionEvent( HttpExceptionEvent event )
	{
		if ( !getConfig().getBoolean( "config.renderExceptionPages", true ) )
			return;

		try
		{
			// We check if this exception was thrown from inside our plugin to prevent a fatal looping issue
			if ( ExceptionUtils.indexOfThrowable( event.getThrowable(), Template.class ) > -1 )
				return;

			ScriptingFactory factory = event.getRequest().getEvalFactory();

			// We initialize a temporary EvalFactory if the request did not contain one
			if ( factory == null )
				if ( event.getRequest().getBinding() == null )
					factory = ScriptingFactory.create( new HashMap<String, Object>() );
				else
					factory = ScriptingFactory.create( event.getRequest() );

			event.setErrorHtml( generateExceptionPage( event.getThrowable(), factory ) );
		}
		catch ( Throwable t )
		{
			t.printStackTrace();
		}
	}

	@Override
	public void onLoad() throws PluginException
	{

	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onRenderEvent( RenderEvent event ) throws Exception
	{
		try
		{
			Site site = event.getSite();
			Map<String, String> fwParams = event.getParams();

			if ( site == null )
				site = SiteManager.instance().getDefaultSite();

			if ( fwParams.get( "themeless" ) != null && ZObjects.isTrue( fwParams.get( "themeless" ) ) )
				return;

			String theme = fwParams.get( "theme" );
			String view = fwParams.get( "view" );
			String title = fwParams.get( "title" );

			if ( ZObjects.isNull( theme ) )
				theme = "";

			if ( ZObjects.isNull( view ) )
				view = "";

			if ( !getConfig().getBoolean( "config.alwaysRender" ) && ZObjects.isEmpty( theme ) && ZObjects.isEmpty( view ) )
				return;

			// TODO return if the request is for a none text contentType

			if ( ZObjects.isEmpty( theme ) )
				theme = "com.chiorichan.themes.default";

			StringBuilder ob = new StringBuilder();

			String docType = getConfig().getString( "config.defaultDocType", "html" );

			if ( fwParams.get( "docType" ) != null && !fwParams.get( "docType" ).isEmpty() )
				docType = fwParams.get( "docType" );

			ob.append( "<!DOCTYPE " + docType + ">\n" );
			ob.append( "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" );
			ob.append( "<head>\n" );
			ob.append( "<meta charset=\"utf-8\">\n" );

			String siteTitle;
			if ( site.getTitle() == null || site.getTitle().isEmpty() )
				siteTitle = AppConfig.get().getString( "framework.sites.defaultTitle", "Unnamed Site" );
			else
				siteTitle = site.getTitle();

			if ( title == null || title.isEmpty() )
				ob.append( "<title>" + siteTitle + "</title>\n" );
			else if ( title.startsWith( "!" ) )
				ob.append( "<title>" + title.substring( 1 ) + "</title>\n" );
			else
				ob.append( "<title>" + title + " - " + siteTitle + "</title>\n" );

			// for ( String tag : site.getMetatags() )
			// ob.append( tag + "\n" );

			boolean showCommons = !getConfig().getBoolean( "config.noCommons" );

			if ( fwParams.get( "noCommons" ) != null )
				showCommons = !ZObjects.isTrue( fwParams.get( "noCommons" ) );

			List<String> headers = Lists.newArrayList();

			if ( fwParams.get( "header" ) != null && !fwParams.get( "header" ).isEmpty() )
				headers.add( fwParams.get( "header" ) );

			Namespace ns = Namespace.parseString( theme ).getParentNamespace( 2 );

			if ( showCommons )
				headers.add( ns.append( "includes.common" ).getString() );
			headers.add( ns.append( "includes." + ns.getLocalName() ).getString() );

			for ( String pack : headers )
				try
				{
					ob.append( packageRead( pack, event ) + "\n" );
				}
				catch ( Throwable t )
				{
					getLogger().warning( String.format( "There was a problem reading the header package '%s'", pack ) );
				}

			ob.append( "</head>\n" );

			String pageMark = "<!-- " + getConfig().getString( "config.defaultTag", "PAGE DATA" ) + " -->";
			String pageData = "";
			String viewData = "";
			Map<String, String> params = new HashMap<>( fwParams );

			if ( !theme.isEmpty() )
			{
				ScriptingResult result = packageEval( theme, event );
				pageData = result.getString();
				params.putAll( result.context().request().getRequestMapRaw() );
			}

			if ( !view.isEmpty() )
			{
				ScriptingResult result = packageEval( view, event );
				viewData = result.getString();
				params.putAll( result.context().request().getRequestMapRaw() );
			}

			ob.append( "<body" + ( params == null ? " " + params.get( "bodyArgs" ) : "" ) + ">\n" );

			if ( viewData != null && !viewData.isEmpty() )
				if ( pageData.indexOf( pageMark ) < 0 )
					pageData = pageData + viewData;
				else
					pageData = pageData.replace( pageMark, viewData );

			if ( pageData.indexOf( pageMark ) < 0 )
				pageData = pageData + ServerFunc.byteBuf2String( event.getSource(), event.getEncoding() );
			else
				pageData = pageData.replace( pageMark, ServerFunc.byteBuf2String( event.getSource(), event.getEncoding() ) );

			ob.append( pageData + "\n" );

			if ( fwParams.get( "footer" ) != null && !fwParams.get( "footer" ).isEmpty() )
				ob.append( packageRead( fwParams.get( "footer" ), event ) + "\n" );

			ob.append( "</body>\n" );
			ob.append( "</html>\n" );

			event.setSource( Unpooled.buffer().writeBytes( ob.toString().getBytes() ) );
		}
		catch ( ScriptingException | MultipleException e )
		{
			event.getResponse().sendException( e );
		}
	}

	private ScriptingResult packageEval( String pack, RenderEvent event ) throws Exception
	{
		ScriptingContext context = ScriptingContext.fromPackage( event.getSite(), pack ).request( event.getRequest() ).require();
		ScriptingResult result = event.getRequest().getEvalFactory().eval( context );

		if ( result.hasNonIgnorableExceptions() )
			ExceptionReport.throwExceptions( result.getExceptions() );

		return result;
	}

	private String packageRead( String pack, RenderEvent event ) throws ScriptingException, MultipleException
	{
		ScriptingContext context = ScriptingContext.fromPackage( event.getSite(), pack ).request( event.getRequest() );
		context.require( !getConfig().getBoolean( "config.ignoreFileNotFound" ) );
		return context.read( false );
	}
}
