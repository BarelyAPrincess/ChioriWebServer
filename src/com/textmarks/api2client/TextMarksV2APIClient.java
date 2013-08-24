package com.textmarks.api2client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class TextMarksV2APIClient
{
	static public String API_URL_BASE = "http://java1.api2.textmarks.com";
	
	/**
	 * Create TextMarksV2APIClient around indicated authentication info (optional).
	 * 
	 * @param sApiKey
	 *           API Key ( register at https://www.textmarks.com/manage/account/profile/api_keys/ ). (null for none).
	 * @param sAuthUser
	 *           Phone# or TextMarks username to authenticate to API with. (null for none).
	 * @param sAuthPass
	 *           TextMarks Password associated with sAuthUser. (null for none).
	 */
	public TextMarksV2APIClient(String sApiKey, String sAuthUser, String sAuthPass)
	{
		this.m_sApiKey = sApiKey;
		this.m_sAuthUser = sAuthUser;
		this.m_sAuthPass = sAuthPass;
	}
	
	public void setApiKey( String sApiKey )
	{
		this.m_sApiKey = sApiKey;
	}
	
	public void setAuthUser( String sAuthUser )
	{
		this.m_sAuthUser = sAuthUser;
	}
	
	public void setAuthPass( String sAuthPass )
	{
		this.m_sAuthPass = sAuthPass;
	}
	
	public String getApiKey()
	{
		return this.m_sApiKey;
	}
	
	public String getAuthUser()
	{
		return this.m_sAuthUser;
	}
	
	public String getAuthPass()
	{
		return this.m_sAuthPass;
	}
	
	/**
	 * Public method to call API.
	 * 
	 * The API Key and auth params are automatically added if present.
	 * 
	 * @param sPackageName
	 *           Package name.
	 * @param sMethodName
	 *           Method name.
	 * @param msoParams
	 *           Params for method.
	 * @return Decoded (from JSON) response.
	 * @throws Exception
	 *            on error.
	 */
	public JSONObject call( String sPackageName, String sMethodName, Map<String, Object> msoParams ) throws TextMarksV2APIClientTransportException, TextMarksV2APIClientResultException
	{
		return this._callJsonApiMethod( sPackageName, sMethodName, msoParams );
	}
	
	/**
	 * Execute HTTP request (post params to API endpoint) and return string response.
	 * 
	 * @param sUrl
	 *           URL to request (method endpoint).
	 * @param msoParams
	 *           Request params.
	 * @return Response (usually JSON string).
	 * @throws TextMarksV2APIClientTransportException
	 *            on error.
	 */
	protected String _rawHttpCall( String sUrl, Map<String, Object> msoParams ) throws TextMarksV2APIClientTransportException
	{
		// Convert param map to encoded form (to post):
		String sPostData = "";
		try
		{
			StringBuffer sbPostData = new StringBuffer();
			for ( Map.Entry<String, Object> entry : msoParams.entrySet() )
			{
				sbPostData.append( "&" ).append( URLEncoder.encode( entry.getKey(), "UTF-8" ) );
				sbPostData.append( "=" ).append( URLEncoder.encode( String.valueOf( entry.getValue() ), "UTF-8" ) );
			}
			sPostData = sbPostData.toString();
		}
		catch ( UnsupportedEncodingException e )
		{}
		
		// Create HTTP URL connection:
		HttpURLConnection urlConnection = null;
		try
		{
			URL url = new URL( sUrl );
			urlConnection = (HttpURLConnection) url.openConnection();
			( urlConnection ).setRequestMethod( "POST" );
			urlConnection.setDoInput( true );
			urlConnection.setDoOutput( true );
			urlConnection.setUseCaches( false );
			urlConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
			urlConnection.setRequestProperty( "Content-Length", "" + sPostData.length() );
		}
		catch ( Exception e )
		{
			throw new TextMarksV2APIClientTransportException( "TextMarksV2APIClient (" + sUrl + ") saw HTTP connection error: " + e.toString(), e );
		}
		
		// Send request:
		try
		{
			DataOutputStream out = new DataOutputStream( urlConnection.getOutputStream() );
			out.writeBytes( sPostData );
			out.close();
		}
		catch ( Exception e )
		{
			throw new TextMarksV2APIClientTransportException( "TextMarksV2APIClient (" + sUrl + ") saw HTTP request error: " + e.toString(), e );
		}
		
		// Get response:
		try
		{
			BufferedReader in = new BufferedReader( new InputStreamReader( urlConnection.getInputStream() ) );
			StringBuffer sbResponse = new StringBuffer();
			String sBuf;
			while ( ( sBuf = in.readLine() ) != null )
			{
				sbResponse.append( sBuf );
			}
			in.close();
			return sbResponse.toString();
		}
		catch ( Exception e )
		{
			throw new TextMarksV2APIClientTransportException( "TextMarksV2APIClient (" + sUrl + ") saw HTTP response error: " + e.toString(), e );
		}
	}
	
	/**
	 * Execute API call and return decoded JSON response.
	 * 
	 * The API Key and auth params are automatically added.
	 * 
	 * @param sPackageName
	 *           Package name.
	 * @param sMethodName
	 *           Method name.
	 * @param msoParams
	 *           Params for method.
	 * @return Decoded (from JSON) response.
	 * @throws Exception
	 *            on error.
	 */
	protected JSONObject _callJsonApiMethod( String sPackageName, String sMethodName, Map<String, Object> msoParams ) throws TextMarksV2APIClientTransportException, TextMarksV2APIClientResultException
	{
		// Prep:
		Map<String, Object> msoParamsFull = new HashMap<String, Object>( msoParams ); // (copy to keep original clean)
		if ( this.m_sApiKey != null )
		{
			msoParamsFull.put( "api_key", this.m_sApiKey );
		}
		if ( this.m_sAuthUser != null )
		{
			msoParamsFull.put( "auth_user", this.m_sAuthUser );
		}
		if ( this.m_sAuthPass != null )
		{
			msoParamsFull.put( "auth_pass", this.m_sAuthPass );
		}
		String sUrl = API_URL_BASE + "/" + sPackageName + "/" + sMethodName + "/";
		
		// Make actual HTTP call:
		String sResp = this._rawHttpCall( sUrl, msoParamsFull );
		
		// Parse JSON response:
		JSONObject joResp = null;
		try
		{
			joResp = new JSONObject( sResp );
		}
		catch ( JSONException e )
		{
			throw new TextMarksV2APIClientTransportException( "TextMarksV2APIClient (" + sUrl + ") got invalid JSON response: " + e.toString(), e );
		}
		
		// Check API response code:
		try
		{
			int iResCode = joResp.getJSONObject( "head" ).getInt( "rescode" );
			String sResMsg = joResp.getJSONObject( "head" ).getString( "resmsg" );
			if ( iResCode != 0 )
			{
				throw new TextMarksV2APIClientResultException( "TextMarksV2APIClient.call(" + sPackageName + "." + sMethodName + ") got API error #" + iResCode + ": " + sResMsg, iResCode );
			}
		}
		catch ( JSONException e )
		{
			throw new TextMarksV2APIClientTransportException( "TextMarksV2APIClient (" + sUrl + ") got bad malformed JSON response: " + e.toString(), e );
		}
		
		return joResp;
	}
	
	// -----------------------------------------------------------------------
	
	protected String m_sApiKey = null;
	protected String m_sAuthUser = null;
	protected String m_sAuthPass = null;
}
