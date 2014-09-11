/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2014 Chiori-chan. All Right Reserved.
 *
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.console.ConsoleReader;

public class TerminalConsoleHandler extends ConsoleHandler
{
	private final ConsoleReader reader;
	
	public TerminalConsoleHandler(ConsoleReader reader)
	{
		super();
		this.reader = reader;
	}
	
	@Override
	public synchronized void flush()
	{
		try
		{
			reader.print( ConsoleReader.RESET_LINE + "" );
			reader.flush();
			super.flush();
			try
			{
				reader.drawLine();
			}
			catch ( Throwable ex )
			{
				reader.getCursorBuffer().clear();
			}
			reader.flush();;
		}
		catch ( IOException ex )
		{
			Logger.getLogger( TerminalConsoleHandler.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}
}
