/**
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 * Copyright (c) 2017 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Rights Reserved
 */
package com.chiorichan.http.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import com.chiorichan.AppConfig;
import com.chiorichan.http.HttpRequestWrapper;
import com.chiorichan.lang.HttpError;

public class MultiPartRequestParser
{
	/**
	 * Extracts and returns the content type from a line, or null if the
	 * line was empty.
	 *
	 * @return content type, or null if line was empty.
	 * @exception IOException
	 *                 if the line is malformatted.
	 */
	private static String extractContentType( String line ) throws IOException
	{
		// Convert the line to a lowercase string
		line = line.toLowerCase();

		// Get the content type, if any
		// Note that Opera at least puts extra info after the type, so handle
		// that. For example: Content-Type: text/plain; name="foo"
		// Thanks to Leon Poyyayil, leon.poyyayil@trivadis.com, for noticing this.
		int end = line.indexOf( ";" );
		if ( end == -1 )
			end = line.length();

		return line.substring( 13, end ).trim(); // "content-type:" is 13
	}

	public static boolean isMultipart( HttpRequestWrapper request )
	{
		String requestType = request.getOriginal().headers().getAndConvert( "Content-Type" );
		return requestType.toLowerCase().startsWith( "multipart/form-data" );
	}

	private String boundary;

	// private static String DEFAULT_ENCODING = "ISO-8859-1";
	// private String encoding = DEFAULT_ENCODING;

	private FilePart lastFilePart;

	private RandomReadWriteByteArray buffer = new RandomReadWriteByteArray();

	public MultiPartRequestParser( HttpRequestWrapper request ) throws IOException, HttpError
	{
		String requestType = request.getOriginal().headers().getAndConvert( "Content-Type" );
		int maxUpload = AppConfig.get().getInt( "server.maxFileUploadKb", 5120 );

		if ( request.getContentLength() > maxUpload )
			throw new HttpError( 413, "The uploaded file exceeds the `server.maxFileUploadKb` directive that was specified in the server config." );
		else
		{
			InputStream is = null;// request.getOriginal().getRequestBody(); // XXX FIX THIS!
			boundary = extractBoundary( requestType );

			if ( boundary == null )
				throw new IOException( "Separation boundary was not specified" );

			buffer.consumeInputStream( is );

			String line = null;

			do
			{
				line = buffer.readLine();

				if ( line == null )
					throw new IOException( "Corrupt form data: premature ending" );
			}
			while ( !line.startsWith( boundary ) );
		}
	}

	/**
	 * Extracts and returns the boundary token from a line.
	 *
	 * @return the boundary token.
	 */
	private String extractBoundary( String line )
	{
		int index = line.lastIndexOf( "boundary=" );
		if ( index == -1 )
			return null;
		String boundary = line.substring( index + 9 ); // 9 for "boundary="
		if ( boundary.charAt( 0 ) == '"' )
		{
			// The boundary is enclosed in quotes, strip them
			index = boundary.lastIndexOf( '"' );
			boundary = boundary.substring( 1, index );
		}

		// The real boundary is always preceeded by an extra "--"
		boundary = "--" + boundary;

		return boundary;
	}

	/**
	 * Extracts and returns disposition info from a line, as a <code>String<code>
	 * array with elements: disposition, name, filename.
	 *
	 * @return String[] of elements: disposition, name, filename.
	 * @exception IOException
	 *                 if the line is malformatted.
	 */
	private String[] extractDispositionInfo( String line ) throws IOException
	{
		// Return the line's data as an array: disposition, name, filename
		String[] retval = new String[4];

		// Convert the line to a lowercase string without the ending \r\n
		// Keep the original line for error messages and for variable names.
		String origline = line;
		line = origline.toLowerCase();

		// Get the content disposition, should be "form-data"
		int start = line.indexOf( "content-disposition: " );
		int end = line.indexOf( ";" );
		if ( start == -1 || end == -1 )
			throw new IOException( "Content disposition corrupt: " + origline );
		String disposition = line.substring( start + 21, end ).trim();
		if ( !disposition.equals( "form-data" ) )
			throw new IOException( "Invalid content disposition: " + disposition );

		// Get the field name
		start = line.indexOf( "name=\"", end ); // start at last semicolon
		end = line.indexOf( "\"", start + 7 ); // skip name=\"
		int startOffset = 6;
		if ( start == -1 || end == -1 )
		{
			// Some browsers like lynx don't surround with ""
			// Thanks to Deon van der Merwe, dvdm@truteq.co.za, for noticing
			start = line.indexOf( "name=", end );
			end = line.indexOf( ";", start + 6 );
			if ( start == -1 )
				throw new IOException( "Content disposition corrupt: " + origline );
			else if ( end == -1 )
				end = line.length();
			startOffset = 5; // without quotes we have one fewer char to skip
		}
		String name = origline.substring( start + startOffset, end );

		// Get the filename, if given
		String filename = null;
		String origname = null;
		start = line.indexOf( "filename=\"", end + 2 ); // start after name
		end = line.indexOf( "\"", start + 10 ); // skip filename=\"
		if ( start != -1 && end != -1 )
		{ // note the !=
			filename = origline.substring( start + 10, end );
			origname = filename;
			// The filename may contain a full path. Cut to just the filename.
			int slash = Math.max( filename.lastIndexOf( '/' ), filename.lastIndexOf( '\\' ) );
			if ( slash > -1 )
				filename = filename.substring( slash + 1 ); // past last slash
		}

		// Return a String array: disposition, name, filename
		// empty filename denotes no file posted!
		retval[0] = disposition;
		retval[1] = name;
		retval[2] = filename;
		retval[3] = origname;
		return retval;
	}

