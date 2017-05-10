/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http.multipart;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A <code>FilePart</code> is an upload part which represents a <code>INPUT TYPE="file"</code> form parameter. Note that because file
 * upload data arrives via a single InputStream, each FilePart's contents
 * must be read before moving onto the next part. Don't try to store a
 * FilePart object for later processing because by then their content will
 * have been passed by.
 */
public class FilePart extends Part
{
	
	/** "file system" name of the file */
	private String fileName;
	private String tmpFileName;
	
	/** path of the file as sent in the request, if given */
	private String filePath;
	
	/** content type of the file */
	private String contentType;
	
	byte[] data;
	
	/**
	 * Construct a file part; this is called by the parser.
	 * 
	 * @param name
	 *            the name of the parameter.
	 * @param buffer
	 *            the servlet input stream to read the file from.
	 * @param boundary
	 *            the MIME boundary that delimits the end of file.
	 * @param contentType
	 *            the content type of the file provided in the
	 *            MIME header.
	 * @param fileName
	 *            the file system name of the file provided in the
	 *            MIME header.
	 * @param filePath
	 *            the file system path of the file provided in the
	 *            MIME header (as specified in disposition info).
	 * 
	 * @exception IOException
	 *                if an input or output exception has occurred.
	 */
	FilePart( String name, byte[] data, String contentType, String fileName, String filePath ) throws IOException
	{
		super( name );
		this.fileName = fileName;
		this.filePath = filePath;
		this.contentType = contentType;
		this.data = data;
	}
	
	/**
	 * Returns the name that the file was stored with on the remote system,
	 * or <code>null</code> if the user didn't enter a file to be uploaded.
	 * Note: this is not the same as the name of the form parameter used to
	 * transmit the file; that is available from the <code>getName</code> method. Further note: if file rename logic is in effect, the file
	 * name can change during the writeTo() method when there's a collision
	 * with another file of the same name in the same directory. If this
	 * matters to you, be sure to pay attention to when you call the method.
	 * 
	 * @return name of file uploaded or <code>null</code>.
	 * 
	 * @see Part#getName()
	 */
	public String getFileName()
	{
		return fileName;
	}
	
	/**
	 * Warning: Temp File Name is not determined until writeTo is called.
	 */
	public String getTmpFileName()
	{
		return tmpFileName;
	}
	
	/**
	 * Returns the full path and name of the file on the remote system,
	 * or <code>null</code> if the user didn't enter a file to be uploaded.
	 * If path information was not supplied by the remote system, this method
	 * will return the same as <code>getFileName()</code>.
	 *
	 * @return path of file uploaded or <code>null</code>.
	 *
	 * @see Part#getName()
	 */
	public String getFilePath()
	{
		return filePath;
	}
	
	/**
	 * Returns the content type of the file data contained within.
	 * 
	 * @return content type of the file data.
	 */
	public String getContentType()
	{
		return contentType;
	}
	
	/**
	 * Returns an input stream which contains the contents of the
	 * file supplied. If the user didn't enter a file to upload
	 * there will be <code>0</code> bytes in the input stream.
	 * It's important to read the contents of the InputStream
	 * immediately and in full before proceeding to process the
	 * next part. The contents will otherwise be lost on moving
	 * to the next part.
	 * 
	 * @return an input stream containing contents of file.
	 */
	public InputStream getInputStream()
	{
		return new ByteArrayInputStream( data );
	}
	
	/**
	 * Write this file part to a file or directory. If the user
	 * supplied a file, we write it to that file, and if they supplied
	 * a directory, we write it to that directory with the filename
	 * that accompanied it. If this part doesn't contain a file this
	 * method does nothing.
	 *
	 * @return number of bytes written
	 * @exception IOException
	 *                if an input or output exception has occurred.
	 */
	public long writeTo( File fileOrDirectory ) throws IOException
	{
		long written = 0;
		
		OutputStream fileOut = null;
		try
		{
			// Only do something if this part contains a file
			if ( fileName != null )
			{
				// Check if user supplied directory
				File file;
				if ( fileOrDirectory.isDirectory() )
				{
					// Write it to that dir the user supplied,
					// with the filename it arrived with
					file = new File( fileOrDirectory, fileName );
				}
				else
				{
					// Write it to the file the user supplied,
					// ignoring the filename it arrived with
					file = fileOrDirectory;
				}
				
				file = checkFile( file );
				tmpFileName = file.getName();
				
				fileOut = new BufferedOutputStream( new FileOutputStream( file ) );
				written = write( fileOut );
			}
		}
		finally
		{
			if ( fileOut != null )
				fileOut.close();
		}
		return written;
	}
	
	private File checkFile( File f )
	{
		if ( createNewFile( f ) )
		{
			return f;
		}
		String name = f.getName();
		String body = null;
		String ext = null;
		
		int dot = name.lastIndexOf( "." );
		if ( dot != -1 )
		{
			body = name.substring( 0, dot );
			ext = name.substring( dot ); // includes "."
		}
		else
		{
			body = name;
			ext = "";
		}
		
		// Increase the count until an empty spot is found.
		// Max out at 9999 to avoid an infinite loop caused by a persistent
		// IOException, like when the destination dir becomes non-writable.
		// We don't pass the exception up because our job is just to rename,
		// and the caller will hit any IOException in normal processing.
		int count = 0;
		while ( !createNewFile( f ) && count < 9999 )
		{
			count++;
			String newName = body + count + ext;
			f = new File( f.getParent(), newName );
		}
		
		return f;
	}
	
	private boolean createNewFile( File f )
	{
		try
		{
			return f.createNewFile();
		}
		catch ( IOException ignored )
		{
			return false;
		}
	}
	
	/**
	 * Write this file part to the given output stream. If this part doesn't
	 * contain a file this method does nothing.
	 *
	 * @return number of bytes written.
	 * @exception IOException
	 *                if an input or output exception has occurred.
	 */
	public long writeTo( OutputStream out ) throws IOException
	{
		long size = 0;
		// Only do something if this part contains a file
		if ( fileName != null )
		{
			// Write it out
			size = write( out );
		}
		return size;
	}
	
	/**
	 * Internal method to write this file part; doesn't check to see
	 * if it has contents first.
	 *
	 * @return number of bytes written.
	 * @exception IOException
	 *                if an input or output exception has occurred.
	 */
	long write( OutputStream out ) throws IOException
	{
		// decode macbinary if this was sent
		if ( contentType.equals( "application/x-macbinary" ) )
		{
			out = new MacBinaryDecoderOutputStream( out );
		}
		for ( byte b : data )
		{
			out.write( b );
		}
		return data.length;
	}
	
	/**
	 * Returns <code>true</code> to indicate this part is a file.
	 * 
	 * @return true.
	 */
	@Override
	public boolean isFile()
	{
		return true;
	}
}
