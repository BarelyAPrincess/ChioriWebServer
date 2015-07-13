package com.chiorichan.plugin.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.InterpreterOverrides;
import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalResult;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.lang.EvalException;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.PluginNotFoundException;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.site.SiteManager;
import com.chiorichan.util.FileFunc;
import com.chiorichan.util.NetworkFunc;
import com.chiorichan.util.Versioning;
import com.chiorichan.util.WebFunc;

/**
 * Chiori-chan's Web Server Template Plugin
 */
public class TemplateUtils
{
	private static final String GITHUB_BRANCH = Versioning.getGitHubBranch();
	private static final String GITHUB_SERVER_URL = "https://raw.githubusercontent.com/ChioriGreene/ChioriWebServer/";
	private static final String SERVER_PLUGIN_NAMESPACE = "com.chiorichan.plugin.";
	
	private static String baseTemplate = null;
	
	static String formatStackTrace( StackTraceElement[] stackTrace, ScriptTraceElement[] scriptTrace )
	{
		Validate.notEmpty( stackTrace );
		
		int l = 0;
		
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement ste : stackTrace )
		{
			String fileName = ste.getFileName() == null ? "<Unknown Source>" : ste.getFileName() + ( ( ste.getLineNumber() > -1 ) ? String.format( "(%s)", ste.getLineNumber() ) : "" );
			
			String previewType = "core";
			
			if ( ste.getClassName().startsWith( "com.chiori" ) )
				previewType = "app";
			
			if ( ste.getFileName() != null && ste.getFileName().matches( "GroovyScript\\d*\\.chi" ) )
				previewType = "groovy";
			
			String codePreview = "There is no source file available for this preview";
			boolean expanded = false;
			
			if ( !previewType.equals( "core" ) )
			{
				codePreview = generateCodePreview( ste );
				
				if ( scriptTrace != null )
					for ( ScriptTraceElement st : scriptTrace )
						if ( st.getFileName() != null && ste.getFileName() != null && st.getFileName().equals( ste.getFileName() ) && st.getLineNumber() == ste.getLineNumber() )
						{
							codePreview = generateCodePreview( st );
							if ( st.context() != null )
								fileName = st.context().filename() + ( ste.getLineNumber() > -1 ? String.format( "(%s)", ste.getLineNumber() ) : "" );
							break;
						}
				
				if ( codePreview == null )
					codePreview = "There was a problem getting this code preview, either the file is non-existent or could not be read.";
				else
					expanded = true;
			}
			
			sb.append( "<tr class=\"trace " + previewType + ( expanded ? " expanded" : " collapsed" ) + "\">\n" );
			sb.append( "	<td class=\"number\">#" + l + "</td>\n" );
			sb.append( "	<td class=\"content\">\n" );
			sb.append( "		<div class=\"trace-file\">\n" );
			sb.append( "			<div class=\"plus\">+</div>\n" );
			sb.append( "			<div class=\"minus\">&#8259;</div>\n" );
			sb.append( "			" + fileName + ": <strong>" + ste.getClassName() + "." + ste.getMethodName() + "</strong>\n" );
			sb.append( "		</div>\n" );
			sb.append( "		<div class=\"code\">\n" );
			sb.append( "			<pre>" + codePreview + "</pre>\n" );
			sb.append( "		</div>\n" );
			sb.append( "	</td>\n" );
			sb.append( "</tr>\n" );
			
			l++;
		}
		
