/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import java.util.Map;

import com.chiorichan.tasks.Timings;
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
	/**
	 * Persistent session variables<br>
	 * Session variables will live outside of the sessions's life
	 */
	Map<String, String> data = Maps.newHashMap();
	
	int timeout;
	String ipAddr;
	String sessionName;
	String sessionId;
	String site;
	boolean stale;
	
	private final SessionDatastore datastore;
	
	SessionData( SessionDatastore datastore, boolean stale )
	{
		this.datastore = datastore;
		this.stale = stale;
		defaults();
	}
	
	final SessionDatastore datastore()
	{
		return datastore;
	}
	
	void defaults()
	{
		timeout = Timings.epoch() + SessionManager.getDefaultTimeout();
		ipAddr = null;
		sessionName = SessionManager.getDefaultSessionName();
		sessionId = null;
		site = "default";
	}
	
	@Override
	public String toString()
	{
		return "SessionData{name=" + sessionName + ",id=" + sessionId + ",ip=" + ipAddr + ",timeout=" + timeout + ",site=" + site + "," + Joiner.on( "," ).withKeyValueSeparator( "=" ).useForNull( "<null>" ).join( data ) + "}";
	}
	
	abstract void reload() throws SessionException;
	
	abstract void destroy() throws SessionException;
	
	abstract void save() throws SessionException;
}
