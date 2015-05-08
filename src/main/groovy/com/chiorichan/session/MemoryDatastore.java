/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.session;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class MemoryDatastore extends SessionDatastore
{
	@Override
	List<SessionData> getSessions() throws SessionException
	{
		return Lists.newArrayList();
	}
	
	@Override
	SessionData createSession() throws SessionException
	{
		return new MemorySessionData();
	}
	
	class MemorySessionData extends SessionData
	{
		MemorySessionData()
		{
			super( MemoryDatastore.this );
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
