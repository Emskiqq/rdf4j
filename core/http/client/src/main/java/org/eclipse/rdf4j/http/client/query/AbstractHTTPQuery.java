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
package org.eclipse.rdf4j.http.client.query;

import java.util.Iterator;

import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.model.util.VersionLabel;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.AbstractQuery;

/**
 * Base class for any {@link Query} operation over HTTP.
 *
 * @author Andreas Schwarte
 */
public abstract class AbstractHTTPQuery extends AbstractQuery implements RDFVersionAware {

	private final SPARQLProtocolSession httpClient;

	protected final QueryLanguage queryLanguage;

	protected final String queryString;

	protected final String baseURI;

	protected VersionLabel preferredRDFResultsVersion;

	protected VersionLabel qlVersion;

	protected AbstractHTTPQuery(SPARQLProtocolSession httpClient, QueryLanguage queryLanguage, String queryString,
			String baseURI) {
		super();
		this.httpClient = httpClient;
		this.queryLanguage = queryLanguage;
		this.queryString = queryString;
		// TODO think about the following
		// for legacy reasons we should support the empty string for baseURI
		// this is used in the SPARQL repository in several places, e.g. in
		// getStatements
		this.baseURI = baseURI != null && !baseURI.isEmpty() ? baseURI : null;
	}

	protected AbstractHTTPQuery(SPARQLProtocolSession httpClient, QueryLanguage queryLanguage, VersionLabel qlVersion,
			String queryString,
			String baseURI) {
		super();
		this.httpClient = httpClient;
		this.queryLanguage = queryLanguage;
		this.qlVersion = qlVersion;
		this.queryString = queryString;
		// TODO think about the following
		// for legacy reasons we should support the empty string for baseURI
		// this is used in the SPARQL repository in several places, e.g. in
		// getStatements
		this.baseURI = baseURI != null && !baseURI.isEmpty() ? baseURI : null;
	}

	/**
	 * @return Returns the {@link SPARQLProtocolSession} to be used for all HTTP based interaction
	 */
	protected SPARQLProtocolSession getHttpClient() {
		return httpClient;
	}

	public Binding[] getBindingsArray() {
		BindingSet bindings = this.getBindings();

		Binding[] bindingsArray = new Binding[bindings.size()];

		Iterator<Binding> iter = bindings.iterator();
		for (int i = 0; i < bindings.size(); i++) {
			bindingsArray[i] = iter.next();
		}

		return bindingsArray;
	}

	@Override
	public void setMaxExecutionTime(int maxExecutionTimeSeconds) {
		super.setMaxExecutionTime(maxExecutionTimeSeconds);
		this.httpClient.setConnectionTimeout(1000L * this.getMaxExecutionTime());
	}

	@Override
	public String toString() {
		return queryString;
	}

	public VersionLabel getPreferredRDFResultsVersion() {
		return this.preferredRDFResultsVersion;
	}

	public void setPreferredRDFResultsVersion(VersionLabel preferredRDFResultsVersion) {
		this.preferredRDFResultsVersion = preferredRDFResultsVersion;
	}

	@Override
	public VersionLabel getQLVersion() {
		return this.qlVersion;
	}

	@Override
	public void setQLVersion(VersionLabel qlVersion) {
		this.qlVersion = qlVersion;
	}
}
