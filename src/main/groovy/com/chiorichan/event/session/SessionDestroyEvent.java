/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.event.session;

import com.chiorichan.event.AbstractEvent;
import com.chiorichan.session.Session;

/**
 * Called when a Session has been called for destruction
 * Gives subsystems like OnetimeTokenAccountAuthenticator a chance to destroy old auth tokens
 */
public class SessionDestroyEvent extends AbstractEvent
{
	private Session session;
	private int reasonCode;
	
	public SessionDestroyEvent( Session session, int reasonCode )
	{
		this.session = session;
		this.reasonCode = reasonCode;
	}
	
	public int reason()
	{
		return reasonCode;
	}
	
	public Session session()
	{
		return session;
	}
}
