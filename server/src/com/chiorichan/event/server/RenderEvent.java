package com.chiorichan.event.server;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.framework.CodeParsingException;
import com.chiorichan.framework.Framework;
import com.chiorichan.framework.Site;
import com.chiorichan.http.PersistentSession;

public class RenderEvent extends ServerEvent
{
	private String pageSource, pageHash;
	private PersistentSession fw;
	
	// Temporary until we can figure out how to allow templates to register their own extra fields
	public String theme, view, title;
	
	public RenderEvent(PersistentSession sess, String source)
	{
		pageSource = source;
		pageHash = DigestUtils.md5Hex( source );
		fw = sess;
	}
	
	public Site getSite()
	{
		return fw.getRequest().getSite();
	}
	
	public String getRequestId()
	{
		return fw.getId();
	}
	
	public PersistentSession getFramework()
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
	
	public String executeCode( String source ) throws IOException, CodeParsingException
	{
		return fw.getFramework().getServer().executeCode( source );
	}
}
