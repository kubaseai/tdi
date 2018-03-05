/*
 * Copyright (c) 2010-2011 Jakub Jozwicki. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * By using this file and API you are obligated to use GPL licence
 * for your code.
 */

package tdi.tibcovery.endpoints;

public class FileEndpoint extends Endpoint {
	
	private String pollInterval = null;
	private String createEvent = null;
	private String modifyEvent = null;
	private String deleteEvent = null;
	private String mode = null;
	private String encoding = null;
	private String fileName = null;
	
	public FileEndpoint() {}

	@Override
	public Endpoint deepClone() {
		FileEndpoint file = new FileEndpoint();
		file.pollInterval = pollInterval;
		file.createEvent = createEvent;
		file.modifyEvent = modifyEvent;
		file.deleteEvent = deleteEvent;
		file.mode = mode;
		file.encoding = encoding;
		file.fileName = fileName;
		file.destinations.addAll(destinations);
		return file;
	}

	public String getPollInterval() {
		return pollInterval;
	}

	public void setPollInterval(String pollInterval) {
		this.pollInterval = pollInterval;
	}

	public String getCreateEvent() {
		return createEvent;
	}

	public void setCreateEvent(String createEvent) {
		this.createEvent = createEvent;
	}

	public String getModifyEvent() {
		return modifyEvent;
	}

	public void setModifyEvent(String modifyEvent) {
		this.modifyEvent = modifyEvent;
	}

	public String getDeleteEvent() {
		return deleteEvent;
	}

	public void setDeleteEvent(String deleteEvent) {
		this.deleteEvent = deleteEvent;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public String getGroupingKey() {
		return fileName!=null ? fileName : "<empty-file-mask>";
	}

	@Override
	public String description() {
		StringBuilder builder = new StringBuilder();
		builder.append("FileEndpoint [fileName=");
		builder.append(fileName);
		builder.append(",\r\ncreateEvent=");
		builder.append(createEvent);
		builder.append(", modifyEvent=");
		builder.append(modifyEvent);
		builder.append(", deleteEvent=");
		builder.append(deleteEvent);
		builder.append(",\r\nmode=");
		builder.append(mode);
		builder.append(", encoding=");
		builder.append(encoding);		
		builder.append("]");
		return builder.toString();
	}	
}
