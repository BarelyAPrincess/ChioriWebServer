/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.groovy;

import groovy.lang.GroovyShell;

/**
 * Helps the EvalFactory keep track of it's GroovyShells
 */
public class GroovyShellTracker
{
	private final GroovyShell shell;
	private boolean inUse = false;
	
	public GroovyShellTracker( GroovyShell shell )
	{
		this.shell = shell;
	}
	
	public GroovyShell getShell()
	{
		return shell;
	}
	
	public void setInUse( boolean inUse )
	{
		this.inUse = inUse;
	}
	
	public boolean isInUse()
	{
		return inUse;
	}
	
	@Override
	public String toString()
	{
		return "GroovyShellTracker(shell=" + shell + ",inUse=" + inUse + ")";
	}
}
