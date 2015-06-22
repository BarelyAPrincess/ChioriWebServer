/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.lang;

/**
 * Used to gracefully abort a server startup, e.g., by user interaction.
 */
public class StartupAbortException extends StartupException
{
	private static final long serialVersionUID = -4937198089020390887L;
	
	public StartupAbortException()
	{
		super( "STARTUP ABORTED!" );
	}
}
