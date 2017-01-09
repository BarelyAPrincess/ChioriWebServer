/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.http;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.chiorichan.AppLoader;
import com.chiorichan.configuration.apache.ApacheDirective;
import com.chiorichan.configuration.apache.ApacheDirectiveException;
import com.chiorichan.configuration.apache.ApacheSection;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.site.Site;
import com.chiorichan.util.StringFunc;
import com.chiorichan.util.Versioning;

/**
 * Used to parse Apache configuration files, e.g., .htaccess
 */
public class ApacheHandler
{
	public class Option
	{
		private final String key;
		private boolean enabled;

		private Option( String key )
		{
			this.key = key;
		}

		public void disable()
		{
			enabled = false;
		}

		public void enable()
		{
			enabled = true;
		}

		public boolean enabled()
		{
			return enabled;
		}

		public String name()
		{
			return key;
		}
	}

	private class OptionsSet extends HashSet<Option>
	{
		public final Option optionExecCGI = new Option( "ExecCGI" );
		public final Option optionFollowSymLinks = new Option( "FollowSymLinks" );
		public final Option optionIncludes = new Option( "Includes" );
		public final Option optionIncludesNOEXEC = new Option( "IncludesNOEXEC" );
		public final Option optionIndexes = new Option( "Indexes" );
		public final Option optionMultiViews = new Option( "MultiViews" );
		public final Option optionSymLinksIfOwnerMatch = new Option( "SymLinksIfOwnerMatch" );

		{
			add( optionExecCGI );
			add( optionFollowSymLinks );
			add( optionIncludes );
			add( optionIncludesNOEXEC );
			add( optionIndexes );
			add( optionMultiViews );
			add( optionSymLinksIfOwnerMatch );
		}
	}

	public class Override
	{
		private String params = "";
		private boolean allowed;
		private String key;

		private Override( String key )
		{
			this.key = key;
		}

		public void allow()
		{
			allowed = true;
		}

		public boolean allowed()
		{
			return allowed;
		}

		public void deny()
		{
			allowed = false;
		}

		public String getParams()
		{
			return params;
		}

		public String name()
		{
			return key;
		}

		public void setParams( String params )
		{
			if ( this == overrides.overrideNonfatal )
			{
				if ( !params.equalsIgnoreCase( "Override" ) && !params.equalsIgnoreCase( "Unknown" ) && !params.equalsIgnoreCase( "All" ) )
					throw new IllegalArgumentException( "The 'Nonfatal' override only accepts the values Override, Unknown, and All." );
			}
			else if ( this == overrides.overrideOptions )
			{
				if ( !params.equalsIgnoreCase( "All" ) && !params.equalsIgnoreCase( "ExecCGI" ) && !params.equalsIgnoreCase( "FollowSymLinks" ) && !params.equalsIgnoreCase( "Includes" ) && !params.equalsIgnoreCase( "IncludesNOEXEC" ) && !params.equalsIgnoreCase( "Indexes" ) && !params.equalsIgnoreCase( "MultiViews" ) && !params.equalsIgnoreCase( "SymLinksIfOwnerMatch" ) )
					throw new IllegalArgumentException( "The 'Options' override only accepts the values All, ExecCGI, FollowSymLinks, Includes, IncludesNOEXEC, Indexes, MultiViews, and SymLinksIfOwnerMatch. The param must be comma-separated and have absolutely no spaces." );
			}
			else
				throw new IllegalArgumentException( "The '" + name() + "' does not support value arguements, only Nonfatal and Options do." );

			this.params = params;
		}
	}

	public class OverrideDirective
	{
		private String directive;

		private OverrideDirective( String directive )
		{
			this.directive = directive;
		}

		public String directive()
		{
			return directive;
		}
	}

	private class OverridesSet extends HashSet<Override>
	{
		public final Override overrideAuthConfig = new Override( "AuthConfig" );
		public final Override overrideFileInfo = new Override( "FileInfo" );
		public final Override overrideIndexes = new Override( "Indexes" );
		public final Override overrideLimit = new Override( "Limit" );
		public final Override overrideNonfatal = new Override( "Nonfatal" );
		public final Override overrideOptions = new Override( "Options" );

		{
			add( overrideAuthConfig );
			add( overrideFileInfo );
			add( overrideIndexes );
			add( overrideLimit );
			add( overrideNonfatal );
			add( overrideOptions );
		}
	}

	private final Map<Integer, ErrorDocument> errorDocuments = new HashMap<>();
	private final OptionsSet options = new OptionsSet();
	private final OverridesSet overrides = new OverridesSet();

	// private final Set<OverrideDirective> directives = new HashSet<OverrideDirective>();

	public ErrorDocument getErrorDocument( int httpCode )
	{
		return errorDocuments.get( httpCode );
	}

