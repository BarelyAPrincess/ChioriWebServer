/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
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
