/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import com.chiorichan.lang.ErrorReporting;

/**
 * Provides a callback to when a registered exception is thrown
 */
public interface EvalExceptionCallback
{
	/**
	 * Called for each registered Exception Callback for handling.
	 * 
	 * @param cause
	 *            The thrown exception
	 * @param factory
	 *            The EvalFactory instance
	 * @param result
	 *            The EvalFactoryResult in play
	 * @param level
	 *            The recommend ErrorReporting level
	 * @param message
	 *            The associated error message
	 * @return true is the thrown exception was handled properly, otherwise false which will cause the<br>
	 *         {@link EvalException#exceptionHandler(Throwable t, ShellFactory factory, EvalFactoryResult result, ErrorReporting level, String message )}<br>
	 *         to try the next matched registered exception, then lastly throwing a general eval exception.
	 */
	boolean callback( Throwable cause, ShellFactory factory, EvalFactoryResult result, ErrorReporting level, String message );
}
