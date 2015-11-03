/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2015 Chiori Greene a.k.a. Chiori-chan <me@chiorichan.com>
 * All Right Reserved.
 */
package com.chiorichan.datastore.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang3.Validate;

/**
 * 
 */
public class SQLResultSet implements ResultSet
{
	protected final ResultSet result;
	
	SQLResultSet( ResultSet result )
	{
		Validate.notNull( result );
		this.result = result;
	}
	
	@Override
	public final boolean absolute( int row ) throws SQLException
	{
		return result.absolute( row );
	}
	
	@Override
	public final void afterLast() throws SQLException
	{
		result.afterLast();
	}
	
	@Override
	public final void beforeFirst() throws SQLException
	{
		result.beforeFirst();
	}
	
	@Override
	public final void cancelRowUpdates() throws SQLException
	{
		result.cancelRowUpdates();
	}
	
	@Override
	public final void clearWarnings() throws SQLException
	{
		result.clearWarnings();
	}
	
	@Override
	public final void close() throws SQLException
	{
		result.close();
	}
	
	@Override
	public final void deleteRow() throws SQLException
	{
		result.deleteRow();
	}
	
	@Override
	public final int findColumn( String columnLabel ) throws SQLException
	{
		return result.findColumn( columnLabel );
	}
	
	@Override
	public final boolean first() throws SQLException
	{
		return result.first();
	}
	
	@Override
	public final Array getArray( int columnIndex ) throws SQLException
	{
		return result.getArray( columnIndex );
	}
	
	@Override
	public final Array getArray( String columnLabel ) throws SQLException
	{
		return result.getArray( columnLabel );
	}
	
	@Override
	public final InputStream getAsciiStream( int columnIndex ) throws SQLException
	{
		return result.getAsciiStream( columnIndex );
	}
	
	@Override
	public final InputStream getAsciiStream( String columnLabel ) throws SQLException
	{
		return result.getAsciiStream( columnLabel );
	}
	
	@Override
	public final BigDecimal getBigDecimal( int columnIndex ) throws SQLException
	{
		return result.getBigDecimal( columnIndex );
	}
	
	@Override
	@Deprecated
	public final BigDecimal getBigDecimal( int columnIndex, int scale ) throws SQLException
	{
		return result.getBigDecimal( columnIndex, scale );
	}
	
	@Override
	public final BigDecimal getBigDecimal( String columnLabel ) throws SQLException
	{
		return result.getBigDecimal( columnLabel );
	}
	
	@Override
	@Deprecated
	public final BigDecimal getBigDecimal( String columnLabel, int scale ) throws SQLException
	{
		return result.getBigDecimal( columnLabel, scale );
	}
	
	@Override
	public final InputStream getBinaryStream( int columnIndex ) throws SQLException
	{
		return result.getBinaryStream( columnIndex );
	}
	
	@Override
	public final InputStream getBinaryStream( String columnLabel ) throws SQLException
	{
		return result.getBinaryStream( columnLabel );
	}
	
	@Override
	public final Blob getBlob( int columnIndex ) throws SQLException
	{
		return result.getBlob( columnIndex );
	}
	
	@Override
	public final Blob getBlob( String columnLabel ) throws SQLException
	{
		return result.getBlob( columnLabel );
	}
	
	@Override
	public final boolean getBoolean( int columnIndex ) throws SQLException
	{
		return result.getBoolean( columnIndex );
	}
	
	@Override
	public final boolean getBoolean( String columnLabel ) throws SQLException
	{
		return result.getBoolean( columnLabel );
	}
	
	@Override
	public final byte getByte( int columnIndex ) throws SQLException
	{
		return result.getByte( columnIndex );
	}
	
	@Override
	public final byte getByte( String columnLabel ) throws SQLException
	{
		return result.getByte( columnLabel );
	}
	
	@Override
	public final byte[] getBytes( int columnIndex ) throws SQLException
	{
		return result.getBytes( columnIndex );
	}
	
