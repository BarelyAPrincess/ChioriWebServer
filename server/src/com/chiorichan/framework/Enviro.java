package com.chiorichan.framework;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

public class Enviro
{
	Binding binding = new Binding();
	
	public Enviro(Framework fw)
	{
		binding.setVariable( "__FILE__", new File( "" ) );
		setFramework( fw );
	}
	
	public void setFramework( Framework fw )
	{
		binding.setVariable( "chiori", fw );
	}
	
	public void set( String name, Object value )
	{
		binding.setVariable( name, value );
	}
	
	public Evaling newEval()
	{
		Evaling eval = ( (Framework) binding.getVariable( "chiori" ) ).eval;
		
		if ( eval == null )
			eval = new Evaling( binding );
		
		// return new Evaling( binding );
		
		return eval;
	}
}
