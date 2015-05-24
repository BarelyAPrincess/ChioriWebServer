/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory.postprocessors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.chiorichan.factory.EvalMetaData;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;

/**
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class JSMinPostProcessor implements PostProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return new String[] {"js", "application/javascript-x"};
	}
	
	@Override
	public ByteBuf process( EvalMetaData meta, ByteBuf buf ) throws Exception
	{
		// A simple way to ignore JS files that might already be minimized
		if ( meta.fileName != null && meta.fileName.toLowerCase().endsWith( ".min.js" ) )
			return buf;
		
		String code = buf.toString( Charset.defaultCharset() );
		List<SourceFile> externs = Lists.newArrayList();
		List<SourceFile> inputs = Arrays.asList( SourceFile.fromCode( ( meta.fileName == null || meta.fileName.isEmpty() ) ? "fakefile.js" : meta.fileName, code ) );
		
		Compiler compiler = new Compiler();
		
		CompilerOptions options = new CompilerOptions();
		
		CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel( options );
		
		compiler.compile( externs, inputs, options );
		
		return Unpooled.buffer().writeBytes( StringUtils.trimToNull( compiler.toSource() ).getBytes() );
	}
	
}