	@Override
	public final byte[] getBytes( String columnLabel ) throws SQLException
	{
		return result.getBytes( columnLabel );
	}
	
	@Override
	public final Reader getCharacterStream( int columnIndex ) throws SQLException
	{
		return result.getCharacterStream( columnIndex );
	}
	
	@Override
	public final Reader getCharacterStream( String columnLabel ) throws SQLException
	{
		return result.getCharacterStream( columnLabel );
	}
	
	@Override
	public final Clob getClob( int columnIndex ) throws SQLException
	{
		return result.getClob( columnIndex );
	}
	
	@Override
	public final Clob getClob( String columnLabel ) throws SQLException
	{
		return result.getClob( columnLabel );
	}
	
	@Override
	public final int getConcurrency() throws SQLException
	{
		return result.getConcurrency();
	}
	
	@Override
	public final String getCursorName() throws SQLException
	{
		return result.getCursorName();
	}
	
	@Override
	public final Date getDate( int columnIndex ) throws SQLException
	{
		return result.getDate( columnIndex );
	}
	
	@Override
	public final Date getDate( int columnIndex, Calendar cal ) throws SQLException
	{
		return result.getDate( columnIndex, cal );
	}
	
	@Override
	public final Date getDate( String columnLabel ) throws SQLException
	{
		return result.getDate( columnLabel );
	}
	
	@Override
	public final Date getDate( String columnLabel, Calendar cal ) throws SQLException
	{
		return result.getDate( columnLabel, cal );
	}
	
	@Override
	public final double getDouble( int columnIndex ) throws SQLException
	{
		return result.getDouble( columnIndex );
	}
	
	@Override
	public final double getDouble( String columnLabel ) throws SQLException
	{
		return result.getDouble( columnLabel );
	}
	
	@Override
	public final int getFetchDirection() throws SQLException
	{
		return result.getFetchDirection();
	}
	
	@Override
	public final int getFetchSize() throws SQLException
	{
		return result.getFetchSize();
	}
	
	@Override
	public final float getFloat( int columnIndex ) throws SQLException
	{
		return result.getFloat( columnIndex );
	}
	
	@Override
	public final float getFloat( String columnLabel ) throws SQLException
	{
		return result.getFloat( columnLabel );
	}
	
	@Override
	public final int getHoldability() throws SQLException
	{
		return result.getHoldability();
	}
	
	@Override
	public final int getInt( int columnIndex ) throws SQLException
	{
		return result.getInt( columnIndex );
	}
	
	@Override
	public final int getInt( String columnLabel ) throws SQLException
	{
		return result.getInt( columnLabel );
	}
	
	@Override
	public final long getLong( int columnIndex ) throws SQLException
	{
		return result.getLong( columnIndex );
	}
	
	@Override
	public final long getLong( String columnLabel ) throws SQLException
	{
		return result.getLong( columnLabel );
	}
	
	@Override
	public final ResultSetMetaData getMetaData() throws SQLException
	{
		return result.getMetaData();
	}
	
	@Override
	public final Reader getNCharacterStream( int columnIndex ) throws SQLException
	{
		return result.getNCharacterStream( columnIndex );
	}
	
	@Override
	public final Reader getNCharacterStream( String columnLabel ) throws SQLException
	{
		return result.getNCharacterStream( columnLabel );
	}
	
	@Override
	public final NClob getNClob( int columnIndex ) throws SQLException
	{
		return result.getNClob( columnIndex );
	}
	
	@Override
	public final NClob getNClob( String columnLabel ) throws SQLException
	{
		return result.getNClob( columnLabel );
	}
	
	@Override
	public final String getNString( int columnIndex ) throws SQLException
	{
		return result.getNString( columnIndex );
	}
	
	@Override
	public final String getNString( String columnLabel ) throws SQLException
	{
		return result.getNString( columnLabel );
	}
	
	@Override
	public final Object getObject( int columnIndex ) throws SQLException
	{
		return result.getObject( columnIndex );
	}
	
