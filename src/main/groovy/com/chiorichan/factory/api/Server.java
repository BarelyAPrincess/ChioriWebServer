/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.factory.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.lang.PluginNotFoundException;
import com.chiorichan.logger.Log;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.loader.Plugin;
import com.chiorichan.session.Session;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Provides the Server API
 */
public class Server
{
	/**
	 * Used to execute site file within the script.
	 * FYI, Absolute and .. paths are disallowed for security reasons
	 *
	 * @param file
	 *             The file relative
	 * @return
	 *         The EvalContext ready for eval() or read()
	 * @throws FileNotFoundException
	 */
	public static ScriptingContext fileContext( String file ) throws FileNotFoundException
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		return ScriptingContext.fromFile( request.getLocation(), file ).request( request );
	}

	public static String formatPhone( String phone )
	{
		if ( phone == null || phone.isEmpty() )
			return "";

		phone = phone.replaceAll( "[ -()\\.]", "" );

		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try
		{
			PhoneNumber num = phoneUtil.parse( phone, "US" );
			return phoneUtil.format( num, PhoneNumberFormat.NATIONAL );
		}
		catch ( NumberParseException e )
		{
			Log.get().warning( "NumberParseException was thrown: " + e.toString() );
			return phone;
		}
	}

	public static Plugin getPluginByClassname( String search ) throws PluginNotFoundException
	{
		return PluginManager.instance().getPluginByClassname( search );
	}

	public static Plugin getPluginByClassnameWithoutException( String search )
	{
		return PluginManager.instance().getPluginByClassnameWithoutException( search );
	}

	public static Plugin getPluginByName( String search ) throws PluginNotFoundException
	{
		return PluginManager.instance().getPluginByName( search );
	}

	public static Plugin getPluginByNameWithoutException( String search )
	{
		return PluginManager.instance().getPluginByNameWithoutException( search );
	}

	public static List<ScriptTraceElement> getScriptTrace()
	{
		return request().getEvalFactory().getScriptTrace();
	}

	/**
	 * Used to execute package file within the script.
	 *
	 * @param pack
	 *             The package, e.g, com.chiorichan.widgets.sidemenu
	 * @return
	 *         The EvalContext ready for eval() or read()
	 */
	public static ScriptingContext packageContext( String pack )
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		return ScriptingContext.fromPackage( request.getLocation(), pack ).request( request );
	}

	public static ScriptingContext packageContextWithException( String pack ) throws IOException
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		return ScriptingContext.fromPackageWithException( request.getLocation(), pack ).request( request );
	}

	public static HttpRequestWrapper request()
	{
		return HttpRequestWrapper.getRequest();
	}

	public static HttpResponseWrapper response()
	{
		return request().getResponse();
	}

	public static Session session()
	{
		return request().getSession();
	}
}
