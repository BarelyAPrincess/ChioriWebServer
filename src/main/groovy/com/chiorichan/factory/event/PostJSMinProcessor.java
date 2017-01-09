/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
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
