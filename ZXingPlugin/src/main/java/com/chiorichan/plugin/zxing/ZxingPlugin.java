/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.plugin.zxing;

import java.io.IOException;

import org.apache.commons.lang3.Validate;

import com.chiorichan.plugin.lang.PluginException;
import com.chiorichan.plugin.loader.Plugin;

/**
 * TODO Implement more features from ZXing Library
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
public class ZxingPlugin extends Plugin
{
	/**
	 * Create a QR code from the given text. <br/>
	 * <br/>
	 * 
	 * There is a size limitation to how much you can put into a QR code. This has been tested to work with up to a
	 * length of 2950 characters.<br/>
	 * <br/>
	 * 
	 * The QRCode will have the following defaults: <br/>
	 * {size: 100x100}<br/>
	 * {imageType:PNG} <br/>
	 * <br/>
	 * 
	 * Both size and imageType can be overridden: <br/>
	 * Image type override is done by calling {@link QRCode#to(com.chiorichan.plugin.zxing.ImageType)} e.g.
	 * QRCode.from("hello world").to(JPG) <br/>
	 * Size override is done by calling {@link QRCode#withSize} e.g. QRCode.from("hello world").to(JPG).withSize(125,
	 * 125) <br/>
	 * 
	 * @param text
	 *            the text to encode to a new QRCode, this may fail if the text is too large. <br/>
	 * @return the QRCode object <br/>
	 */
	public QRCode createQRCode( String text )
	{
		return new QRCode( text );
	}
	
	public String createQRCodeSimple( String code ) throws IOException
	{
		return createQRCodeSimple( code, 200 );
	}
	
	public String createQRCodeSimple( String code, int size ) throws IOException
	{
		if ( size < 1 )
			return "";
		
		Validate.notNull( code );
		Validate.notEmpty( code );
		
		return new String( createQRCode( code ).withSize( size, size ).setMargin( 1 ).to( ImageType.PNG ).stream().toByteArray(), "ISO-8859-1" );
	}
	
	@Override
	public void onDisable() throws PluginException
	{
		
	}
	
	@Override
	public void onEnable() throws PluginException
	{
		
	}
	
	@Override
	public void onLoad() throws PluginException
	{
		
	}
}
