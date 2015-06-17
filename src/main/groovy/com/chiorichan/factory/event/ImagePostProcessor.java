/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.event;

import io.netty.buffer.ByteBufInputStream;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;
import com.chiorichan.factory.EvalExecutionContext;
import com.chiorichan.http.HttpRequestWrapper;

public class ImagePostProcessor implements Listener
{
	@EventHandler( )
	public void onEvent( EvalFactoryPostEvent event )
	{
		if ( !Arrays.asList( ContentTypes.getAllTypes( "image" ) ).contains( event.context().contentType() ) )
			return;
		
		float x = 0;
		float y = 0;
		
		EvalExecutionContext context = event.context();
		HttpRequestWrapper request = context.request();
		Map<String, String> rewrite = request.getRewriteMap();
		
		if ( rewrite != null )
		{
			if ( rewrite.get( "serverSideOptions" ) != null )
			{
				String[] params = rewrite.get( "serverSideOptions" ).trim().split( "_" );
				
				for ( String p : params )
				{
					if ( p.toLowerCase().startsWith( "width" ) && p.length() > 1 )
						x = Integer.parseInt( p.substring( 5 ) );
					else if ( ( p.toLowerCase().startsWith( "x" ) || p.toLowerCase().startsWith( "w" ) ) && p.length() > 1 )
						x = Integer.parseInt( p.substring( 1 ) );
					else if ( p.toLowerCase().startsWith( "height" ) && p.length() > 1 )
						y = Integer.parseInt( p.substring( 6 ) );
					else if ( ( p.toLowerCase().startsWith( "y" ) || p.toLowerCase().startsWith( "h" ) ) && p.length() > 1 )
						y = Integer.parseInt( p.substring( 1 ) );
					else if ( p.toLowerCase().equals( "thumb" ) )
					{
						x = 150;
						y = 0;
						break;
					}
				}
			}
			
			if ( request.getArgument( "width" ) != null )
				x = request.getArgumentInt( "width" );
			
			if ( request.getArgument( "height" ) != null )
				y = request.getArgumentInt( "height" );
			
			if ( request.getArgument( "w" ) != null )
				x = request.getArgumentInt( "w" );
			
			if ( request.getArgument( "h" ) != null )
				y = request.getArgumentInt( "h" );
			
			if ( request.getArgument( "thumb" ) != null )
			{
				x = 150;
				y = 0;
			}
		}
		
		// Tests if our Post Processor can process the current image.
		List<String> readerFormats = Arrays.asList( ImageIO.getReaderFormatNames() );
		List<String> writerFormats = Arrays.asList( ImageIO.getWriterFormatNames() );
		if ( context.contentType() != null && !readerFormats.contains( context.contentType().split( "/" )[1].toLowerCase() ) )
			return;
		
		try
		{
			BufferedImage img = ImageIO.read( new ByteBufInputStream( event.context().buffer() ) );
			
			if ( img != null )
			{
				float w = img.getWidth();
				float h = img.getHeight();
				float w1 = w;
				float h1 = h;
				
				if ( x < 1 && y < 1 )
				{
					x = w;
					y = h;
				}
				else if ( x > 0 && y < 1 )
				{
					w1 = x;
					h1 = x * ( h / w );
				}
				else if ( y > 0 && x < 1 )
				{
					w1 = y * ( w / h );
					h1 = y;
				}
				else if ( x > 0 && y > 0 )
				{
					w1 = x;
					h1 = y;
				}
				
				if ( w1 < 1 || h1 < 1 || ( w1 == w && h1 == h ) )
					return;
				
				Image image = img.getScaledInstance( Math.round( w1 ), Math.round( h1 ), Loader.getConfig().getBoolean( "advanced.processors.useFastGraphics", true ) ? Image.SCALE_FAST : Image.SCALE_SMOOTH );
				
				BufferedImage rtn = new BufferedImage( Math.round( w1 ), Math.round( h1 ), img.getType() );
				Graphics2D graphics = rtn.createGraphics();
				graphics.drawImage( image, 0, 0, null );
				graphics.dispose();
				
				Loader.getLogger().info( ConsoleColor.GRAY + "Resized image from " + Math.round( w ) + "px by " + Math.round( h ) + "px to " + Math.round( w1 ) + "px by " + Math.round( h1 ) + "px" );
				
				if ( rtn != null )
				{
					ByteArrayOutputStream bs = new ByteArrayOutputStream();
					
					if ( context.contentType() != null && writerFormats.contains( context.contentType().split( "/" )[1].toLowerCase() ) )
						ImageIO.write( rtn, context.contentType().split( "/" )[1].toLowerCase(), bs );
					else
						ImageIO.write( rtn, "png", bs );
					
					event.context().resetAndWrite( bs.toByteArray() );
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		return;
	}
}
