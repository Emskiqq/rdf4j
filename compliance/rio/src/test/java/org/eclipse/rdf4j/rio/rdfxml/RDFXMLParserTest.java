/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import org.eclipse.rdf4j.testsuite.rio.rdfxml.RDFXMLParserTestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;

public class RDFXMLParserTest {

    public static Test suite() throws Exception {
        final TestSuite suite = new TestSuite();
        //suite.addTest(RDFXML11ParserTest.suite());
        suite.addTest(RDFXML12ParserTest.suite());
        return suite;
    }

    static class RDFXML11ParserTest extends RDFXMLParserTestCase {

        public static Test suite() throws Exception {
            return new RDFXML11ParserTest().createTestSuite();
        }
    }

    static class RDFXML12ParserTest extends RDFXMLParserTestCase {

        protected static final String TESTS_W3C_BASE_URL =
                "https://w3c.github.io/rdf-tests/rdf/rdf12/rdf-xml/";

        protected static final String TEST_W3C_FILE_BASE_PATH_RDF12 =
                "/testcases/rdfxml/rdf12/";

        private RDFXML12ParserTest() {
            super(TEST_W3C_FILE_BASE_PATH_RDF12, TESTS_W3C_BASE_URL);
        }

        public static Test suite() throws Exception {
            return new RDFXML12ParserTest().createTestSuite();
        }
    }
}