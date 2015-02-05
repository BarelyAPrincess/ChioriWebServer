package com.chiorichan.plugin.builtin.template;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.InterpreterOverrides;
import com.chiorichan.Loader;
import com.chiorichan.exception.ShellExecuteException;
import com.chiorichan.factory.CodeEvalFactory;
import com.chiorichan.factory.CodeMetaData;
import com.chiorichan.framework.WebUtils;
import com.chiorichan.util.FileUtil;
import com.chiorichan.util.Versioning;
import com.google.common.collect.Lists;

public class ExceptionPageUtils
{
	public static StackTraceElement getGroovyScriptElement( StackTraceElement[] elements )
	{
		StackTraceElement result = null;
		
		for ( StackTraceElement ele : elements )
		{
			if ( ele.getFileName() != null && ele.getFileName().toLowerCase().contains( ".groovy" ) )
			{
				result = ele;
				break;
			}
		}
		
		return result;
	}
	
	public static StackTraceElement[] getGroovyScriptElements( StackTraceElement[] elements )
	{
		List<StackTraceElement> result = Lists.newArrayList();
		
		for ( StackTraceElement ele : elements )
		{
			if ( !ele.getClassName().startsWith( "org.codehaus.groovy." ) )
				result.add( ele );
		}
		
		return result.toArray( new StackTraceElement[0] );
	}
	
	public static String getCodeSample( StackTraceElement e, String filePath )
	{
		if ( filePath != null )
			return codeSample( filePath, e.getLineNumber() );
		
		// TODO Match the server version to the correct commit on the github. The closer the better.
		
		String codeSample = null;
		
		filePath = e.getClassName();
		
		String subProject = "";
		String builtinPluginClassPath = "com.chiorichan.plugin.builtin.";
		if ( filePath.startsWith( builtinPluginClassPath ) )
		{
			String plugin = filePath.substring( builtinPluginClassPath.length(), filePath.indexOf( ".", builtinPluginClassPath.length() ) ).toLowerCase();
			
			if ( plugin.equals( "email" ) )
				subProject = "EmailPlugin/";
			else if ( plugin.equals( "template" ) )
				subProject = "TemplatesPlugin/";
		}
		
		String githubGroovyUrl = "https://raw.githubusercontent.com/ChioriGreene/ChioriWebServer/master/" + subProject + "src/main/groovy/";
		String githubJavaUrl = "https://raw.githubusercontent.com/ChioriGreene/ChioriWebServer/master/" + subProject + "src/main/java/";
		String fileUrl = filePath.replace( '.', '/' ).replace( "$1", "" ) + "." + InterpreterOverrides.getFileExtension( e.getFileName() );
		String finalFileUrl = "";
		
		try
		{
			finalFileUrl = githubGroovyUrl + fileUrl;
			codeSample = new String( WebUtils.readUrl( finalFileUrl ) );
		}
		catch( IOException e1 )
		{
			try
			{
				finalFileUrl = githubJavaUrl + fileUrl;
				codeSample = new String( WebUtils.readUrl( finalFileUrl ) );
			}
			catch( IOException e2 )
			{
				codeSample = "Could not read file '" + fileUrl + "' from the GitHub repository.\nYou could be running a mismatching version to the repository or this file belongs to another repository.";
			}
		}
		
		if ( !codeSample.startsWith( "Could not read file" ) )
		{
			codeSample = codeSampleEval( codeSample, e.getLineNumber() );
			codeSample += "<br /><a target=\"_blank\" href=\"" + finalFileUrl + "\">View this file on our GitHub!</a>";
		}
		
		return codeSample;
	}
	
	public static String getCodeSample( Throwable t )
	{
		try
		{
			if ( t instanceof ShellExecuteException )
			{
				CodeMetaData meta = ((ShellExecuteException) t).getCodeMetaData();
				StackTraceElement ele = getGroovyScriptElement( t.getCause().getStackTrace() );
				
				if ( ele != null )
					return getCodeSample( ele, meta.fileName );
			}
			else
			{
				return getCodeSample( t.getStackTrace()[0], null );
			}
		}
		catch( Throwable t1 )
		{
			t1.printStackTrace();
		}
		
		return null;
	}
	
