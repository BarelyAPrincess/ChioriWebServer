/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.terminal;

import com.chiorichan.account.lang.AccountResult;

/**
 * Represents a terminal connection end-point
 */
public interface TerminalHandler
{
	public enum TerminalType
	{
		LOCAL, TELNET, WEBSOCKET;
	}
	
	String getIpAddr();
	
	AccountResult kick( String reason );
	
	boolean disconnect();
	
	void println( String... msg );
	
	void print( String... msg );
	
	TerminalType type();
}
