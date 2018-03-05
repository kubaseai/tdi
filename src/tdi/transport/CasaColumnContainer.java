/*
 * Copyright (c) 2010-2012 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package tdi.transport;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnParent;

public class CasaColumnContainer {
	
	private ColumnParent parent = null;
	private LinkedList<Column> columns = new LinkedList<Column>();
	private long ts = 0;
	private String rowId = System.currentTimeMillis()+"";
		
	public CasaColumnContainer(String name, long ts) {
		parent = new ColumnParent(name);
		this.ts = ts;
	}
	
	private Column col(String name, Object val, long ts) {
		boolean isEmpty = val==null || (val instanceof CharSequence
			&& ((CharSequence)val).length()==0);
		if (!isEmpty) {				
			Column col = new Column();
			col.setName(name.getBytes());
			col.setValue(val.toString().getBytes());
			col.setTimestamp(ts);
			return col;
		}
		return null;
	}

	public void addIfNotEmpty(String name, Object val) {
		Column col = col(name, val, ts);
		if (col!=null)
			columns.add(col);		
	}
	
	public LinkedList<Column> getColumns() {
		return columns;
	}
	public ColumnParent getColumnParent() {
		return parent;
	}
	public ByteBuffer getRowId() {
		return ByteBuffer.wrap(rowId.getBytes());
	}
	public void setRowId(String s) {
		rowId = s;		
	}
}