		return sb.toString();
	}
	
	static String generateCodePreview( File file, int lineNum )
	{
		return generateCodePreview( file, lineNum, -1 );
	}
	
	static String generateCodePreview( File file, int lineNum, int colNum )
	{
		Validate.notNull( file );
		
		if ( !file.exists() )
			return String.format( "Could not find the file '%s'", file.getAbsolutePath() );
		
		FileInputStream is;
		try
		{
			is = new FileInputStream( file );
		}
		catch ( FileNotFoundException e )
		{
			return e.getMessage();
		}
		
		try
		{
			byte[] bytes = new byte[is.available()];
			
			IOUtils.readFully( is, bytes );
			
			String source = new String( bytes );
			
			return generateCodePreview( source, lineNum, colNum );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return String.format( "<We had a problem: %s>", e.getMessage() );
		}
	}
	
	static String generateCodePreview( ScriptTraceElement ste )
	{
		EvalContext context = ste.context();
		File file = new File( context.filename() );
		int lineNum = ste.getLineNumber();
		int colNum = ste.getColumnNumber();
		
		return generateCodePreview( file, lineNum, colNum );
	}
	
	static String generateCodePreview( StackTraceElement ste )
	{
		// TODO Match the server version to the correct commit on the github. The closer the better.
		
		String className = ste.getClassName();
		String url = GITHUB_SERVER_URL + GITHUB_BRANCH + "/";
		int lineNum = ste.getLineNumber();
		
		if ( className.startsWith( SERVER_PLUGIN_NAMESPACE + "email" ) )
			url += "EmailPlugin/";
		if ( className.startsWith( SERVER_PLUGIN_NAMESPACE + "template" ) )
			url += "TemplatesPlugin/";
		
		try
		{
			Plugin plugin = PluginManager.INSTANCE.getPluginByClass( Class.forName( ste.getClassName() ) );
			
			if ( plugin != null && plugin.getDescription() != null && plugin.getDescription().getGitHubBaseUrl() != null )
				url = plugin.getDescription().getGitHubBaseUrl();
		}
		catch ( PluginNotFoundException | ClassNotFoundException e )
		{
			// Do Nothing
		}
		
		String gitHubGroovyUrl = url + "src/main/groovy/";
		String gitHubJavaUrl = url + "src/main/java/";
		String fileUrl = className.replace( '.', '/' ).replace( "$1", "" ) + "." + InterpreterOverrides.getFileExtension( ste.getFileName() );
		String finalUrl = gitHubGroovyUrl + fileUrl;
		
		byte[] result = NetworkFunc.readUrl( finalUrl );
		
		if ( result == null )
		{
			finalUrl = gitHubJavaUrl + fileUrl;
			result = NetworkFunc.readUrl( finalUrl );
		}
		
		if ( result == null )
			return String.format( "Could not read file '%s' from the GitHub repository.\nYou could be running a mismatching version to the repository or this file belongs to another repository.", fileUrl );
		else
			return String.format( "%s<br /><a target=\"_blank\" href=\"%s\">View this file on our GitHub!</a>", generateCodePreview( new String( result ), lineNum ), finalUrl );
	}
	
	static String generateCodePreview( String source, int lineNum )
	{
		return generateCodePreview( source, lineNum, -1 );
	}
	
	static String generateCodePreview( String source, int lineNum, int colNum )
	{
		StringBuilder sb = new StringBuilder();
		
		int cLine = 0;
		for ( String l : source.split( "\n" ) )
		{
			cLine++;
			
			if ( cLine > lineNum - 5 && cLine < lineNum + 5 )
				if ( cLine == lineNum )
				{
					if ( colNum > -1 && colNum <= l.length() )
					{
						colNum--;
						l = WebFunc.escapeHTML( l.substring( 0, colNum ) ) + "<span style=\"background-color: red; font-weight: bolder;\">" + WebFunc.escapeHTML( l.substring( colNum, colNum + 1 ) ) + "</span>" + WebFunc.escapeHTML( l.substring( colNum + 1 ) );
					}
					else
						l = WebFunc.escapeHTML( l );
					
					sb.append( String.format( "<span class=\"error\"><span class=\"ln error-ln\">%4s</span> %s</span>", cLine, l ) );
				}
				else
					sb.append( String.format( "<span class=\"ln\">%4s</span> %s\n", cLine, WebFunc.escapeHTML( l ) ) );
		}
		
		return sb.toString();
	}
	
	static String generateCodePreview( Throwable t )
	{
		if ( t instanceof EvalException )
		{
			ScriptTraceElement[] ste = ( ( EvalException ) t ).getScriptTrace();
			
			if ( ste != null && ste.length > 0 )
				return generateCodePreview( ste[0] );
		}
		
		return generateCodePreview( t.getStackTrace()[0] );
	}
	
	static EvalResult wrapAndEval( EvalFactory factory, String html ) throws UnsupportedEncodingException, IOException
	{
		Validate.notNull( factory );
		
		String pageMark = "<!-- PAGE DATA -->";
		
		if ( baseTemplate == null )
		{
			InputStream is = TemplateUtils.class.getClassLoader().getResourceAsStream( "BaseTemplate.html" );
			baseTemplate = ( is == null ) ? "" : new String( FileFunc.inputStream2Bytes( is ), "UTF-8" );
		}
		
		return factory.eval( EvalContext.fromSource( baseTemplate.replace( pageMark, html ) ).shell( "html" ).site( SiteManager.INSTANCE.getDefaultSite() ) );
	}
}
