/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.event;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class PostJSMinProcessor implements Listener
{
	@EventHandler( )
	public void onEvent( PostEvalEvent event )
	{
		if ( !event.context().contentType().equals( "application/javascript-x" ) || !event.context().filename().endsWith( "js" ) )
			return;
		
		// A simple way to ignore JS files that might already be minimized
		if ( event.context().filename() != null && event.context().filename().toLowerCase().endsWith( ".min.js" ) )
			return;
		
		String code = event.context().readString();
		List<SourceFile> externs = Lists.newArrayList();
		List<SourceFile> inputs = Arrays.asList( SourceFile.fromCode( ( event.context().filename() == null || event.context().filename().isEmpty() ) ? "fakefile.js" : event.context().filename(), code ) );
		
		Compiler compiler = new Compiler();
		
		CompilerOptions options = new CompilerOptions();
		
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel( options );
		
		compiler.compile( externs, inputs, options );
		
		event.context().resetAndWrite( StringUtils.trimToNull( compiler.toSource() ) );
	}
	
}
