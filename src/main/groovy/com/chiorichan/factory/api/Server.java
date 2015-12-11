/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.api;

import java.io.FileNotFoundException;
import java.util.List;

import com.chiorichan.Loader;
import com.chiorichan.factory.ScriptTraceElement;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.http.HttpResponseWrapper;
import com.chiorichan.plugin.PluginManager;
import com.chiorichan.plugin.lang.PluginNotFoundException;
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
	 * @param pack
	 *            The file relative
	 * @return
	 *         The EvalContext ready for eval() or read()
	 * @throws FileNotFoundException
	 */
	public static ScriptingContext fileContext( String file ) throws FileNotFoundException
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		return ScriptingContext.fromFile( request.getSite(), file ).request( request );
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
			Loader.getLogger().warning( "NumberParseException was thrown: " + e.toString() );
			return phone;
		}
	}

	public static Plugin getPluginbyClassname( String search ) throws PluginNotFoundException
	{
		return PluginManager.INSTANCE.getPluginByClassname( search );
	}

	public static Plugin getPluginbyClassnameWithoutException( String search )
	{
		return PluginManager.INSTANCE.getPluginByClassnameWithoutException( search );
	}

	public static Plugin getPluginByName( String search ) throws PluginNotFoundException
	{
		return PluginManager.INSTANCE.getPluginByName( search );
	}

	public static Plugin getPluginByNameWithoutException( String search )
	{
		return PluginManager.INSTANCE.getPluginByNameWithoutException( search );
	}

	public static List<ScriptTraceElement> getScriptTrace()
	{
		return request().getEvalFactory().getScriptTrace();
	}

	/**
	 * Used to execute package file within the script.
	 *
	 * @param pack
	 *            The package, e.g, com.chiorichan.widgets.sidemenu
	 * @return
	 *         The EvalContext ready for eval() or read()
	 */
	public static ScriptingContext packageContext( String pack )
	{
		HttpRequestWrapper request = HttpRequestWrapper.getRequest();
		return ScriptingContext.fromPackage( request.getSite(), pack ).request( request );
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
