/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.session;

import java.util.List;

import com.google.common.collect.Lists;

/**
 */
public class MemoryDatastore extends SessionDatastore
{
	class MemorySessionData extends SessionData
	{
		MemorySessionData( String sessionId, SessionWrapper wrapper )
		{
			super( MemoryDatastore.this, false );
			this.sessionId = sessionId;

			site = wrapper.getLocation().getId();
			ipAddr = wrapper.getIpAddr();
		}

		@Override
		void destroy() throws SessionException
		{
			// Do Nothing
		}

		@Override
		void reload() throws SessionException
		{
			// Do Nothing
		}

		@Override
		void save() throws SessionException
		{
			// Do Nothing
		}
	}

	@Override
	SessionData createSession( String sessionId, SessionWrapper wrapper ) throws SessionException
	{
		return new MemorySessionData( sessionId, wrapper );
	}

	@Override
	List<SessionData> getSessions() throws SessionException
	{
		return Lists.newArrayList();
	}
}