	@Override
	public final <T> T getObject( int columnIndex, Class<T> type ) throws SQLException
	{
		return result.getObject( columnIndex, type );
	}
	
	@Override
	public final Object getObject( int columnIndex, Map<String, Class<?>> map ) throws SQLException
	{
		return result.getObject( columnIndex, map );
	}
	
	@Override
	public final Object getObject( String columnLabel ) throws SQLException
	{
		return result.getObject( columnLabel );
	}
	
	@Override
	public final <T> T getObject( String columnLabel, Class<T> type ) throws SQLException
	{
		return result.getObject( columnLabel, type );
	}
	
	@Override
	public final Object getObject( String columnLabel, Map<String, Class<?>> map ) throws SQLException
	{
		return result.getObject( columnLabel, map );
	}
	
	@Override
	public final Ref getRef( int columnIndex ) throws SQLException
	{
		return result.getRef( columnIndex );
	}
	
	@Override
	public final Ref getRef( String columnLabel ) throws SQLException
	{
		return result.getRef( columnLabel );
	}
	
	@Override
	public final int getRow() throws SQLException
	{
		return result.getRow();
	}
	
	@Override
	public final RowId getRowId( int columnIndex ) throws SQLException
	{
		return result.getRowId( columnIndex );
	}
	
	@Override
	public final RowId getRowId( String columnLabel ) throws SQLException
	{
		
		return result.getRowId( columnLabel );
	}
	
	@Override
	public final short getShort( int columnIndex ) throws SQLException
	{
		return result.getShort( columnIndex );
	}
	
	@Override
	public final short getShort( String columnLabel ) throws SQLException
	{
		return result.getShort( columnLabel );
	}
	
	@Override
	public final SQLXML getSQLXML( int columnIndex ) throws SQLException
	{
		return result.getSQLXML( columnIndex );
	}
	
	@Override
	public final SQLXML getSQLXML( String columnLabel ) throws SQLException
	{
		return result.getSQLXML( columnLabel );
	}
	
	@Override
	public final Statement getStatement() throws SQLException
	{
		return result.getStatement();
	}
	
	@Override
	public final String getString( int columnIndex ) throws SQLException
	{
		return result.getString( columnIndex );
	}
	
	@Override
	public final String getString( String columnLabel ) throws SQLException
	{
		return result.getString( columnLabel );
	}
	
	@Override
	public final Time getTime( int columnIndex ) throws SQLException
	{
		return result.getTime( columnIndex );
	}
	
	@Override
	public final Time getTime( int columnIndex, Calendar cal ) throws SQLException
	{
		return result.getTime( columnIndex, cal );
	}
	
	@Override
	public final Time getTime( String columnLabel ) throws SQLException
	{
		return result.getTime( columnLabel );
	}
	
	@Override
	public final Time getTime( String columnLabel, Calendar cal ) throws SQLException
	{
		return result.getTime( columnLabel, cal );
	}
	
	@Override
	public final Timestamp getTimestamp( int columnIndex ) throws SQLException
	{
		return result.getTimestamp( columnIndex );
	}
	
	@Override
	public final Timestamp getTimestamp( int columnIndex, Calendar cal ) throws SQLException
	{
		return result.getTimestamp( columnIndex, cal );
	}
	
	@Override
	public final Timestamp getTimestamp( String columnLabel ) throws SQLException
	{
		return result.getTimestamp( columnLabel );
	}
	
	@Override
	public final Timestamp getTimestamp( String columnLabel, Calendar cal ) throws SQLException
	{
		return result.getTimestamp( columnLabel );
	}
	
	@Override
	public final int getType() throws SQLException
	{
		return result.getType();
	}
	
	@Override
	@Deprecated
	public final InputStream getUnicodeStream( int columnIndex ) throws SQLException
	{
		return result.getUnicodeStream( columnIndex );
	}
	
	@Override
	@Deprecated
	public final InputStream getUnicodeStream( String columnLabel ) throws SQLException
	{
		return result.getUnicodeStream( columnLabel );
	}
	
