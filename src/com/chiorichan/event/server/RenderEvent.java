package com.chiorichan.event.server;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;

import bsh.EvalError;

import com.chiorichan.framework.CodeParsingException;
import com.chiorichan.framework.Framework;
import com.chiorichan.framework.Site;

public class RenderEvent extends ServerEvent
{
	private String pageSource, pageHash;
	private Framework fw;
	
	// Temporary until we can figure out how to allow templates to register their own extra fields
	public String theme, view, title;
	
	public RenderEvent(Framework fw0, String source)
	{
		pageSource = source;
		pageHash = DigestUtils.md5Hex( source );
		fw = fw0;
	}
	
	public Site getSite()
	{
		return fw.getCurrentSite();
	}
	
	public String getRequestId()
	{
		return fw.getRequestId();
	}
	
	public Framework getFramework()
	{
		return fw;
	}
	
	public String getSource()
	{
		return pageSource;
	}
	
	public void setSource( String source )
	{
		pageSource = source;
	}
	
	public boolean sourceChanged()
	{
		return !DigestUtils.md5( pageSource ).equals( pageHash );
	}
	
	public String executeCode( String source ) throws IOException, EvalError, CodeParsingException
	{
		return fw.getServer().executeCode( source );
	}
}
