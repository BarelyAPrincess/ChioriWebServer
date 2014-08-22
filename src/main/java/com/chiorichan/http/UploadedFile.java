package com.chiorichan.http;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.chiorichan.ContentTypes;

/**
 * TODO Upload file to temp dir with random name but preserve original filename
 * 
 * @author Chiori Greene
 */
public class UploadedFile
{
	protected File file;
	protected String origFileName;
	protected long size;
	protected String message;
	
	public UploadedFile(File _file, String _origFileName, long _size, String msg)
	{
		file = _file;
		origFileName = _origFileName;
		size = _size;
		message = msg;
	}
	
	public File getFile()
	{
		return file;
	}
	
	public long getFileSize()
	{
		return size;
	}
	
	public String getMimeType()
	{
		return ContentTypes.getContentType( file );
	}
	
	public String getExt()
	{
		String[] exts = file.getName().split( "\\." );
		return exts[exts.length - 1];
	}
	
	public String getTmpFileName()
	{
		return file.getName();
	}
	
	public String getOrigFileName()
	{
		return origFileName;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String readToString() throws IOException
	{
		return FileUtils.readFileToString( file );
	}
	
	public byte[] readToBytes() throws IOException
	{
		return FileUtils.readFileToByteArray( file );
	}
}
