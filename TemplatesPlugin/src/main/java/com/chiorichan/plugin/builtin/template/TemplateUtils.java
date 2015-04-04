package com.chiorichan.plugin.builtin.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.chiorichan.InterpreterOverrides;
import com.chiorichan.Loader;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.factory.EvalMetaData;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.lang.EvalFactoryException;
import com.chiorichan.util.FileUtil;

public class TemplateUtils
{
	private static final String GITHUB_BRANCH = "v9-experimental";
	private static final String GITHUB_SERVER_URL = "https://raw.githubusercontent.com/ChioriGreene/ChioriWebServer/";
	private static final String BUILTIN_PLUGIN_NAMESPACE = "com.chiorichan.plugin.builtin.";
	
	public static String formatStackTrace( StackTraceElement[] stackTrace, ScriptTraceElement[] scriptTrace )
	{
		Validate.notEmpty( stackTrace );
		
		int l = 0;
		
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement ste : stackTrace )
		{
			String fileName = ( ste.getFileName() == null ) ? "Unknown Source" : ste.getFileName() + ( ( ste.getLineNumber() > -1 ) ? "(" + ste.getLineNumber() + ")" : "" );
			
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
				{
					for ( ScriptTraceElement st : scriptTrace )
					{
						if ( st.getFileName().equals( ste.getFileName() ) && st.getLineNumber() == ste.getLineNumber() )
						{
							codePreview = generateCodePreview( st );
							break;
						}
					}
				}
				
				if ( codePreview != null )
					expanded = true;
				
				if ( codePreview == null )
					codePreview = "There was a problem getting this code preview, either the file is non-existent or could not be read.";
			}
			
			sb.append( "<tr class=\"trace " + previewType + ( expanded ? " expanded" : " collapsed" ) + "\">\n" );
			sb.append( "	<td class=\"number\">#" + l + "</td>\n" );
			sb.append( "	<td class=\"content\">\n" );
			sb.append( "		<div class=\"trace-file\">\n" );
			sb.append( "			<div class=\"plus\">+</div>\n" );
			sb.append( "			<div class=\"minus\">–</div>\n" );
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
	
	public static String generateCodePreview( Throwable t )
	{
		if ( t instanceof EvalFactoryException )
		{
			ScriptTraceElement[] ste = ( ( EvalFactoryException ) t ).getScriptTrace();
			
			if ( ste != null && ste.length > 0 )
				return generateCodePreview( ste[0] );
		}
		
		return generateCodePreview( t.getStackTrace()[0] );
	}
	
	public static String generateCodePreview( StackTraceElement ste )
	{
		// TODO Match the server version to the correct commit on the github. The closer the better.
		
		String className = ste.getClassName();
		String url = GITHUB_SERVER_URL + GITHUB_BRANCH + "/";
		int lineNum = ste.getLineNumber();
		
		// Determines if the repository for this piece of code is located at another Github URL, e.g., Plugins
		// TODO Match classname with loaded plugins and query actual plugin for it's Github URL.
		switch ( className )
		{
			case BUILTIN_PLUGIN_NAMESPACE + ".email":
				url += "EmailPlugin/";
				break;
			case BUILTIN_PLUGIN_NAMESPACE + ".template":
				url += "TemplatesPlugin/";
				break;
		}
		
		String gitHubGroovyUrl = url + "src/main/groovy/";
		String gitHubJavaUrl = url + "src/main/java/";
		String fileUrl = className.replace( '.', '/' ).replace( "$1", "" ) + "." + InterpreterOverrides.getFileExtension( ste.getFileName() );
		String finalUrl = gitHubGroovyUrl + fileUrl;
		
		byte[] result = WebUtils.readUrl( finalUrl );
		
		if ( result == null )
		{
			finalUrl = gitHubJavaUrl + fileUrl;
			result = WebUtils.readUrl( finalUrl );
		}
		
		String finalResult;
		
		if ( result == null )
			finalResult = "Could not read file '" + fileUrl + "' from the GitHub repository.\nYou could be running a mismatching version to the repository or this file belongs to another repository.";
		else
		{
			finalResult = generateCodePreview( new String( result ), lineNum );
			finalResult += "<br /><a target=\"_blank\" href=\"" + finalUrl + "\">View this file on our GitHub!</a>";
		}
		
		return finalResult;
	}
	
