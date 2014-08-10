package com.chiorichan.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;

import com.chiorichan.Loader;

public class UpdateInstaller
{
	public static void main( String[] args )
	{
		try
		{
			execute( args );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		System.exit( 0 );
	}
	
	private static void execute( String[] args ) throws Exception
	{
		File currentJar = new File( "update.jar" );
		File codeSource = new File( args[0] );
		String maxMemory = args[1];
		codeSource.delete();
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try
		{
			fis = new FileInputStream( currentJar );
			fos = new FileOutputStream( codeSource );
			IOUtils.copy( fis, fos );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeQuietly( fis );
			IOUtils.closeQuietly( fos );
		}
		
		codeSource.setExecutable( true, true );
		
		ProcessBuilder processBuilder = new ProcessBuilder();
		ArrayList<String> commands = new ArrayList<String>();
		
		if ( OperatingSystem.getOperatingSystem().equals( OperatingSystem.WINDOWS ) )
		{
			commands.add( "javaw" );
		}
		else
		{
			commands.add( "java" );
		}
		commands.add( "-Xmx" + maxMemory + "m" );
		commands.add( "-cp" );
		commands.add( codeSource.getAbsolutePath() );
		commands.add( Loader.class.getName() );
		
		//commands.addAll( Arrays.asList( args ) );
		processBuilder.command( commands );
		
		processBuilder.start();
	}
}
