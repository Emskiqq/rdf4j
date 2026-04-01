package org.eclipse.rdf4j.model.util;

public enum VersionLabel {
	RDF_1_2_FULL("1.2"),
	RDF_1_2_BASIC("1.2-basic"),
	RDF_1_1("1.1"),
	DEFAULT("1.2");

	private final String value;

	private VersionLabel(String value) {
		this.value = value;
	}

	public static VersionLabel fromLabel(String value) {
		for (VersionLabel v : values()) {
			if (v.value.equals(value)) {
				return v;
			}
		}
		throw new IllegalArgumentException("Unknown RDF version: " + value);
	}

	public String getValue() {
		return this.value;
	}

	public String toString() {
		return this.value;
	}
}