	/**
	 * Read the next part arriving in the stream. Will be either a <code>FilePart</code> or a <code>ParamPart</code>, or <code>null</code> to indicate there are no more parts to read. The order of arrival
	 * corresponds to the order of the form elements in the submitted form.
	 *
	 * @return either a <code>FilePart</code>, a <code>ParamPart</code> or <code>null</code> if there are no more parts to read.
	 * @exception IOException
	 *                 if an input or output exception has occurred.
	 *
	 * @see FilePart
	 * @see ParamPart
	 */
	public Part readNextPart() throws IOException
	{
		// Make sure the last file was entirely read from the input
		if ( lastFilePart != null )
		{
			lastFilePart.getInputStream().close();
			lastFilePart = null;
		}

		// Read the headers; they look like this (not all may be present):
		// Content-Disposition: form-data; name="field1"; filename="file1.txt"
		// Content-Type: type/subtype
		// Content-Transfer-Encoding: binary
		Vector<String> headers = new Vector<String>();

		String line = buffer.readLine();

		if ( line == null )
			// No parts left, we're done
			return null;
		else if ( line.length() == 0 )
			// IE4 on Mac sends an empty line at the end; treat that as the end.
			// Thanks to Daniel Lemire and Henri Tourigny for this fix.
			return null;

		// Read the following header lines we hit an empty line
		// A line starting with whitespace is considered a continuation;
		// that requires a little special logic. Thanks to Nic Ferrier for
		// identifying a good fix.
		while ( line != null && line.length() > 0 )
		{
			String nextLine = null;
			boolean getNextLine = true;
			while ( getNextLine )
			{
				nextLine = buffer.readLine();
				if ( nextLine != null && ( nextLine.startsWith( " " ) || nextLine.startsWith( "\t" ) ) )
					line = line + nextLine;
				else
					getNextLine = false;
			}
			// Add the line to the header list
			headers.addElement( line );
			line = nextLine;
		}

		// If we got a null above, it's the end
		if ( line == null )
			return null;

		String name = null;
		String filename = null;
		String origname = null;
		String contentType = "text/plain"; // rfc1867 says this is the default

		Enumeration<String> enu = headers.elements();
		while ( enu.hasMoreElements() )
		{
			String headerline = enu.nextElement();
			if ( headerline.toLowerCase().startsWith( "content-disposition:" ) )
			{
				// Parse the content-disposition line
				String[] dispInfo = extractDispositionInfo( headerline );
				// String disposition = dispInfo[0]; // not currently used
				name = dispInfo[1];
				filename = dispInfo[2];
				origname = dispInfo[3];
			}
			else if ( headerline.toLowerCase().startsWith( "content-type:" ) )
			{
				// Get the content type, or null if none specified
				String type = extractContentType( headerline );
				if ( type != null )
					contentType = type;
			}
		}

		byte[] data = buffer.readUntil( boundary );

		// Now, finally, we read the content (end after reading the boundary)
		if ( filename == null )
			// This is a parameter, add it to the vector of values
			// The encoding is needed to help parse the value
			return new ParamPart( name, data );
		else
		{
			// This is a file
			if ( filename.equals( "" ) )
				filename = null; // empty filename, probably an "empty" file param
			lastFilePart = new FilePart( name, data, contentType, filename, origname );
			return lastFilePart;
		}
	}
}
