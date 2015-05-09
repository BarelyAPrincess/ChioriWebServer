/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import java.util.Map;

import com.chiorichan.site.Site;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * Stores arbitrary data for sessions being loaded from their datastore.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public abstract class SessionData
{
	Map<String, String> data = Maps.newHashMap();
	
	int timeout;
	String ipAddr;
	String sessionName;
	String sessionId;
	Site site;
	boolean newlyCreated;
	
	private final SessionDatastore datastore;
	
	SessionData( SessionDatastore datastore )
	{
		this.datastore = datastore;
		defaults();
	}
	
	final SessionDatastore datastore()
	{
		return datastore;
	}
	
	void defaults()
	{
		timeout = 0;
		ipAddr = "";
		sessionName = null;
		sessionId = null;
		site = null;
		newlyCreated = true;
	}
	
	@Override
	public String toString()
	{
		return "SessionData{" + Joiner.on( "," ).withKeyValueSeparator( "=" ).join( data ) + "}";
	}
	
	abstract void reload() throws SessionException;
	
	abstract void destroy() throws SessionException;
	
	abstract void save() throws SessionException;
}