	@Override
	public final URL getURL( int columnIndex ) throws SQLException
	{
		return result.getURL( columnIndex );
	}
	
	@Override
	public final URL getURL( String columnLabel ) throws SQLException
	{
		return result.getURL( columnLabel );
	}
	
	@Override
	public final SQLWarning getWarnings() throws SQLException
	{
		return result.getWarnings();
	}
	
	@Override
	public final void insertRow() throws SQLException
	{
		result.insertRow();
	}
	
	@Override
	public final boolean isAfterLast() throws SQLException
	{
		return result.isAfterLast();
	}
	
	@Override
	public final boolean isBeforeFirst() throws SQLException
	{
		return result.isBeforeFirst();
	}
	
	@Override
	public final boolean isClosed() throws SQLException
	{
		return result.isClosed();
	}
	
	@Override
	public final boolean isFirst() throws SQLException
	{
		return result.isFirst();
	}
	
	@Override
	public final boolean isLast() throws SQLException
	{
		return result.isLast();
	}
	
	@Override
	public final boolean isWrapperFor( Class<?> iface ) throws SQLException
	{
		return result.isWrapperFor( iface );
	}
	
	@Override
	public final boolean last() throws SQLException
	{
		return result.last();
	}
	
	@Override
	public final void moveToCurrentRow() throws SQLException
	{
		result.moveToCurrentRow();
	}
	
	@Override
	public final void moveToInsertRow() throws SQLException
	{
		result.moveToInsertRow();
	}
	
	@Override
	public final boolean next() throws SQLException
	{
		return result.next();
	}
	
	@Override
	public final boolean previous() throws SQLException
	{
		return result.previous();
	}
	
	@Override
	public final void refreshRow() throws SQLException
	{
		result.refreshRow();
	}
	
	@Override
	public final boolean relative( int rows ) throws SQLException
	{
		return result.relative( rows );
	}
	
	@Override
	public final boolean rowDeleted() throws SQLException
	{
		return result.rowDeleted();
	}
	
	@Override
	public final boolean rowInserted() throws SQLException
	{
		return result.rowInserted();
	}
	
	@Override
	public final boolean rowUpdated() throws SQLException
	{
		return result.rowUpdated();
	}
	
	@Override
	public final void setFetchDirection( int direction ) throws SQLException
	{
		result.setFetchDirection( direction );
	}
	
	@Override
	public final void setFetchSize( int rows ) throws SQLException
	{
		result.setFetchSize( rows );
	}
	
	@Override
	public final <T> T unwrap( Class<T> iface ) throws SQLException
	{
		return result.unwrap( iface );
	}
	
	@Override
	public final void updateArray( int columnIndex, Array x ) throws SQLException
	{
		result.updateArray( columnIndex, x );
	}
	
	@Override
	public final void updateArray( String columnLabel, Array x ) throws SQLException
	{
		result.updateArray( columnLabel, x );
	}
	
	@Override
	public final void updateAsciiStream( int columnIndex, InputStream x ) throws SQLException
	{
		result.updateAsciiStream( columnIndex, x );
	}
	