	public static String generateCodePreview( ScriptTraceElement ste )
	{
		EvalMetaData metaData = ste.getMetaData();
		File file = new File( metaData.fileName );
		int lineNum = ste.getLineNumber();
		int colNum = ste.getColumnNumber();
		
		return generateCodePreview( file, lineNum, colNum );
	}
	
	public static String generateCodePreview( File file, int lineNum )
	{
		return generateCodePreview( file, lineNum, -1 );
	}
	
	public static String generateCodePreview( File file, int lineNum, int colNum )
	{
		Validate.notNull( file );
		
		if ( !file.exists() )
			return "Could not find the file '" + file.getAbsolutePath() + "'";
		
		FileInputStream is;
		try
		{
			is = new FileInputStream( file );
		}
		catch ( FileNotFoundException e )
		{
			return e.getMessage();
		}
		
		if ( true )
		{
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
			}
		}
		
		StringBuilder sb = new StringBuilder();
		try
		{
			BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
			
			int cLine = 0;
			String l;
			while ( ( l = br.readLine() ) != null )
			{
				l = escapeHTML( l );
				
				cLine++;
				
				if ( cLine > lineNum - 5 && cLine < lineNum + 5 )
				{
					if ( cLine == lineNum )
					{
						sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
					}
					else
					{
						sb.append( "<span class=\"ln\">" + cLine + "</span> " + l + "\n" );
					}
				}
			}
			
			is.close();
			
			if ( cLine < lineNum )
				sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + lineNum + "</span> Unexpected EOF!</span>" );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		String rtn = sb.toString();
		
		if ( rtn.endsWith( "\n" ) )
			rtn = sb.toString().substring( 0, sb.toString().length() - 1 );
		
		return rtn;
	}
	
	public static String generateCodePreview( String source, int lineNum )
	{
		return generateCodePreview( source, lineNum, -1 );
	}
	
	public static String generateCodePreview( String source, int lineNum, int colNum )
	{
		StringBuilder sb = new StringBuilder();
		
		int cLine = 0;
		for ( String l : source.split( "\n" ) )
		{
			l = escapeHTML( l );
			
			cLine++;
			
			if ( cLine > lineNum - 5 && cLine < lineNum + 5 )
			{
				if ( cLine == lineNum )
				{
					if ( colNum > -1 && colNum <= l.length() )
					{
						colNum--;
						l = l.substring( 0, colNum ) + "<span style=\"background-color: red; font-weight: bolder;\">" + l.substring( colNum, colNum + 1 ) + "</span>" + l.substring( colNum + 1 );
					}
					
					sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + cLine + "</span> " + l + "</span>" );
				}
				else
				{
					sb.append( "<span class=\"ln\">" + cLine + "</span> " + l + "\n" );
				}
			}
		}
		
		return sb.toString();
	}
	
	public static String escapeHTML( String l )
	{
		return StringUtils.replaceEach( l, new String[] {"&", "\"", "<", ">"}, new String[] {"&amp;", "&quot;", "&lt;", "&gt;"} );
	}
	
	public static String wrapAndEval( EvalFactory factory, String html ) throws IOException, EvalFactoryException
	{
		String pageMark = "<!-- PAGE DATA -->";
		InputStream is = TemplateUtils.class.getClassLoader().getResourceAsStream( "BaseTemplate.html" );
		String baseTemplate = ( is == null ) ? "" : new String( FileUtil.inputStream2Bytes( is ), "UTF-8" );
		
		EvalMetaData meta = new EvalMetaData();
		meta.shell = "html";
		
		return factory.eval( baseTemplate.replace( pageMark, html ), meta, Loader.getSiteManager().getFrameworkSite() ).getString();
	}
}
