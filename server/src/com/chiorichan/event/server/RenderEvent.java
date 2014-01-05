package com.chiorichan.event.server;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import com.chiorichan.framework.CodeParsingException;
import com.chiorichan.framework.Site;
import com.chiorichan.http.HttpRequest;
import com.chiorichan.http.HttpResponse;
import com.chiorichan.http.PersistentSession;

public class RenderEvent extends ServerEvent
{
	private String pageSource, pageHash;
	private final PersistentSession sess;
	private final Map<String, String> pageData;
	
	public RenderEvent(PersistentSession _sess, String source, Map<String, String> _pageData)
	{
		pageSource = source;
		pageHash = DigestUtils.md5Hex( source );
		pageData = _pageData;
		sess = _sess;
	}
	
	public Map<String, String> getPageData()
	{
		return pageData;
	}
	
	public Site getSite()
	{
		return sess.getRequest().getSite();
	}
	
	public String getRequestId()
	{
		return sess.getId();
	}
	
	public PersistentSession getSession()
	{
		return sess;
	}
	
	public HttpRequest getRequest()
	{
		return sess.getRequest();
	}
	
	public HttpResponse getResponse()
	{
		return sess.getResponse();
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
	
	@Deprecated
	public String executeCode( String source ) throws IOException, CodeParsingException
	{
		return sess.getFramework().getServer().executeCode( source );
	}
}
