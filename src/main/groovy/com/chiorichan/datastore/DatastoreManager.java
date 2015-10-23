/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore;

import java.sql.SQLException;
import java.util.List;

import com.chiorichan.ServerLogger;
import com.chiorichan.Loader;
import com.chiorichan.ServerManager;
import com.chiorichan.factory.ExceptionCallback;
import com.chiorichan.factory.ScriptingContext;
import com.chiorichan.lang.EvalException;
import com.chiorichan.lang.ReportingLevel;
import com.chiorichan.tasks.TaskCreator;
import com.google.common.collect.Lists;

/**
 * 
 */
public class DatastoreManager implements ServerManager, TaskCreator
{
	public static final DatastoreManager INSTANCE = new DatastoreManager();
	private static boolean isInitialized = false;
	
	List<Datastore> datastores = Lists.newArrayList();
	
	private DatastoreManager()
	{
		
	}
	
	public static ServerLogger getLogger()
	{
		return Loader.getLogger( "DsMgr" );
	}
	
	public static void init()
	{
		if ( isInitialized )
			throw new IllegalStateException( "The Datastore Manager has already been initialized." );
		assert INSTANCE != null;
		INSTANCE.init0();
		isInitialized = true;
	}
	
	/**
	 * Has this manager already been initialized?
	 * 
	 * @return isInitialized
	 */
	public static boolean isInitialized()
	{
		return isInitialized;
	}
	
	@Override
	public String getName()
	{
		return "DatastoreManger";
	}
	
	private void init0()
	{
		EvalException.registerException( new ExceptionCallback()
		{
			@Override
			public ReportingLevel callback( Throwable cause, ScriptingContext context )
			{
				context.result().addException( new EvalException( ReportingLevel.E_ERROR, cause ) );
				return ReportingLevel.E_ERROR;
			}
		}, SQLException.class );
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
}
