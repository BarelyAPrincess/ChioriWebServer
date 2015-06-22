/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory;

import java.util.LinkedList;
import java.util.List;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * This compilation customizer allows addiing various types of imports to the compilation unit. Supports adding :
 * <ul>
 * <li>standard imports thanks to {@link #addImport(String)}, {@link #addImport(String, String)} or {@link #addImports(String...)}</li>
 * <li>star imports thanks to {@link #addStarImport(String)} or {@link #addStarImports(String...)}</li>
 * <li>static imports thanks to {@link #addStaticImport(String, String)} or {@link #addStaticImport(String, String, String)}</li>
 * <li>static star imports thanks to {@link #addStaticStar(String)} or {@link #addStaticStars(String...)}</li>
 * </ul>
 *
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class GroovyImportCustomizer extends CompilationCustomizer
{
	private final List<Import> imports = new LinkedList<Import>();
	
	public GroovyImportCustomizer()
	{
		super( CompilePhase.CONVERSION );
	}
	
	@Override
	public void call( final SourceUnit source, final GeneratorContext context, final ClassNode classNode ) throws CompilationFailedException
	{
		final ModuleNode ast = source.getAST();
		for ( Import anImport : imports )
		{
			switch ( anImport.type )
			{
				case regular:
					ast.addImport( anImport.alias, anImport.classNode );
					break;
				case staticImport:
					ast.addStaticImport( anImport.classNode, anImport.field, anImport.alias );
					break;
				case staticStar:
					ast.addStaticStarImport( anImport.alias, anImport.classNode );
					break;
				case star:
					ast.addStarImport( anImport.star );
					break;
			}
		}
	}
	
	public GroovyImportCustomizer addImport( final String alias, final String className )
	{
		imports.add( new Import( ImportType.regular, alias, ClassHelper.make( className ) ) );
		return this;
	}
	
	public GroovyImportCustomizer addStaticImport( final String className, final String fieldName )
	{
		final ClassNode node = ClassHelper.make( className );
		imports.add( new Import( ImportType.staticImport, fieldName, node, fieldName ) );
		return this;
	}
	
	public GroovyImportCustomizer addStaticStars( final String... classNames )
	{
		for ( String className : classNames )
		{
			addStaticStar( className );
		}
		return this;
	}
	
	public GroovyImportCustomizer addStaticImport( final String alias, final String className, final String fieldName )
	{
		imports.add( new Import( GroovyImportCustomizer.ImportType.staticImport, alias, ClassHelper.make( className ), fieldName ) );
		return this;
	}
	
	public GroovyImportCustomizer addImports( final Class<?>... imports )
	{
		for ( Class<?> anImport : imports )
		{
			addImport( anImport );
		}
		return this;
	}
	
	public GroovyImportCustomizer addImports( final String... imports )
	{
		for ( String anImport : imports )
		{
			addImport( anImport );
		}
		return this;
	}
	
	public GroovyImportCustomizer addStarImports( final String... packageNames )
	{
		for ( String packageName : packageNames )
		{
			addStarImport( packageName );
		}
		return this;
	}
	
	private void addImport( final Class<?> clz )
	{
		final ClassNode node = ClassHelper.make( clz );
		imports.add( new Import( ImportType.regular, node.getNameWithoutPackage(), node ) );
	}
	
	private void addImport( final String className )
	{
		final ClassNode node = ClassHelper.make( className );
		imports.add( new Import( ImportType.regular, node.getNameWithoutPackage(), node ) );
	}
	
	private void addStaticStar( final String className )
	{
		imports.add( new Import( GroovyImportCustomizer.ImportType.staticStar, className, ClassHelper.make( className ) ) );
	}
	
	private void addStarImport( final String packagename )
	{
		final String packageNameEndingWithDot = packagename.endsWith( "." ) ? packagename : packagename + '.';
		imports.add( new Import( ImportType.star, packageNameEndingWithDot ) );
	}
	
	// -------------------- Helper classes -------------------------
	
	/**
	 * Represents imports which are possibly aliased.
	 */
	private static class Import
	{
		final ImportType type;
		final ClassNode classNode;
		final String alias;
		final String field;
		final String star; // only used for star imports
		
		private Import( final ImportType type, final String alias, final ClassNode classNode, final String field )
		{
			this.alias = alias;
			this.classNode = classNode;
			this.field = field;
			this.type = type;
			this.star = null;
		}
		
		private Import( final ImportType type, final String alias, final ClassNode classNode )
		{
			this.alias = alias;
			this.classNode = classNode;
			this.type = type;
			this.field = null;
			this.star = null;
		}
		
		private Import( final ImportType type, final String star )
		{
			this.type = type;
			this.star = star;
			this.alias = null;
			this.classNode = null;
			this.field = null;
		}
	}
	
	private enum ImportType
	{
		regular, staticImport, staticStar, star
	}
}