	/**
	 * Processes the provided {@link ApacheSection} (i.e., .htaccess) for instructions and options
	 *
	 * @param handler
	 *             The {@link HttpHandler}
	 * @return
	 *         Will return false if the current request should halt, i.e., the request was already handled by the directives
	 */
	public boolean handleDirectives( ApacheSection apache, HttpHandler handler ) throws ApacheDirectiveException
	{
		boolean def = true;

		for ( ApacheDirective kv : apache.directives() )
		{
			String key = kv.getKey();
			String[] args = kv.getArguments();

			HttpRequestWrapper request = handler.getRequest();
			// HttpResponseWrapper response = handler.getResponse();
			WebInterpreter fi = handler.getInterpreter();
			Site site = handler.getSite();

			switch ( key )
			{
			/**
			 * Section Types
			 */
				case "IfDefine":
					kv.isSection();
					kv.hasArguments( 1, "<Startup Argument>" );
					if ( AppLoader.options().has( args[0] ) || AppLoader.options().hasArgument( args[0] ) )
						if ( !handleDirectives( ( ApacheSection ) kv, handler ) )
							def = false;
					break;
				case "IfModule": // TODO Implement detection of common Apache modules the server can currently imitate, e.g., Rewrite
					kv.isSection();
					kv.hasArguments( 1, "<plugin>" );
					if ( PluginManager.instance().getPluginByNameWithoutException( args[0] ) != null || PluginManager.instance().getPluginByClassnameWithoutException( args[0] ) != null )
						if ( !handleDirectives( ( ApacheSection ) kv, handler ) )
							def = false;
					break;
				case "IfVersion":
					kv.isSection();
					kv.hasArguments( 2, "<operator> <version>" );
					if ( StringFunc.compareVersions( Versioning.getVersionNumber(), args[1], args[0] ) )
						if ( !handleDirectives( ( ApacheSection ) kv, handler ) )
							def = false;
					break;
				case "Directory":
					kv.isSection();
					kv.hasArguments( 1, "<directory>" );
					if ( fi.hasFile() )
					{
						String rel = fi.getFilePath().substring( site.getSubdomain( request.getSubdomain() ).directory().getAbsolutePath().length() );
						if ( rel.startsWith( "/" ) )
							rel = rel.substring( 1 );

						if ( args[0].startsWith( "/" ) && ( fi.getFilePath().startsWith( args[0] ) || ( "/" + rel ).startsWith( args[0] ) ) || rel.startsWith( args[0] ) )
							if ( !handleDirectives( ( ApacheSection ) kv, handler ) )
								def = false;
					}
					break;
				/**
				 * Individual Key/Values
				 */
				case "ErrorDocument":
					ErrorDocument doc = ErrorDocument.parseArgs( args );
					errorDocuments.put( doc.getHttpCode(), doc );
					break;
				case "AllowOverride":
					for ( String a : args )
						if ( a.equalsIgnoreCase( "All" ) )
							for ( Override o : overrides )
								o.allow();
						else if ( a.equalsIgnoreCase( "None" ) )
							for ( Override o : overrides )
								o.deny();
						else
						{
							Override o = override( a.contains( "=" ) ? a.substring( 0, a.indexOf( "=" ) ) : a );
							if ( o == null )
								throw new ApacheDirectiveException( "The 'AllowOverride' directive does not reconize the option '" + a + "'", kv );
							if ( ( o == overrides.overrideNonfatal || o == overrides.overrideOptions ) && a.contains( "=" ) )
								o.setParams( a.substring( a.indexOf( "=" ) + 1 ) );
							o.allow();
						}
					break;
				case "Location":
					kv.isSection();
					kv.hasArguments( 1, "<url>" );
					if ( request.getUri().startsWith( args[0] ) )
						if ( !handleDirectives( ( ApacheSection ) kv, handler ) )
							def = false;
					break;
				case "LocationMatch":
					kv.isSection();
					kv.hasArguments( 1, "<url regex>" );
					if ( request.getUri().matches( args[0] ) )
						if ( !handleDirectives( ( ApacheSection ) kv, handler ) )
							def = false;
					break;
				case "Options":
					if ( overrides.overrideOptions.allowed() )
						for ( String a : args )
						{
							if ( ( a.startsWith( "+" ) || a.startsWith( "-" ) ? a.substring( 1 ) : a ).equalsIgnoreCase( "All" ) )
							{
								for ( Option o : options )
									if ( o != options.optionMultiViews )
										if ( a.startsWith( "-" ) )
											o.disable();
										else
											o.enable();
							}
							else
							{
								Option o = option( a.startsWith( "+" ) || a.startsWith( "-" ) ? a.substring( 1 ) : a );
								if ( o == null )
									throw new ApacheDirectiveException( "The 'Options' directive does not reconize the option '" + a + "'", kv );
								if ( a.startsWith( "-" ) )
									o.disable();
								else
									o.enable();
							}
						}
					else
						throw new ApacheDirectiveException( "The directive 'Option' has been forbidden here", kv );
					break;
				case "VirtualHost":
					throw new ApacheDirectiveException( "You can not define a new site using the VirtualHost directive here, site configs are located within the webroot." );
				case "Proxy":
				case "ProxyMatch":
				case "ProxyPass":
					throw new ApacheDirectiveException( "The module mod_proxy is exclusive to the Apache Web Server. We currently have no implementation, nor interest, to support said directives." );
				default:
					throw new ApacheDirectiveException( "Currently the Apache directive '" + key + "' is not implemented", kv );
			}

			// ErrorDocument 403 http://www.yahoo.com/
			// Order deny,allow
			// Deny from all
			// Allow from 208.113.134.190


		}

		return def;
	}

	public Option option( String key )
	{
		for ( Option o : options )
			if ( o.name().equalsIgnoreCase( key ) )
				return o;
		return null;
	}

	public boolean optionNone()
	{
		for ( Option o : options )
			if ( o.enabled() )
				return false;
		return true;
	}

	public Override override( String key )
	{
		for ( Override o : overrides )
			if ( o.name().equalsIgnoreCase( key ) )
				return o;
		return null;
	}

	public boolean overrideListNone()
	{
		// TODO Auto-generated method stub
		return false;
	}

	public boolean overrideNone()
	{
		for ( Override o : overrides )
			if ( o.allowed() )
				return false;
		return true;
	}
}
