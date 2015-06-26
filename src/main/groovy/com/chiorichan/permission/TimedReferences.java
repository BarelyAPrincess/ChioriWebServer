/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.permission;

import com.chiorichan.tasks.Timings;

public class TimedReferences extends References
{
	int lifeTime;
	
	public TimedReferences( int lifeTime )
	{
		if ( lifeTime < 1 )
			this.lifeTime = -1;
		else
			this.lifeTime = Timings.epoch() + lifeTime;
	}
	
	@Override
	public TimedReferences add( References refs )
	{
		super.add( refs );
		return this;
	}
	
	@Override
	public TimedReferences add( String... refs )
	{
		super.add( refs );
		return this;
	}
	
	public boolean isExpired()
	{
		return lifeTime > 0 && ( lifeTime - Timings.epoch() < 0 );
	}
	
	@Override
	public TimedReferences remove( References refs )
	{
		super.remove( refs );
		return this;
	}
	
	@Override
	public TimedReferences remove( String... refs )
	{
		super.remove( refs );
		return this;
	}
}
