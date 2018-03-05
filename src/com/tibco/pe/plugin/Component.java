package com.tibco.pe.plugin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import tdi.core.CompileTimeStub;

@CompileTimeStub
@SuppressWarnings("serial")
public class Component implements Serializable {

	public Component() {
	}

	public final void setName(String name) {
		if (name == null)
			this.name = null;
		else
			this.name = name.intern();
	}

	public final String getName() {
		return name;
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		name = ((String) stream.readObject()).intern();
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeObject(((Object) (name)));
	}

	private String name;
}
