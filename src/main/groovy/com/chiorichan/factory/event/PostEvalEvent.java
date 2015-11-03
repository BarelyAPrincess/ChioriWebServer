/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.event;

import com.chiorichan.event.AbstractEvent;
import com.chiorichan.event.Cancellable;
import com.chiorichan.factory.ScriptingContext;

public class PostEvalEvent extends AbstractEvent implements Cancellable
{
	private ScriptingContext context;
	private boolean cancelled;
	
	public PostEvalEvent( ScriptingContext context )
	{
		this.context = context;
	}
	
	public ScriptingContext context()
	{
		return context;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
	
	@Override
	public void setCancelled( boolean cancelled )
	{
		this.cancelled = cancelled;
	}
}
