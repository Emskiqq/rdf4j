/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.rio;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

import static org.junit.Assert.fail;

public class PositiveParserTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final String inputURL;

	private String outputURL;

	private final String baseURL;

	private final RDFParser targetParser;

	private final RDFParser ntriplesParser;

	protected IRI testUri;

	private static final Logger logger = LoggerFactory.getLogger(PositiveParserTest.class);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public PositiveParserTest(IRI testUri, String testName, String inputURL, String outputURL, String baseURL,
			RDFParser targetParser, RDFParser ntriplesParser) {
		super(testName);
		this.testUri = testUri;
		this.inputURL = inputURL;
		if (outputURL != null) {
			this.outputURL = outputURL;
		}
		this.baseURL = baseURL;
		this.targetParser = targetParser;
		this.ntriplesParser = ntriplesParser;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void runTest() throws Exception {
		// Parse input data
		// targetParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

		Set<Statement> inputCollection = new LinkedHashSet<>();
		StatementCollector inputCollector = new StatementCollector(inputCollection);
		targetParser.setRDFHandler(inputCollector);

		InputStream in = this.getClass().getResourceAsStream(inputURL);
		assertNotNull("Test resource was not found: inputURL=" + inputURL, in);

		logger.debug("test: " + inputURL);

		ParseErrorCollector el = new ParseErrorCollector();
		targetParser.setParseErrorListener(el);

		try {
			targetParser.parse(in, baseURL);
		} finally {
			in.close();

			if (!el.getFatalErrors().isEmpty()) {
				logger.error("[Turtle] Input file had fatal parsing errors: \n" + el.getFatalErrors());
			}

			if (!el.getErrors().isEmpty()) {
				logger.error("[Turtle] Input file had parsing errors: \n" + el.getErrors());
			}

			if (!el.getWarnings().isEmpty()) {
				logger.warn("[Turtle] Input file had parsing warnings: \n" + el.getWarnings());
			}
		}

		if (outputURL != null) {
			// Parse expected output data
			Set<Statement> outputCollection = new LinkedHashSet<>();
			StatementCollector outputCollector = new StatementCollector(outputCollection);
			ntriplesParser.setRDFHandler(outputCollector);

			in = this.getClass().getResourceAsStream(outputURL);
			try {
				ntriplesParser.parse(in, baseURL);
			} finally {
				in.close();
			}

			// Check equality of the two models
			if (!Models.isomorphic(inputCollection, outputCollection)) {
                Set<Statement> normActual = BNodeNormalizer.rewriteBNodes(inputCollection);
                Set<Statement> normExpected = BNodeNormalizer.rewriteBNodes(outputCollection);
                if (!Models.isomorphic(normActual, normExpected)) {
                    logger.error("===models not equal===\n"
                            + "Expected: " + outputCollection + "\n"
                            + "Actual  : " + inputCollection + "\n"
                            + "======================");
                    fail("models not equal");
                }
			}
		}
	}

    public static final class BNodeNormalizer {

        private static final ValueFactory vf = SimpleValueFactory.getInstance();

        private BNodeNormalizer() {
        }

        /**
         * Rewrite blank nodes deterministically for a set of statements.
         *
         * @param inputCollection the statements to normalize
         * @return a new Set of statements with normalized blank nodes
         */
        public static Set<Statement> rewriteBNodes(Set<Statement> inputCollection) {
            // 1. Sort statements canonically
            List<Statement> sorted = new ArrayList<>(inputCollection);
            sorted.sort(Comparator
                    .comparing((Statement s) -> canonicalString(s.getSubject()))
                    .thenComparing(s -> canonicalString(s.getPredicate()))
                    .thenComparing(s -> canonicalString(s.getObject()))
                    .thenComparing(s -> s.getContext() == null ? "" : canonicalString(s.getContext()))
            );

            // 2. Map original BNodes to deterministic ones
            Map<BNode, BNode> mapping = new HashMap<>();
            AtomicInteger counter = new AtomicInteger();
            Set<Statement> normalized = new LinkedHashSet<>();

            for (Statement st : sorted) {
                Resource subj = (Resource) rewriteValue(st.getSubject(), mapping, counter);
                IRI pred = st.getPredicate();
                Value obj = rewriteValue(st.getObject(), mapping, counter);
                Resource ctx = st.getContext() == null ? null : (Resource) rewriteValue(st.getContext(), mapping, counter);

                if (ctx == null) {
                    normalized.add(vf.createStatement(subj, pred, obj));
                } else {
                    normalized.add(vf.createStatement(subj, pred, obj, ctx));
                }
            }

            return normalized;
        }

        private static Value rewriteValue(Value value, Map<BNode, BNode> mapping, AtomicInteger counter) {
            if (value instanceof BNode) {
                BNode b = (BNode) value;
                return mapping.computeIfAbsent(b, k -> vf.createBNode("genid" + counter.incrementAndGet()));
            }
            if (value instanceof TripleTerm) {
                TripleTerm t = (TripleTerm) value;
                Resource newSubj = (Resource) rewriteValue(t.getSubject(), mapping, counter);
                IRI newPred = t.getPredicate();
                Value newObj = rewriteValue(t.getObject(), mapping, counter);
                return vf.createTripleTerm(newSubj, newPred, newObj);
            }
            return value;
        }

        private static String canonicalString(Value v) {
            if (v instanceof BNode) {
                return "bnode"; // placeholder for sorting
            }
            if (v instanceof IRI) {
                return v.stringValue();
            }
            if (v instanceof Literal) {
                return v.stringValue();
            }
            if (v instanceof TripleTerm) {
                TripleTerm t = (TripleTerm) v;
                return String.join("|",
                        canonicalString(t.getSubject()),
                        t.getPredicate().stringValue(),
                        canonicalString(t.getObject()));
            }
            return v.toString();
        }
    }

} // end inner class PositiveParserTest
