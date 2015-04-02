/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright 2015 Chiori-chan. All Right Reserved.
 * 
 * @author Chiori Greene
 * @email chiorigreene@gmail.com
 */
package com.chiorichan.http;

import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import sun.misc.BASE64Encoder;

import com.chiorichan.ContentTypes;
import com.chiorichan.util.StringUtil;

/**
 * TODO Upload file to temp dir with random name but preserve original filename
 */
public class UploadedFile
{
	protected File file;
	protected String origFileName;
	protected long size;
	protected String message;
	protected FileUpload cachedFileUpload = null;
	
	public UploadedFile( File file, String origFileName, long size, String msg )
	{
		this.file = file;
		this.origFileName = origFileName;
		this.size = size;
		message = msg;
	}
	
	public UploadedFile( FileUpload fileUpload ) throws IOException
	{
		cachedFileUpload = fileUpload;
		
		origFileName = fileUpload.getFilename();
		size = fileUpload.length();
		message = "File upload was successful!";
		
		if ( !fileUpload.isInMemory() )
			file = fileUpload.getFile();
	}
	
	/**
	 * Gets the file object for the temp file.
	 * The file is not long lived and could be deleted as soon as the request finishes,
	 * so please save the file to a database or move it to another location to minimize problems.
	 * 
	 * @return The temporary file.
	 */
	public File getFile()
	{
		return getFile( false );
	}
	
	/**
	 * Get the file object for the temp file.
	 * The file is not long lived and could be deleted as soon as the request finishes,
	 * so please save the file to a database or move it to another location to minimize problems.
	 * 
	 * @param forceToFile
	 *            If this uploaded file is stored in the memory, you can force the creation of a temporary file with this argument.
	 * @return The temporary file.
	 */
	public File getFile( boolean forceToFile )
	{
		if ( file == null && forceToFile )
		{
			file = new File( DiskFileUpload.baseDirectory, getOrigFileName() );
			
			file.getParentFile().mkdirs();
			
			try
			{
				FileUtils.writeByteArrayToFile( file, cachedFileUpload.content().array() );
				// file.deleteOnExit();
				return file;
			}
			catch ( IOException e )
			{
				e.printStackTrace();
				file = null;
				return null;
			}
		}
		else
			return file;
	}
	
	public long getFileSize()
	{
		return size;
	}
	
	public String getMimeType()
	{
		if ( isInMemory() )
			return getOrigMineType();
		else if ( file == null )
			return null;
		else
			return ContentTypes.getContentType( file );
	}
	
	public String getOrigMineType()
	{
		return ( cachedFileUpload == null ) ? getMimeType() : cachedFileUpload.getContentType();
	}
	
	public String getExt()
	{
		String[] exts = origFileName.split( "\\." );
		return exts[exts.length - 1];
	}
	
	public String getTmpFileName()
	{
		return ( isInMemory() || file == null ) ? null : file.getName();
	}
	
	public String getOrigFileName()
	{
		return origFileName;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public boolean isInMemory()
	{
		if ( cachedFileUpload != null && file == null )
			return cachedFileUpload.isInMemory();
		else
			return file == null;
	}
	
	public String getMD5() throws IOException
	{
		if ( isInMemory() || file == null )
			return StringUtil.md5( cachedFileUpload.content().array() );
		else
			return StringUtil.md5( FileUtils.readFileToByteArray( file ) );
	}
	
	public String readToString() throws IOException
	{
		if ( isInMemory() || file == null )
			return new BASE64Encoder().encode( cachedFileUpload.content().array() );
		// return new String( cachedFileUpload.content().array(), cachedFileUpload.getContentTransferEncoding() );
		else
		{
			return new BASE64Encoder().encode( FileUtils.readFileToByteArray( file ) );
			// String s = FileUtils.readFileToString( file );
			// Loader.getLogger().debug( s );
			// return s;
		}
	}
	
	public byte[] readToBytes() throws IOException
	{
		if ( isInMemory() || file == null )
			return cachedFileUpload.content().array();
		else
			return FileUtils.readFileToByteArray( file );
	}
	
	@Override
	public String toString()
	{
		return "UploadedFile(size=" + size + ",tmpFile=" + file + ",origFileName=" + origFileName + ",mimeType=" + getMimeType() + ",message=" + message + ")";
	}
}
