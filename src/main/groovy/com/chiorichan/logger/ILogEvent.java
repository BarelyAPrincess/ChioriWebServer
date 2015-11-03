/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.logger;

import java.util.logging.Level;

import com.chiorichan.lang.EvalException;

/**
 * Interface for {@link LogEvent} and {@link LogRecord}
 */
public interface ILogEvent
{
	void exceptions( EvalException... exceptions );
	
	void log( Level level, String msg, Object... objs );
	
	void flush();
	
	void header( String msg, Object... objs );
}
