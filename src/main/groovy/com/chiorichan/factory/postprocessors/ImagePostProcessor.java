/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.factory.postprocessors;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

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
import com.chiorichan.factory.EvalMetaData;

public class ImagePostProcessor implements PostProcessor
{
	/**
	 * A cached array of content types that start with image, e.g., image/jpeg
	 */
	private static final String[] types = ContentTypes.getAllTypes( "image" );
	
	@Override
	public String[] getHandledTypes()
	{
		return types;
	}
	
	@Override
	public ByteBuf process( EvalMetaData meta, ByteBuf buf ) throws Exception
	{
		float x = 0;
		float y = 0;
		
		Map<String, String> paramsRaw = meta.getParamStrings();
		
		if ( paramsRaw != null )
		{
			if ( paramsRaw.get( "serverSideOptions" ) != null )
			{
				String[] params = paramsRaw.get( "serverSideOptions" ).trim().split( "_" );
				
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
			
			if ( meta.params.get( "width" ) != null )
				x = Integer.parseInt( paramsRaw.get( "width" ) );
			
			if ( meta.params.get( "height" ) != null )
				y = Integer.parseInt( paramsRaw.get( "height" ) );
			
			if ( meta.params.get( "w" ) != null )
				x = Integer.parseInt( paramsRaw.get( "w" ) );
			
			if ( meta.params.get( "h" ) != null )
				y = Integer.parseInt( paramsRaw.get( "h" ) );
			
			if ( meta.params.get( "thumb" ) != null )
			{
				x = 150;
				y = 0;
			}
		}
		
		// Tests if our Post Processor can process the current image.
		List<String> readerFormats = Arrays.asList( ImageIO.getReaderFormatNames() );
		List<String> writerFormats = Arrays.asList( ImageIO.getWriterFormatNames() );
		if ( meta.contentType != null && !readerFormats.contains( meta.contentType.split( "/" )[1].toLowerCase() ) )
			return null;
		
		try
		{
			int inx = buf.readerIndex();
			BufferedImage img = ImageIO.read( new ByteBufInputStream( buf ) );
			buf.readerIndex( inx );
			
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
					return null;
				
				Image image = img.getScaledInstance( Math.round( w1 ), Math.round( h1 ), Loader.getConfig().getBoolean( "advanced.processors.useFastGraphics", true ) ? Image.SCALE_FAST : Image.SCALE_SMOOTH );
				
				BufferedImage rtn = new BufferedImage( Math.round( w1 ), Math.round( h1 ), img.getType() );
				Graphics2D graphics = rtn.createGraphics();
				graphics.drawImage( image, 0, 0, null );
				graphics.dispose();
				
				Loader.getLogger().info( ConsoleColor.GRAY + "Resized image from " + Math.round( w ) + "px by " + Math.round( h ) + "px to " + Math.round( w1 ) + "px by " + Math.round( h1 ) + "px" );
				
				if ( rtn != null )
				{
					ByteArrayOutputStream bs = new ByteArrayOutputStream();
					
					if ( meta.contentType != null && writerFormats.contains( meta.contentType.split( "/" )[1].toLowerCase() ) )
						ImageIO.write( rtn, meta.contentType.split( "/" )[1].toLowerCase(), bs );
					else
						ImageIO.write( rtn, "png", bs );
					
					return Unpooled.buffer().writeBytes( bs.toByteArray() );
				}
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
}
