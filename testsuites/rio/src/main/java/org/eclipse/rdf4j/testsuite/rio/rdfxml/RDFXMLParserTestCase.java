/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.rio.rdfxml;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.ntriples.NTriplesParser;
import org.eclipse.rdf4j.rio.rdfxml.RDFXMLParser;
import org.eclipse.rdf4j.testsuite.rio.AbstractParserTestSuite;

/**
 * JUnit test for the RDF/XML parser that uses the tests that are available
 * online from the W3C RDF test suite.
 */
public abstract class RDFXMLParserTestCase extends AbstractParserTestSuite {

    public static String RDF_XML_TESTS_W3C_BASE_URL =
            "https://w3c.github.io/rdf-tests/rdf/rdf11/rdf-xml/";

    protected static String TEST_W3C_FILE_BASE_PATH =
            "/testcases/rdfxml/rdf11/";

    public RDFXMLParserTestCase() {
        this(TEST_W3C_FILE_BASE_PATH, RDF_XML_TESTS_W3C_BASE_URL);
    }

    public RDFXMLParserTestCase(String testFileBasePath, String testBaseURL) {
        super(testFileBasePath, testBaseURL, RDFFormat.RDFXML, "XML");
    }

    @Override
    protected RDFParser createRDFParser() {
        RDFXMLParser parser = new RDFXMLParser();
        parser.setParseStandAloneDocuments(true);
        return parser;
    }

    @Override
    protected RDFParser createRDFBaseParser() {
        return new NTriplesParser();
    }

    // backwards compatibility helpers

    protected RDFParser createRDFXMLParser() {
        return createRDFParser();
    }

    protected RDFParser createNTriplesParser() {
        return createRDFBaseParser();
    }

    @Override
    protected String computeSubManifestFilePath(String subManifestFile) {
        String subManifestFilePath = "";
        if (format == RDFFormat.RDFXML && subManifestFile.contains("rdf11")) {
            final String relativePath = subManifestFile.substring(RDF_XML_TESTS_W3C_BASE_URL.length());
            subManifestFilePath = testFileBasePath + relativePath;
        } else if (subManifestFile.startsWith(testBaseURL)) {
            final String relativePath = subManifestFile.substring(testBaseURL.length());
            subManifestFilePath = testFileBasePath + relativePath;
        }
        return subManifestFilePath;
    }

}