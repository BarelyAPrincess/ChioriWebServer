/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2014, Atom Node LLC. All Right Reserved.
 */

package com.chiorichan.framework;

import com.chiorichan.exceptions.ShellExecuteException;
import com.chiorichan.http.FileInterpreter;
import java.io.File;

/**
 *
 * @author Chiori Greene
 */
public interface SeaShell
{
	public boolean doYouHandle( String shellIdent );
	public void eval( FileInterpreter fi, Evaling eval ) throws ShellExecuteException;
	public void evalFile( File file, Evaling aThis ) throws ShellExecuteException;
	public void evalCode( String html, Evaling aThis ) throws ShellExecuteException;
}
