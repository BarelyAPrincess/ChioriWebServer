/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.session;

import java.util.Map;

import com.chiorichan.tasks.Timings;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * Stores arbitrary data for sessions being loaded from their datastore.
 */
public abstract class SessionData
{
	/**
	 * Persistent session variables<br>
	 * Session variables will live outside of the sessions's life
	 */
	Map<String, String> data = Maps.newHashMap();
	
	long timeout;
	String ipAddress;
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
		ipAddress = null;
		sessionName = SessionManager.getDefaultSessionName();
		sessionId = null;
		site = "default";
	}
	
	@Override
	public String toString()
	{
		return "SessionData{name=" + sessionName + ",id=" + sessionId + ",ip=" + ipAddress + ",timeout=" + timeout + ",site=" + site + "," + Joiner.on( "," ).withKeyValueSeparator( "=" ).useForNull( "<null>" ).join( data ) + "}";
	}
	
	abstract void reload() throws SessionException;
	
	abstract void destroy() throws SessionException;
	
	abstract void save() throws SessionException;
}