	@Override
	public final void updateAsciiStream( int columnIndex, InputStream x, int length ) throws SQLException
	{
		result.updateAsciiStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateAsciiStream( int columnIndex, InputStream x, long length ) throws SQLException
	{
		result.updateAsciiStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateAsciiStream( String columnLabel, InputStream x ) throws SQLException
	{
		result.updateAsciiStream( columnLabel, x );
	}
	
	@Override
	public final void updateAsciiStream( String columnLabel, InputStream x, int length ) throws SQLException
	{
		result.updateAsciiStream( columnLabel, x, length );
	}
	
	@Override
	public final void updateAsciiStream( String columnLabel, InputStream x, long length ) throws SQLException
	{
		result.updateAsciiStream( columnLabel, x, length );
	}
	
	@Override
	public final void updateBigDecimal( int columnIndex, BigDecimal x ) throws SQLException
	{
		result.updateBigDecimal( columnIndex, x );
	}
	
	@Override
	public final void updateBigDecimal( String columnLabel, BigDecimal x ) throws SQLException
	{
		result.updateBigDecimal( columnLabel, x );
	}
	
	@Override
	public final void updateBinaryStream( int columnIndex, InputStream x ) throws SQLException
	{
		result.updateBinaryStream( columnIndex, x );
	}
	
	@Override
	public final void updateBinaryStream( int columnIndex, InputStream x, int length ) throws SQLException
	{
		result.updateBinaryStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateBinaryStream( int columnIndex, InputStream x, long length ) throws SQLException
	{
		result.updateBinaryStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateBinaryStream( String columnLabel, InputStream x ) throws SQLException
	{
		result.updateBinaryStream( columnLabel, x );
	}
	
	@Override
	public final void updateBinaryStream( String columnLabel, InputStream x, int length ) throws SQLException
	{
		result.updateBinaryStream( columnLabel, x, length );
	}
	
	@Override
	public final void updateBinaryStream( String columnLabel, InputStream x, long length ) throws SQLException
	{
		result.updateBinaryStream( columnLabel, x, length );
	}
	
	@Override
	public final void updateBlob( int columnIndex, Blob x ) throws SQLException
	{
		result.updateBlob( columnIndex, x );
	}
	
	@Override
	public final void updateBlob( int columnIndex, InputStream inputStream ) throws SQLException
	{
		result.updateBlob( columnIndex, inputStream );
	}
	
	@Override
	public final void updateBlob( int columnIndex, InputStream inputStream, long length ) throws SQLException
	{
		result.updateBlob( columnIndex, inputStream, length );
	}
	
	@Override
	public final void updateBlob( String columnLabel, Blob x ) throws SQLException
	{
		result.updateBlob( columnLabel, x );
	}
	
	@Override
	public final void updateBlob( String columnLabel, InputStream inputStream ) throws SQLException
	{
		result.updateBlob( columnLabel, inputStream );
	}
	
	@Override
	public final void updateBlob( String columnLabel, InputStream inputStream, long length ) throws SQLException
	{
		result.updateBlob( columnLabel, inputStream, length );
	}
	
	@Override
	public final void updateBoolean( int columnIndex, boolean x ) throws SQLException
	{
		result.updateBoolean( columnIndex, x );
	}
	
	@Override
	public final void updateBoolean( String columnLabel, boolean x ) throws SQLException
	{
		result.updateBoolean( columnLabel, x );
	}
	
	@Override
	public final void updateByte( int columnIndex, byte x ) throws SQLException
	{
		result.updateByte( columnIndex, x );
	}
	
	@Override
	public final void updateByte( String columnLabel, byte x ) throws SQLException
	{
		result.updateByte( columnLabel, x );
	}
	
	@Override
	public final void updateBytes( int columnIndex, byte[] x ) throws SQLException
	{
		result.updateBytes( columnIndex, x );
	}
	
	@Override
	public final void updateBytes( String columnLabel, byte[] x ) throws SQLException
	{
		result.updateBytes( columnLabel, x );
	}
	
	@Override
	public final void updateCharacterStream( int columnIndex, Reader x ) throws SQLException
	{
		result.updateCharacterStream( columnIndex, x );
	}
	
	@Override
	public final void updateCharacterStream( int columnIndex, Reader x, int length ) throws SQLException
	{
		result.updateCharacterStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateCharacterStream( int columnIndex, Reader x, long length ) throws SQLException
	{
		result.updateCharacterStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateCharacterStream( String columnLabel, Reader reader ) throws SQLException
	{
		result.updateCharacterStream( columnLabel, reader );
	}
	
	@Override
	public final void updateCharacterStream( String columnLabel, Reader reader, int length ) throws SQLException
	{
		result.updateCharacterStream( columnLabel, reader, length );
	}
	
	@Override
	public final void updateCharacterStream( String columnLabel, Reader reader, long length ) throws SQLException
	{
		result.updateCharacterStream( columnLabel, reader, length );
	}
	
	@Override
	public final void updateClob( int columnIndex, Clob x ) throws SQLException
	{
		result.updateClob( columnIndex, x );
	}
	
	@Override
	public final void updateClob( int columnIndex, Reader reader ) throws SQLException
	{
		result.updateClob( columnIndex, reader );
	}
	
	@Override
	public final void updateClob( int columnIndex, Reader reader, long length ) throws SQLException
	{
		result.updateClob( columnIndex, reader, length );
	}
	
	@Override
	public final void updateClob( String columnLabel, Clob x ) throws SQLException
	{
		result.updateClob( columnLabel, x );
	}
	
	@Override
	public final void updateClob( String columnLabel, Reader reader ) throws SQLException
	{
		result.updateClob( columnLabel, reader );
	}
	
	@Override
	public final void updateClob( String columnLabel, Reader reader, long length ) throws SQLException
	{
		result.updateClob( columnLabel, reader, length );
	}
	
	@Override
	public final void updateDate( int columnIndex, Date x ) throws SQLException
	{
		result.updateDate( columnIndex, x );
	}
	
	@Override
	public final void updateDate( String columnLabel, Date x ) throws SQLException
	{
		result.updateDate( columnLabel, x );
	}
	
	@Override
	public final void updateDouble( int columnIndex, double x ) throws SQLException
	{
		result.updateDouble( columnIndex, x );
	}
	
	@Override
	public final void updateDouble( String columnLabel, double x ) throws SQLException
	{
		result.updateDouble( columnLabel, x );
	}
	
	@Override
	public final void updateFloat( int columnIndex, float x ) throws SQLException
	{
		result.updateFloat( columnIndex, x );
	}
	
	@Override
	public final void updateFloat( String columnLabel, float x ) throws SQLException
	{
		result.updateFloat( columnLabel, x );
	}
	
	@Override
	public final void updateInt( int columnIndex, int x ) throws SQLException
	{
		result.updateInt( columnIndex, x );
	}
	
	@Override
	public final void updateInt( String columnLabel, int x ) throws SQLException
	{
		result.updateInt( columnLabel, x );
	}
	
	@Override
	public final void updateLong( int columnIndex, long x ) throws SQLException
	{
		result.updateLong( columnIndex, x );
	}
	
	@Override
	public final void updateLong( String columnLabel, long x ) throws SQLException
	{
		result.updateLong( columnLabel, x );
	}
	
	@Override
	public final void updateNCharacterStream( int columnIndex, Reader x ) throws SQLException
	{
		result.updateNCharacterStream( columnIndex, x );
	}
	
	@Override
	public final void updateNCharacterStream( int columnIndex, Reader x, long length ) throws SQLException
	{
		result.updateNCharacterStream( columnIndex, x, length );
	}
	
	@Override
	public final void updateNCharacterStream( String columnLabel, Reader reader ) throws SQLException
	{
		result.updateNCharacterStream( columnLabel, reader );
	}
	
	@Override
	public final void updateNCharacterStream( String columnLabel, Reader reader, long length ) throws SQLException
	{
		result.updateNCharacterStream( columnLabel, reader, length );
	}
	
	@Override
	public final void updateNClob( int columnIndex, NClob nClob ) throws SQLException
	{
		result.updateNClob( columnIndex, nClob );
	}
	
	@Override
	public final void updateNClob( int columnIndex, Reader reader ) throws SQLException
	{
		result.updateNClob( columnIndex, reader );
	}
	
	@Override
	public final void updateNClob( int columnIndex, Reader reader, long length ) throws SQLException
	{
		result.updateNClob( columnIndex, reader, length );
	}
	
	@Override
	public final void updateNClob( String columnLabel, NClob nClob ) throws SQLException
	{
		result.updateNClob( columnLabel, nClob );
	}
	
	@Override
	public final void updateNClob( String columnLabel, Reader reader ) throws SQLException
	{
		result.updateNClob( columnLabel, reader );
	}
	
	@Override
	public final void updateNClob( String columnLabel, Reader reader, long length ) throws SQLException
	{
		result.updateNClob( columnLabel, reader, length );
	}
	
	@Override
	public final void updateNString( int columnIndex, String nString ) throws SQLException
	{
		result.updateNString( columnIndex, nString );
	}
	
	@Override
	public final void updateNString( String columnLabel, String nString ) throws SQLException
	{
		result.updateNString( columnLabel, nString );
	}
	
	@Override
	public final void updateNull( int columnIndex ) throws SQLException
	{
		result.updateNull( columnIndex );
	}
	
	@Override
	public final void updateNull( String columnLabel ) throws SQLException
	{
		result.updateNull( columnLabel );
	}
	
	@Override
	public final void updateObject( int columnIndex, Object x ) throws SQLException
	{
		result.updateObject( columnIndex, x );
	}
	
	@Override
	public final void updateObject( int columnIndex, Object x, int scaleOrLength ) throws SQLException
	{
		result.updateObject( columnIndex, x, scaleOrLength );
	}
	
	@Override
	public final void updateObject( String columnLabel, Object x ) throws SQLException
	{
		result.updateObject( columnLabel, x );
	}
	
	@Override
	public final void updateObject( String columnLabel, Object x, int scaleOrLength ) throws SQLException
	{
		result.updateObject( columnLabel, x, scaleOrLength );
	}
	
	@Override
	public final void updateRef( int columnIndex, Ref x ) throws SQLException
	{
		result.updateRef( columnIndex, x );
	}
	
	@Override
	public final void updateRef( String columnLabel, Ref x ) throws SQLException
	{
		result.updateRef( columnLabel, x );
	}
	
	@Override
	public final void updateRow() throws SQLException
	{
		result.updateRow();
	}
	
	@Override
	public final void updateRowId( int columnIndex, RowId x ) throws SQLException
	{
		result.updateRowId( columnIndex, x );
	}
	
	@Override
	public final void updateRowId( String columnLabel, RowId x ) throws SQLException
	{
		result.updateRowId( columnLabel, x );
	}
	
	@Override
	public final void updateShort( int columnIndex, short x ) throws SQLException
	{
		result.updateShort( columnIndex, x );
	}
	
	@Override
	public final void updateShort( String columnLabel, short x ) throws SQLException
	{
		result.updateShort( columnLabel, x );
	}
	
	@Override
	public final void updateSQLXML( int columnIndex, SQLXML xmlObject ) throws SQLException
	{
		result.updateSQLXML( columnIndex, xmlObject );
	}
	
	@Override
	public final void updateSQLXML( String columnLabel, SQLXML xmlObject ) throws SQLException
	{
		result.updateSQLXML( columnLabel, xmlObject );
	}
	
	@Override
	public final void updateString( int columnIndex, String x ) throws SQLException
	{
		result.updateString( columnIndex, x );
	}
	
	@Override
	public final void updateString( String columnLabel, String x ) throws SQLException
	{
		result.updateString( columnLabel, x );
	}
	
	@Override
	public final void updateTime( int columnIndex, Time x ) throws SQLException
	{
		result.updateTime( columnIndex, x );
	}
	
	@Override
	public final void updateTime( String columnLabel, Time x ) throws SQLException
	{
		result.updateTime( columnLabel, x );
	}
	
	@Override
	public final void updateTimestamp( int columnIndex, Timestamp x ) throws SQLException
	{
		result.updateTimestamp( columnIndex, x );
	}
	
	@Override
	public final void updateTimestamp( String columnLabel, Timestamp x ) throws SQLException
	{
		result.updateTimestamp( columnLabel, x );
	}
	
	@Override
	public final boolean wasNull() throws SQLException
	{
		return result.wasNull();
	}
	
}
