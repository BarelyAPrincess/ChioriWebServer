/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.factory.event;

import io.netty.buffer.ByteBufInputStream;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import com.chiorichan.ConsoleColor;
import com.chiorichan.Loader;
import com.chiorichan.event.EventHandler;
import com.chiorichan.event.Listener;
import com.chiorichan.factory.EvalContext;
import com.chiorichan.factory.EvalFactory;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.util.StringFunc;

/**
 * Applies special builtin image filters post {@link EvalFactory} via {@link PostEvalEvent}
 *
 * @author Chiori Greene, a.k.a. Chiori-chan {@literal <me@chiorichan.com>}
 */
public class PostImageProcessor implements Listener
{
	static class RGBColorFilter extends RGBImageFilter
	{
		private final int filter;
		
		RGBColorFilter( int filter )
		{
			this.filter = filter;
			canFilterIndexColorModel = true;
		}
		
		@Override
		public int filterRGB( int x, int y, int rgb )
		{
			return rgb & filter;
		}
	}
	
	@EventHandler( )
	public void onEvent( PostEvalEvent event )
	{
		try
		{
			if ( event.context().contentType() == null || !event.context().contentType().toLowerCase().startsWith( "image" ) )
				return;
			
			float x = -1;
			float y = -1;
			
			boolean cacheEnabled = Loader.getConfig().getBoolean( "advanced.processors.imageProcessorCache", true );
			boolean grayscale = false;
			
			EvalContext context = event.context();
			HttpRequestWrapper request = context.request();
			Map<String, String> rewrite = request.getRewriteMap();
			
			if ( rewrite != null )
			{
				if ( rewrite.get( "serverSideOptions" ) != null )
				{
					String[] params = rewrite.get( "serverSideOptions" ).trim().split( "_" );
					
					for ( String p : params )
						if ( p.toLowerCase().startsWith( "width" ) && x < 0 )
							x = Integer.parseInt( p.substring( 5 ) );
						else if ( ( p.toLowerCase().startsWith( "x" ) || p.toLowerCase().startsWith( "w" ) ) && p.length() > 1 && x < 0 )
							x = Integer.parseInt( p.substring( 1 ) );
						else if ( p.toLowerCase().startsWith( "height" ) && y < 0 )
							y = Integer.parseInt( p.substring( 6 ) );
						else if ( ( p.toLowerCase().startsWith( "y" ) || p.toLowerCase().startsWith( "h" ) ) && p.length() > 1 && y < 0 )
							y = Integer.parseInt( p.substring( 1 ) );
						else if ( p.toLowerCase().equals( "thumb" ) )
						{
							x = 150;
							y = 0;
							break;
						}
						else if ( p.toLowerCase().equals( "bw" ) || p.toLowerCase().equals( "grayscale" ) )
							grayscale = true;
				}
				
				if ( request.getArgument( "width" ) != null && request.getArgument( "width" ).length() > 0 )
					x = request.getArgumentInt( "width" );
				
				if ( request.getArgument( "height" ) != null && request.getArgument( "height" ).length() > 0 )
					y = request.getArgumentInt( "height" );
				
				if ( request.getArgument( "w" ) != null && request.getArgument( "w" ).length() > 0 )
					x = request.getArgumentInt( "w" );
				
				if ( request.getArgument( "h" ) != null && request.getArgument( "h" ).length() > 0 )
					y = request.getArgumentInt( "h" );
				
				if ( request.getArgument( "thumb" ) != null )
				{
					x = 150;
					y = 0;
				}
				
				if ( request.hasArgument( "bw" ) || request.hasArgument( "grayscale" ) )
					grayscale = true;
			}
			
			// Tests if our Post Processor can process the current image.
			List<String> readerFormats = Arrays.asList( ImageIO.getReaderFormatNames() );
			List<String> writerFormats = Arrays.asList( ImageIO.getWriterFormatNames() );
			if ( context.contentType() != null && !readerFormats.contains( context.contentType().split( "/" )[1].toLowerCase() ) )
				return;
			
			int inx = event.context().buffer().readerIndex();
			BufferedImage img = ImageIO.read( new ByteBufInputStream( event.context().buffer() ) );
			event.context().buffer().readerIndex( inx );
			
			if ( img == null )
				return;
			
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
			
			boolean resize = w1 > 0 && h1 > 0 && w1 != w && h1 != h;
			boolean argb = request.hasArgument( "argb" ) && request.getArgument( "argb" ).length() == 8;
			
			if ( !resize && !argb && !grayscale )
				return;
			
			// Produce a unique encapsulated id based on this image processing request
			String encapId = StringFunc.md5( context.filename() + w1 + h1 + request.getArgument( "argb" ) + grayscale );
			File tmp = context.site() == null ? Loader.getTempFileDirectory() : context.site().getTempFileDirectory();
			File file = new File( tmp, encapId + "_" + new File( context.filename() ).getName() );
			
			if ( cacheEnabled && file.exists() )
			{
				event.context().resetAndWrite( FileUtils.readFileToByteArray( file ) );
				return;
			}
			
			Image image = resize ? img.getScaledInstance( Math.round( w1 ), Math.round( h1 ), Loader.getConfig().getBoolean( "advanced.processors.useFastGraphics", true ) ? Image.SCALE_FAST : Image.SCALE_SMOOTH ) : img;
			
			// TODO Report malformed parameters to user
			
			if ( argb )
			{
				FilteredImageSource filteredSrc = new FilteredImageSource( image.getSource(), new RGBColorFilter( ( int ) Long.parseLong( request.getArgument( "argb" ), 16 ) ) );
				image = Toolkit.getDefaultToolkit().createImage( filteredSrc );
			}
			
			BufferedImage rtn = new BufferedImage( Math.round( w1 ), Math.round( h1 ), img.getType() );
			Graphics2D graphics = rtn.createGraphics();
			graphics.drawImage( image, 0, 0, null );
			graphics.dispose();
			
			if ( grayscale )
			{
				ColorConvertOp op = new ColorConvertOp( ColorSpace.getInstance( ColorSpace.CS_GRAY ), null );
				op.filter( rtn, rtn );
			}
			
			if ( resize )
				Loader.getLogger().info( ConsoleColor.GRAY + "Resized image from " + Math.round( w ) + "px by " + Math.round( h ) + "px to " + Math.round( w1 ) + "px by " + Math.round( h1 ) + "px" );
			
			if ( rtn != null )
			{
				ByteArrayOutputStream bs = new ByteArrayOutputStream();
				
				if ( context.contentType() != null && writerFormats.contains( context.contentType().split( "/" )[1].toLowerCase() ) )
					ImageIO.write( rtn, context.contentType().split( "/" )[1].toLowerCase(), bs );
				else
					ImageIO.write( rtn, "png", bs );
				
				if ( cacheEnabled && !file.exists() )
					FileUtils.writeByteArrayToFile( file, bs.toByteArray() );
				
				event.context().resetAndWrite( bs.toByteArray() );
			}
		}
		catch ( Throwable e )
		{
			e.printStackTrace();
		}
		
		return;
	}
}
