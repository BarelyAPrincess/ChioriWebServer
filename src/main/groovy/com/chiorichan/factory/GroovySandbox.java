/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.factory;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * Used to prevent Groovy scripts from accessing curtain parts of the Server and JVM.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class GroovySandbox extends CompilationCustomizer
{
	public GroovySandbox()
	{
		super( CompilePhase.CANONICALIZATION );
	}
	
	@Override
	public void call( SourceUnit source, GeneratorContext context, ClassNode classNode ) throws CompilationFailedException
	{
		
	}
}
