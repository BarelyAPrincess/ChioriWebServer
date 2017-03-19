package com.chiorichan.plugin.template;

import com.chiorichan.ShellOverrides;
import com.chiorichan.Versioning;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.factory.ScriptingFactory;
import com.chiorichan.factory.ScriptingResult;
import com.chiorichan.lang.ScriptingException;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.site.SiteManager;
import com.chiorichan.utils.UtilHttp;
import com.chiorichan.utils.UtilIO;
import com.chiorichan.utils.UtilStrings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Chiori-chan's Web Server Template Plugin
 */
public class TemplateUtils
{
	private static final String GITHUB_API_BRANCH = "master-dev";
	private static final String GITHUB_API_URL = "https://raw.githubusercontent.com/ChioriGreene/ChioriAPI/";
	private static final String GITHUB_SERVER_BRANCH = Versioning.getGitHubBranch();
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
			if ( ste.getFileName() == null && ste.getLineNumber() == -1 )
				continue;

			String fileName = ste.getFileName() == null ? "<Unknown Source>" : ste.getFileName() + ( ste.getLineNumber() > -1 ? String.format( "(%s)", ste.getLineNumber() ) : "" );
			String codePreview = "There is no source code available for this preview";
			String codePreviewType = "core";
			boolean expanded = false;

			if ( ste.getClassName().startsWith( "com.chiorichan." ) )
			{
				codePreview = generateCodePreview( ste );
				codePreviewType = "app";
				expanded = true;
			}

			if ( scriptTrace != null )
				for ( ScriptTraceElement st : scriptTrace )
					if ( st.getFileName() != null && ste.getFileName() != null && st.getFileName().equals( ste.getFileName() ) && st.getLineNumber() == ste.getLineNumber() )
					{
						codePreviewType = "groovy";
						expanded = true;

						codePreview = generateCodePreview( st );
						if ( st.context() != null )
							fileName = st.context().filename() + ( ste.getLineNumber() > -1 ? String.format( "(%s)", ste.getLineNumber() ) : "" );
						break;
					}

			sb.append( "<tr class=\"trace " + codePreviewType + ( expanded ? " expanded" : " collapsed" ) + "\">\n" );
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
			return String.format( "Failed to generate code preview: %s", e.getMessage() );
		}
	}

	static String generateCodePreview( ScriptTraceElement ste )
	{
		ScriptingContext context = ste.context();
		File file = new File( context.filename() );
		int lineNum = ste.getLineNumber();
		int colNum = ste.getColumnNumber();

		return generateCodePreview( file, lineNum, colNum );
	}

	static String generateCodePreview( StackTraceElement ste )
	{
		// TODO Match the server version to the correct commit on the GitHub. The closer the better.

		try
		{
			String className = ste.getClassName();
			if ( className.contains( "$" ) )
				className = className.substring( 0, className.indexOf( "$" ) );
			int lineNum = ste.getLineNumber();
			byte[] result = null;

			String urlAppend = className.replace( '.', '/' ) + "." + ShellOverrides.getFileExtension( ste.getFileName() );
			String url = null;

			Plugin plugin;

			plugin = PluginManager.instance().getPluginByClassWithoutException( Class.forName( className ) );

			if ( plugin != null )
			{
				if ( plugin.getDescription() != null && plugin.getDescription().getGitHubBaseUrl() != null )
				{
					url = plugin.getDescription().getGitHubBaseUrl();
					if ( !url.endsWith( "/" ) )
						url += "/";
					result = UtilHttp.readUrl( url + urlAppend );
				}
				else
					return String.format( "Plugin %s does not have a GitHub base url.", plugin.getName() );
			}
			else if ( className.startsWith( "com.chiorichan." ) )
			{
				// Try API URI first!
				url = GITHUB_API_URL + GITHUB_API_BRANCH + "/src/main/java/" + urlAppend;

				result = UtilHttp.readUrl( url );

				if ( result == null )
				{
					// Try CWS URI second!
					url = GITHUB_SERVER_URL + GITHUB_SERVER_BRANCH + "/src/main/groovy/" + urlAppend;

					result = UtilHttp.readUrl( url );
				}

				if ( result == null )
					return String.format( "Could not read file '%s' from GitHub.\nYou could be running an outdated version or this file belongs to another repository.", urlAppend );
			}
			else
				return "There is no source code available for this preview";

			if ( result == null || url == null )
				return "Failed to get source code from GitHub repository";
			else
				return String.format( "%s<br /><a target=\"_blank\" href=\"%s\">View this file on GitHub!</a>", generateCodePreview( new String( result ), lineNum ), url );
		}
		catch ( Throwable t )
		{
			PluginManager.instance().getPluginByClassWithoutException( Template.class ).getLogger().severe( String.format( "Failed to get %s from GitHub repository.", ste.getFileName() ), t );
			return "Failed to get source code from GitHub repository";
		}
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
						l = UtilStrings.escapeHtml( l.substring( 0, colNum ) ) + "<span style=\"background-color: red; font-weight: bolder;\">" + UtilStrings.escapeHtml( l.substring( colNum, colNum + 1 ) ) + "</span>" + UtilStrings.escapeHtml( l.substring( colNum + 1 ) );
					}
					else
						l = UtilStrings.escapeHtml( l );

					sb.append( String.format( "<span class=\"error\"><span class=\"ln error-ln\">%4s</span> %s</span>", cLine, l ) );
				}
				else
					sb.append( String.format( "<span class=\"ln\">%4s</span> %s\n", cLine, UtilStrings.escapeHtml( l ) ) );
		}

		return sb.toString();
	}

	static String generateCodePreview( Throwable t )
	{
		if ( t instanceof ScriptingException )
		{
			ScriptTraceElement[] ste = ( ( ScriptingException ) t ).getScriptTrace();

			if ( ste != null && ste.length > 0 )
				return generateCodePreview( ste[0] );
		}

		return generateCodePreview( t.getStackTrace()[0] );
	}

	static ScriptingResult wrapAndEval( ScriptingFactory factory, String html ) throws IOException
	{
		Validate.notNull( factory );

		String pageMark = "<!-- PAGE DATA -->";

		if ( baseTemplate == null )
		{
			InputStream is = TemplateUtils.class.getClassLoader().getResourceAsStream( "BaseTemplate.html" );
			baseTemplate = is == null ? "" : new String( UtilIO.inputStream2Bytes( is ), "UTF-8" );
		}

		return factory.eval( ScriptingContext.fromSource( baseTemplate.replace( pageMark, html ) ).shell( "html" ).site( SiteManager.instance().getDefaultSite() ) );
	}
}