	public static String makeExceptionPage( Throwable t, CodeEvalFactory factory )
	{
		try
		{
			StringBuilder ob = new StringBuilder();
			
			String codeSample = getCodeSample( t );
			String fileName = "";
			int lineNo = -1;
			String className = null;
			
			if ( t instanceof ShellExecuteException )
			{
				CodeMetaData meta = ((ShellExecuteException) t).getCodeMetaData();
				StackTraceElement ele = getGroovyScriptElement( t.getCause().getStackTrace() );
				t = t.getCause();
				
				fileName = meta.fileName;
				
				if ( ele == null )
				{
					Pattern p1 = Pattern.compile( "line[: ]?([0-9]*)" );
					Matcher m1 = p1.matcher( t.getMessage() );
					
					if ( m1.find() && !m1.group( 1 ).isEmpty() )
						lineNo = Integer.parseInt( m1.group( 1 ) );
				}
				else
				{
					lineNo = ele.getLineNumber();
					className = ele.getClassName() + "." + ele.getMethodName();
				}
				
				codeSample = "<p>Source Code:</p><pre>" + codeSample( fileName, lineNo ) + "</pre>";
				
				if ( meta.source != null && !meta.source.isEmpty() )
					codeSample += "<p>Pre-evaluated Code:</p><pre>" + codeSampleEval( meta.source, lineNo ) + "</pre>";
			}
			else
			{
				StackTraceElement ele;
				if ( t.getCause() == null )
					ele = t.getStackTrace()[0];
				else
					ele = t.getCause().getStackTrace()[0];
				
				fileName = ele.getFileName();
				lineNo = ele.getLineNumber();
				className = ele.getClassName() + "." + ele.getMethodName();
			}
			
			Loader.getLogger().warning( "Could not run file '" + fileName + "' because of error '" + t.getMessage() + "'" );
			
			ob.append( "<h1>Internal Server Exception Thrown</h1>\n" );
			ob.append( "<p class=\"message\">\n" );
			ob.append( t.getClass().getName() + ": " + t.getMessage() + "\n" );
			ob.append( "</p>\n" );
			ob.append( "\n" );
			ob.append( "<div class=\"source\">\n" );
			
			ob.append( "<p class=\"file\">" + fileName + "(" + lineNo + "): <strong>" + ((className != null) ? className : "") + "</strong></p>\n" );
			
			ob.append( "\n" );
			ob.append( "<div class=\"code\">\n" );
			if ( codeSample != null )
				ob.append( codeSample + "\n" );
			ob.append( "</div>\n" );
			ob.append( "</div>\n" );
			ob.append( "\n" );
			ob.append( "<div class=\"traces\">\n" );
			ob.append( "<h2>Stack Trace</h2>\n" );
			ob.append( "<p><b><i>Disclaimer: Code samples are pulled from our GitHub repository, if the server is not running the latest version then the sample will not be accurate. When it does work, it can really help dignose bugs.</i></b></p>\n" );
			ob.append( "<table style=\"width:100%;\">\n" );
			ob.append( stackTraceToHtml( t.getStackTrace() ) + "\n" );
			ob.append( "</table>\n" );
			ob.append( "</div>\n" );
			ob.append( "\n" );
			ob.append( "<div class=\"version\">Running <a href=\"https://github.com/ChioriGreene/ChioriWebServer\">" + Versioning.getProduct() + "</a> Version " + Versioning.getVersion() + "<br />" + Versioning.getCopyright() + "</div>\n" );
			
			return wrapAndEval( factory, ob.toString() );
		}
		catch( Throwable t1 )
		{
			t1.printStackTrace();
			return "";
		}
	}
	
