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
package org.eclipse.rdf4j.http.server.repository;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.rdf4j.model.util.VersionLabel;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterFactory;
import org.eclipse.rdf4j.rio.helpers.RDFVersionSettings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * View used to render boolean query results. Renders results in a format specified using a parameter or Accept header.
 *
 * @author Arjohn Kampman
 */
public class BooleanQueryResultView extends QueryResultView {

	private static final BooleanQueryResultView INSTANCE = new BooleanQueryResultView();

	public static BooleanQueryResultView getInstance() {
		return INSTANCE;
	}

	private BooleanQueryResultView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void renderInternal(Map model, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		BooleanQueryResultWriterFactory brWriterFactory = (BooleanQueryResultWriterFactory) model.get(FACTORY_KEY);
		VersionLabel preferredRDFFormat = (VersionLabel) model.get(PREFERRED_OUTPUT_RDF_VERSION); // TODO: Decide on if
																									// to keep
		// the version in boolean
		// queries for consistency
		BooleanQueryResultFormat brFormat = brWriterFactory.getBooleanQueryResultFormat();

		response.setStatus(SC_OK);
		// TODO: Use this or change the version, depending on what approach we decide on: ignore the
		// version/always return 1.2 or do indeed follow the user's requirements.
		setContentType(response, brFormat, null); // TODO: depending on the above and on whether boolean
													// queries do need version at all.
		setContentDisposition(model, response, brFormat);

		boolean headersOnly = (Boolean) model.get(HEADERS_ONLY);

		if (!headersOnly) {
			try (OutputStream out = response.getOutputStream()) {
				BooleanQueryResultWriter qrWriter = brWriterFactory.getWriter(out);
				qrWriter.getWriterConfig().set(RDFVersionSettings.PREFERRED_OUTPUT_RDF_VERSION, preferredRDFFormat);
				boolean value = (Boolean) model.get(QUERY_RESULT_KEY);
				qrWriter.handleBoolean(value);
			} catch (QueryResultHandlerException e) {
				if (e.getCause() != null && e.getCause() instanceof IOException) {
					throw (IOException) e.getCause();
				} else {
					throw new IOException(e);
				}
			}
		}
		logEndOfRequest(request);
	}
}
