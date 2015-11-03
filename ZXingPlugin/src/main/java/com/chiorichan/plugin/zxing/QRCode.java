/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 */
package com.chiorichan.plugin.zxing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QRCode generator. This is a simple class that is built on top of <a href="http://code.google.com/p/zxing/">ZXING</a><br/>
 * <br/>
 * 
 * Please take a look at their framework, as it has a lot of features. <br/>
 * This small project is just a wrapper that gives a convenient interface to work with. <br/>
 * <br/>
 * 
 * Start here: {@link QRCode#from(String)} (e.g QRCode.from("hello"))
 */
public class QRCode
{
	Writer qrWriter;
	private int width = 125;
	private int height = 125;
	private final String text;
	private ImageType imageType = ImageType.PNG;
	private final Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
	
	protected QRCode( String text )
	{
		this.text = text;
		qrWriter = new QRCodeWriter();
	}
	
	/**
	 * Overrides the imageType from its default {@link ImageType#PNG}
	 * 
	 * @param imageType
	 *            the {@link ImageType} you would like the resulting QR to be
	 * @return the current QRCode object
	 */
	public QRCode to( ImageType imageType )
	{
		this.imageType = imageType;
		return this;
	}
	
	/**
	 * Overrides the size of the qr from its default 125x125
	 * 
	 * @param width
	 *            the width in pixels
	 * @param height
	 *            the height in pixels
	 * @return the current QRCode object
	 */
	public QRCode withSize( int width, int height )
	{
		this.width = width;
		this.height = height;
		return this;
	}
	
	/**
	 * Overrides the default cahrset by supplying a {@link com.google.zxing.EncodeHintType#CHARACTER_SET} hint to {@link com.google.zxing.qrcode.QRCodeWriter#encode}
	 * 
	 * @return the current QRCode object
	 */
	public QRCode withCharset( String charset )
	{
		hints.put( EncodeHintType.CHARACTER_SET, charset );
		return this;
	}
	
	public QRCode setMargin( int margin )
	{
		hints.put( EncodeHintType.MARGIN, margin );
		return this;
	}
	
	/**
	 * returns a {@link File} representation of the QR code. The file is set to be deleted on exit (i.e. {@link java.io.File#deleteOnExit()}). If you want the file to live beyond the life of the jvm process, you should
	 * make a copy.
	 * 
	 * @return qrcode as file
	 */
	@SuppressWarnings( "deprecation" )
	public File file()
	{
		File file;
		try
		{
			file = createTempFile();
			MatrixToImageWriter.writeToFile( createMatrix(), imageType.toString(), file );
		}
		catch ( Exception e )
		{
			throw new QRGenerationException( "Failed to create QR image from text due to underlying exception", e );
		}
		
		return file;
	}
	
	/**
	 * returns a {@link File} representation of the QR code. The file has the given name. The file is set to be deleted
	 * on exit (i.e. {@link java.io.File#deleteOnExit()}). If you want the file to live beyond the life of the jvm
	 * process, you should make a copy.
	 * 
	 * @see #file()
	 * @param name
	 *            name of the created file
	 * @return qrcode as file
	 */
	@SuppressWarnings( "deprecation" )
	public File file( String name )
	{
		File file;
		try
		{
			file = createTempFile( name );
			MatrixToImageWriter.writeToFile( createMatrix(), imageType.toString(), file );
		}
		catch ( Exception e )
		{
			throw new QRGenerationException( "Failed to create QR image from text due to underlying exception", e );
		}
		
		return file;
	}
	
	/**
	 * returns a {@link ByteArrayOutputStream} representation of the QR code
	 * 
	 * @return qrcode as stream
	 */
	public ByteArrayOutputStream stream()
	{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try
		{
			writeToStream( stream );
		}
		catch ( Exception e )
		{
			throw new QRGenerationException( "Failed to create QR image from text due to underlying exception", e );
		}
		
		return stream;
	}
	
	/**
	 * writes a representation of the QR code to the supplied {@link OutputStream}
	 * 
	 * @param stream
	 *            the {@link OutputStream} to write QR Code to
	 */
	public void writeTo( OutputStream stream )
	{
		try
		{
			writeToStream( stream );
		}
		catch ( Exception e )
		{
			throw new QRGenerationException( "Failed to create QR image from text due to underlying exception", e );
		}
	}
	
	private void writeToStream( OutputStream stream ) throws IOException, WriterException
	{
		MatrixToImageWriter.writeToStream( createMatrix(), imageType.toString(), stream );
	}
	
	private BitMatrix createMatrix() throws WriterException
	{
		return qrWriter.encode( text, com.google.zxing.BarcodeFormat.QR_CODE, width, height, hints );
	}
	
	private File createTempFile() throws IOException
	{
		File file = File.createTempFile( "QRCode", "." + imageType.toString().toLowerCase() );
		file.deleteOnExit();
		return file;
	}
	
	private File createTempFile( String name ) throws IOException
	{
		File file = File.createTempFile( name, "." + imageType.toString().toLowerCase() );
		file.deleteOnExit();
		return file;
	}
}