	public static String stackTraceToHtml( StackTraceElement[] ste )
	{
		if ( ste == null || ste.length < 1 )
			return "";
		
		int l = 0;
		
		StringBuilder sb = new StringBuilder();
		
		for ( StackTraceElement e : ste )
		{
			String file = (e.getFileName() == null) ? "eval()" : e.getFileName() + "(" + e.getLineNumber() + ")";
			boolean expanded = false;
			boolean isApp = e.getClassName().startsWith( "com.chiori" );
			
			String codeSample = isApp ? getCodeSample( e, null ) : "There is no source code available to preview.";
			
			if ( isApp && !codeSample.startsWith( "Could not read file" ) )
				expanded = true;
			
			sb.append( "<tr class=\"trace" + (isApp ? " app" : " core") + (expanded ? " expanded" : " collapsed") + "\">\n" );
			sb.append( "	<td class=\"number\">#" + l + "</td>\n" );
			sb.append( "	<td class=\"content\">\n" );
			sb.append( "		<div class=\"trace-file\">\n" );
			sb.append( "			<div class=\"plus\">+</div>\n" );
			sb.append( "			<div class=\"minus\">–</div>\n" );
			sb.append( "			" + file + ": <strong>" + e.getClassName() + "." + e.getMethodName() + "</strong>\n" );
			sb.append( "		</div>\n" );
			sb.append( "		<div class=\"code\">\n" );
			sb.append( "			<pre>" + codeSample + "</pre>\n" );
			sb.append( "		</div>\n" );
			sb.append( "	</td>\n" );
			sb.append( "</tr>\n" );
			
			l++;
		}
		
		return sb.toString();
	}
	
	public static String escapeHTML( String l )
	{
		return StringUtils.replaceEach( l, new String[] {"&", "\"", "<", ">"}, new String[] {"&amp;", "&quot;", "&lt;", "&gt;"} );
	}
	
	public static String codeSampleEval( String code, int line )
	{
		return codeSampleEval( code, line, -1 );
	}
	
	public static String codeSampleEval( String code, int line, int col )
	{
		StringBuilder sb = new StringBuilder();
		
		int cLine = 0;
		for ( String l : code.split( "\n" ) )
		{
			l = escapeHTML( l );
			
			cLine++;
			
			if ( cLine > line - 5 && cLine < line + 5 )
			{
				if ( cLine == line )
				{
					if ( col > -1 )
					{
						col++;
						
						l = l.substring( 0, col ) + "<span style=\"background-color: red; font-weight: bolder;\">" + l.substring( col, col + 1 ) + "</span>" + l.substring( col + 1 );
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
	
	public static String codeSample( String file, int line )
	{
		return codeSample( file, line, -1 );
	}
	
	public static String codeSample( String file, int line, int col )
	{
		if ( !file.isEmpty() )
		{
			FileInputStream is;
			try
			{
				is = new FileInputStream( file );
			}
			catch( FileNotFoundException e )
			{
				return e.getMessage();
			}
			
			StringBuilder sb = new StringBuilder();
			try
			{
				BufferedReader br = new BufferedReader( new InputStreamReader( is, "ISO-8859-1" ) );
				
				int cLine = 0;
				String l;
				while( (l = br.readLine()) != null )
				{
					l = escapeHTML( l );
					
					cLine++;
					
					if ( cLine > line - 5 && cLine < line + 5 )
					{
						if ( cLine == line )
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
				
				if ( cLine < line )
					sb.append( "<span class=\"error\"><span class=\"ln error-ln\">" + line + "</span> Unexpected EOF!</span>" );
			}
			catch( IOException e )
			{
				e.printStackTrace();
			}
			
			String rtn = sb.toString();
			
			if ( rtn.endsWith( "\n" ) )
				rtn = sb.toString().substring( 0, sb.toString().length() - 1 );
			
			return rtn;
		}
		return "";
	}
	
	public static String wrapAndEval( CodeEvalFactory factory, String html ) throws IOException, ShellExecuteException
	{
		String pageMark = "<!-- PAGE DATA -->";
		InputStream is = ExceptionPageUtils.class.getClassLoader().getResourceAsStream( "BaseTemplate.html" );
		String baseTemplate = (is == null) ? "" : new String( FileUtil.inputStream2Bytes( is ), "UTF-8" );
		
		CodeMetaData meta = new CodeMetaData();
		meta.shell = "html";
		return factory.eval( baseTemplate.replace( pageMark, html ), meta, Loader.getSiteManager().getFrameworkSite() );
	}
}
