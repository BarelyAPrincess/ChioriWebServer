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

import javax.imageio.ImageIO;

import com.chiorichan.ConsoleColor;
import com.chiorichan.ContentTypes;
import com.chiorichan.Loader;
import com.chiorichan.factory.EvalMetaData;
import com.google.zxing.common.detector.MathUtils;

public class ImagePostProcessor implements PostProcessor
{
	@Override
	public String[] getHandledTypes()
	{
		return ContentTypes.getAllTypes( "image" );
	}
	
	@Override
	public ByteBuf process( EvalMetaData meta, ByteBuf buf ) throws Exception
	{
		float x = 0;
		float y = 0;
		
		if ( meta.params != null )
		{
			if ( meta.params.get( "serverSideOptions" ) != null )
			{
				String[] params = meta.params.get( "serverSideOptions" ).trim().split( "_" );
				
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
				x = Integer.parseInt( meta.params.get( "width" ) );
			
			if ( meta.params.get( "height" ) != null )
				y = Integer.parseInt( meta.params.get( "height" ) );
			
			if ( meta.params.get( "w" ) != null )
				x = Integer.parseInt( meta.params.get( "w" ) );
			
			if ( meta.params.get( "h" ) != null )
				y = Integer.parseInt( meta.params.get( "h" ) );
			
			if ( meta.params.get( "thumb" ) != null )
			{
				x = 150;
				y = 0;
			}
		}
		
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
				
				Image image = img.getScaledInstance( MathUtils.round( w1 ), MathUtils.round( h1 ), Loader.getConfig().getBoolean( "advanced.processors.useFastGraphics", true ) ? Image.SCALE_FAST : Image.SCALE_SMOOTH );
				
				BufferedImage rtn = new BufferedImage( MathUtils.round( w1 ), MathUtils.round( h1 ), img.getType() );
				Graphics2D graphics = rtn.createGraphics();
				graphics.drawImage( image, 0, 0, null );
				graphics.dispose();
				
				Loader.getLogger().info( ConsoleColor.GRAY + "Resized image from " + MathUtils.round( w ) + "px by " + MathUtils.round( h ) + "px to " + MathUtils.round( w1 ) + "px by " + MathUtils.round( h1 ) + "px" );
				
				if ( rtn != null )
				{
					ByteArrayOutputStream bs = new ByteArrayOutputStream();
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
