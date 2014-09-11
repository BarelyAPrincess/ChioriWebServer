/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.plugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.chiorichan.Loader;

/**
 * The PluginLogger class is a modified {@link Logger} that prepends all logging calls with the name of the plugin doing
 * the logging. The API for PluginLogger is exactly the same as {@link Logger}.
 * 
 * @see Logger
 */
public class PluginLogger extends Logger
{
	private String pluginName;
	
	/**
	 * Creates a new PluginLogger that extracts the name from a plugin.
	 * 
	 * @param context
	 *             A reference to the plugin
	 */
	public PluginLogger(Plugin context)
	{
		super( context.getClass().getCanonicalName(), null );
		String prefix = context.getDescription().getPrefix();
		pluginName = prefix != null ? new StringBuilder().append( "[" ).append( prefix ).append( "] " ).toString() : "[" + context.getDescription().getName() + "] ";
		setParent( Loader.getLogger().getLogger() );
		setLevel( Level.ALL );
	}
	
	@Override
	public void log( LogRecord logRecord )
	{
		logRecord.setMessage( pluginName + logRecord.getMessage() );
		super.log( logRecord );
	}
	
}
