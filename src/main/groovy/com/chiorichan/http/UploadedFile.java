/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Joel Greene <joel.greene@penoaks.com>
 * Copyright (c) 2017 Penoaks Publishing LLC <development@penoaks.com>
 *
 * All Rights Reserved.
 */
package com.chiorichan.http;

import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.chiorichan.ContentTypes;
import com.chiorichan.utils.UtilEncryption;

/**
 * Acts as the in between for uploaded files and web script
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
	
	public String getExt()
	{
		String[] extensions = origFileName.split( "\\." );
		return extensions[extensions.length - 1];
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
	
	public String getMD5() throws IOException
	{
		if ( isInMemory() || file == null )
			return UtilEncryption.md5( cachedFileUpload.content().array() );
		else
			return UtilEncryption.md5( FileUtils.readFileToByteArray( file ) );
	}
	
	public String getMessage()
	{
		return message;
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
	
	public String getOrigFileName()
	{
		return origFileName;
	}
	
	public String getOrigMineType()
	{
		return ( cachedFileUpload == null ) ? getMimeType() : cachedFileUpload.getContentType();
	}
	
	public String getTmpFileName()
	{
		return ( isInMemory() || file == null ) ? null : file.getName();
	}
	
	public boolean isInMemory()
	{
		if ( cachedFileUpload != null && file == null )
			return cachedFileUpload.isInMemory();
		else
			return file == null;
	}
	
	public byte[] readToBytes() throws IOException
	{
		if ( isInMemory() || file == null )
			return cachedFileUpload.content().array();
		else
			return FileUtils.readFileToByteArray( file );
	}
	
	public String readToString() throws IOException
	{
		if ( isInMemory() || file == null )
			return UtilEncryption.base64Encode( cachedFileUpload.content().array() );
		else
			return UtilEncryption.base64Encode( FileUtils.readFileToByteArray( file ) );
	}
	
	@Override
	public String toString()
	{
		return "UploadedFile(size=" + size + ",tmpFile=" + file + ",origFileName=" + origFileName + ",mimeType=" + getMimeType() + ",message=" + message + ")";
	}
}
