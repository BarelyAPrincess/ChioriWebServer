/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.session;

import java.util.List;

import com.google.common.collect.Lists;

/**
 */
public class MemoryDatastore extends SessionDatastore
{
	@Override
	List<SessionData> getSessions() throws SessionException
	{
		return Lists.newArrayList();
	}
	
	@Override
	SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException
	{
		return new MemorySessionData( sessionId, wrapper );
	}
	
	class MemorySessionData extends SessionData
	{
		MemorySessionData( String sessionId, SessionWrapper wrapper )
		{
			super( MemoryDatastore.this, false );
			this.sessionId = sessionId;
			
			site = wrapper.getSite().getSiteId();
			ipAddr = wrapper.getIpAddr();
		}
		
		@Override
		void reload() throws SessionException
		{
			// Do Nothing
		}
		
		@Override
		void destroy() throws SessionException
		{
			// Do Nothing
		}
		
		@Override
		void save() throws SessionException
		{
			// Do Nothing
		}
	}
}
