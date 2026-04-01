package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.model.util.VersionLabel;
import org.eclipse.rdf4j.rio.RioSetting;

public class RDFVersionSettings {

	public static final RioSetting<VersionLabel> PREFERRED_OUTPUT_RDF_VERSION = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.rdf.preferred.output.version", "Preferred output RDF version", null);

	public static final RioSetting<VersionLabel> INPUT_RDF_VERSION = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.rdf.input.version", "Input RDF version", null);
}