/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclParsingException;

public interface ShapeSource extends AutoCloseable {

	Model DASH_CONSTANTS = resourceAsModel("shacl-sparql-inference/dashConstants.ttl");

	private static Model resourceAsModel(String filename) {
		try (InputStream resourceAsStream = ShapeSource.class.getClassLoader().getResourceAsStream(filename)) {
			return Rio.parse(resourceAsStream, "", RDFFormat.TURTLE);
		} catch (IOException e) {
			throw new IllegalStateException("Resource could not be read: " + filename, e);
		}
	}

	static Stream<ShapesGraph> getRsxDataAndShapesGraphLink(SailConnection connection, Resource[] context) {
		Stream<ShapesGraph> rsxDataAndShapesGraphLink;

		try (var stream = connection.getStatements(null, RDF.TYPE, RSX.DataAndShapesGraphLink, false, context)
				.stream()) {

			var collect = stream.collect(Collectors.toList()); // consume entire stream to ensure that it can be closed

			rsxDataAndShapesGraphLink = collect
					.stream()
					.map(statement -> {
						List<Resource> dataGraphList;
						List<Resource> shapesGraphList;

						Resource subject = statement.getSubject();
						Resource subjectContext = statement.getContext();

						try (Stream<? extends Statement> dataGraphStream = connection
								.getStatements(subject, RSX.dataGraph, null, false, subjectContext)
								.stream()) {
							dataGraphList = dataGraphStream
									.map(Statement::getObject)
									.map(v -> {
										try {
											return ((Resource) v);
										} catch (ClassCastException e) {
											throw new ShaclParsingException(
													"Expected an Resource for rsx:dataGraph, found: " + v);
										}
									})
									.collect(Collectors.toList());
						}

						try (Stream<? extends Statement> dataGraphStream = connection
								.getStatements(subject, RSX.shapesGraph, null, false, subjectContext)
								.stream()) {
							shapesGraphList = dataGraphStream
									.map(Statement::getObject)
									.map(v -> {
										try {
											return ((Resource) v);
										} catch (ClassCastException e) {
											throw new ShaclParsingException(
													"Expected an Resource for rsx:shapesGraph, found: " + v);
										}
									})
									.collect(Collectors.toList());
						}

						return new ShapesGraph(dataGraphList, shapesGraphList);

					})
					.collect(Collectors.toList()) // consume entire stream to ensure that it can be closed
					.stream();

		}
		return rsxDataAndShapesGraphLink;
	}

	static Stream<ShapesGraph> getRsxDataAndShapesGraphLink(RepositoryConnection connection, Resource[] context) {

		Stream<ShapesGraph> rsxDataAndShapesGraphLink;
		try (var stream = connection.getStatements(null, RDF.TYPE, RSX.DataAndShapesGraphLink, false, context)
				.stream()) {

			var collect = stream.collect(Collectors.toList()); // consume entire stream to ensure that it can be closed

			rsxDataAndShapesGraphLink = collect
					.stream()
					.map(statement -> {
						List<Resource> dataGraphList;
						List<Resource> shapesGraphList;

						Resource subject = statement.getSubject();
						Resource subjectContext = statement.getContext();

						try (Stream<? extends Statement> dataGraphStream = connection
								.getStatements(subject, RSX.dataGraph, null, false, subjectContext)
								.stream()) {
							dataGraphList = dataGraphStream
									.map(Statement::getObject)
									.map(v -> {
										try {
											return ((Resource) v);
										} catch (ClassCastException e) {
											throw new ShaclParsingException(
													"Expected an Resource for rsx:dataGraph, found: " + v);
										}
									})
									.collect(Collectors.toList());
						}

						try (Stream<? extends Statement> dataGraphStream = connection
								.getStatements(subject, RSX.shapesGraph, null, false, subjectContext)
								.stream()) {
							shapesGraphList = dataGraphStream
									.map(Statement::getObject)
									.map(v -> {
										try {
											return ((Resource) v);
										} catch (ClassCastException e) {
											throw new ShaclParsingException(
													"Expected an Resource for rsx:shapesGraph, found: " + v);
										}
									})
									.collect(Collectors.toList());
						}

						return new ShapesGraph(dataGraphList, shapesGraphList);

					})
					.collect(Collectors.toList()) // consume entire stream to ensure that it can be closed
					.stream();

		}
		return rsxDataAndShapesGraphLink;
	}

	ShapeSource withContext(Resource[] context);

	Resource[] getActiveContexts();

	Stream<ShapesGraph> getAllShapeContexts();

	Stream<Resource> getTargetableShape();

	boolean isType(Resource subject, IRI type);

	Stream<Resource> getSubjects(Predicates predicate);

	Stream<Value> getObjects(Resource subject, Predicates predicate);

	Stream<Statement> getAllStatements(Resource id);

	Value getRdfFirst(Resource subject);

	Resource getRdfRest(Resource subject);

	@Override
	void close();

	class ShapesGraph {
		private final static Resource[] allContext = {};

		private final Resource[] dataGraph;
		private final Resource[] shapesGraph;

		public ShapesGraph(Resource dataGraph, List<? extends Statement> shapesGraph) {
			this.dataGraph = new Resource[] { handleDefaultGraph(dataGraph) };
			this.shapesGraph = shapesGraph
					.stream()
					.map(Statement::getObject)
					.map(o -> {
						try {
							return ((Resource) o);
						} catch (ClassCastException e) {
							throw new ShaclParsingException("Expected an Resource for sh:shapesGraph, found: " + o);
						}
					})
					.map(ShapesGraph::handleDefaultGraph)
					.toArray(Resource[]::new);
		}

		public ShapesGraph(List<Resource> dataGraph, List<Resource> shapesGraph) {
			this.dataGraph = dataGraph
					.stream()
					.map(ShapesGraph::handleDefaultGraph)
					.toArray(Resource[]::new);
			this.shapesGraph = shapesGraph
					.stream()
					.map(ShapesGraph::handleDefaultGraph)
					.toArray(Resource[]::new);
		}

		public ShapesGraph(IRI shapesGraph) {
			this.dataGraph = allContext;
			this.shapesGraph = new Resource[] { handleDefaultGraph(shapesGraph) };
		}

		private static Resource handleDefaultGraph(Resource graph) {
			if (RDF4J.NIL.equals(graph)) {
				return null;
			}
			if (SESAME.NIL.equals(graph)) {
				return null;
			}
			return graph;
		}

		public Resource[] getDataGraph() {
			return dataGraph;
		}

		public Resource[] getShapesGraph() {
			return shapesGraph;
		}
	}

	enum Predicates {

		ABSTRACT_RESULT(SHACL.ABSTRACT_RESULT),
		AND_CONSTRAINT_COMPONENT(SHACL.AND_CONSTRAINT_COMPONENT),
		AND_CONSTRAINT_COMPONENT_AND(SHACL.AND_CONSTRAINT_COMPONENT_AND),
		BLANK_NODE(SHACL.BLANK_NODE),
		BLANK_NODE_OR_IRI(SHACL.BLANK_NODE_OR_IRI),
		BLANK_NODE_OR_LITERAL(SHACL.BLANK_NODE_OR_LITERAL),
		CLASS_CONSTRAINT_COMPONENT(SHACL.CLASS_CONSTRAINT_COMPONENT),
		CLASS_CONSTRAINT_COMPONENT_CLASS(SHACL.CLASS_CONSTRAINT_COMPONENT_CLASS),
		CLOSED_CONSTRAINT_COMPONENT(SHACL.CLOSED_CONSTRAINT_COMPONENT),
		CLOSED_CONSTRAINT_COMPONENT_CLOSED(SHACL.CLOSED_CONSTRAINT_COMPONENT_CLOSED),
		CLOSED_CONSTRAINT_COMPONENT_IGNORED_PROPERTIES(SHACL.CLOSED_CONSTRAINT_COMPONENT_IGNORED_PROPERTIES),
		CONSTRAINT_COMPONENT(SHACL.CONSTRAINT_COMPONENT),
		DATATYPE_CONSTRAINT_COMPONENT(SHACL.DATATYPE_CONSTRAINT_COMPONENT),
		DATATYPE_CONSTRAINT_COMPONENT_DATATYPE(SHACL.DATATYPE_CONSTRAINT_COMPONENT_DATATYPE),
		DERIVED_VALUES_CONSTRAINT_COMPONENT(SHACL.DERIVED_VALUES_CONSTRAINT_COMPONENT),
		DISJOINT_CONSTRAINT_COMPONENT(SHACL.DISJOINT_CONSTRAINT_COMPONENT),
		DISJOINT_CONSTRAINT_COMPONENT_DISJOINT(SHACL.DISJOINT_CONSTRAINT_COMPONENT_DISJOINT),
		EQUALS_CONSTRAINT_COMPONENT(SHACL.EQUALS_CONSTRAINT_COMPONENT),
		EQUALS_CONSTRAINT_COMPONENT_EQUALS(SHACL.EQUALS_CONSTRAINT_COMPONENT_EQUALS),
		FUNCTION(SHACL.FUNCTION),
		HAS_VALUE_CONSTRAINT_COMPONENT(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT),
		HAS_VALUE_CONSTRAINT_COMPONENT_HAS_VALUE(SHACL.HAS_VALUE_CONSTRAINT_COMPONENT_HAS_VALUE),
		IRI(SHACL.IRI),
		IRI_OR_LITERAL(SHACL.IRI_OR_LITERAL),
		IN_CONSTRAINT_COMPONENT(SHACL.IN_CONSTRAINT_COMPONENT),
		IN_CONSTRAINT_COMPONENT_IN(SHACL.IN_CONSTRAINT_COMPONENT_IN),
		INFO(SHACL.INFO),
		LANGUAGE_IN_CONSTRAINT_COMPONENT(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT),
		LANGUAGE_IN_CONSTRAINT_COMPONENT_LANGUAGE_IN(SHACL.LANGUAGE_IN_CONSTRAINT_COMPONENT_LANGUAGE_IN),
		LESS_THAN_CONSTRAINT_COMPONENT(SHACL.LESS_THAN_CONSTRAINT_COMPONENT),
		LESS_THAN_CONSTRAINT_COMPONENT_LESS_THAN(SHACL.LESS_THAN_CONSTRAINT_COMPONENT_LESS_THAN),
		LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT(SHACL.LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT),
		LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT_LESS_THAN_OR_EQUALS(
				SHACL.LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT_LESS_THAN_OR_EQUALS),
		LITERAL(SHACL.LITERAL),
		MAX_COUNT_CONSTRAINT_COMPONENT(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT),
		MAX_COUNT_CONSTRAINT_COMPONENT_MAX_COUNT(SHACL.MAX_COUNT_CONSTRAINT_COMPONENT_MAX_COUNT),
		MAX_EXCLUSIVE_CONSTRAINT_COMPONENT(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT),
		MAX_EXCLUSIVE_CONSTRAINT_COMPONENT_MAX_EXCLUSIVE(SHACL.MAX_EXCLUSIVE_CONSTRAINT_COMPONENT_MAX_EXCLUSIVE),
		MAX_INCLUSIVE_CONSTRAINT_COMPONENT(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT),
		MAX_INCLUSIVE_CONSTRAINT_COMPONENT_MAX_INCLUSIVE(SHACL.MAX_INCLUSIVE_CONSTRAINT_COMPONENT_MAX_INCLUSIVE),
		MAX_LENGTH_CONSTRAINT_COMPONENT(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT),
		MAX_LENGTH_CONSTRAINT_COMPONENT_MAX_LENGTH(SHACL.MAX_LENGTH_CONSTRAINT_COMPONENT_MAX_LENGTH),
		MIN_COUNT_CONSTRAINT_COMPONENT(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT),
		MIN_COUNT_CONSTRAINT_COMPONENT_MIN_COUNT(SHACL.MIN_COUNT_CONSTRAINT_COMPONENT_MIN_COUNT),
		MIN_EXCLUSIVE_CONSTRAINT_COMPONENT(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT),
		MIN_EXCLUSIVE_CONSTRAINT_COMPONENT_MIN_EXCLUSIVE(SHACL.MIN_EXCLUSIVE_CONSTRAINT_COMPONENT_MIN_EXCLUSIVE),
		MIN_INCLUSIVE_CONSTRAINT_COMPONENT(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT),
		MIN_INCLUSIVE_CONSTRAINT_COMPONENT_MIN_INCLUSIVE(SHACL.MIN_INCLUSIVE_CONSTRAINT_COMPONENT_MIN_INCLUSIVE),
		MIN_LENGTH_CONSTRAINT_COMPONENT(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT),
		MIN_LENGTH_CONSTRAINT_COMPONENT_MIN_LENGTH(SHACL.MIN_LENGTH_CONSTRAINT_COMPONENT_MIN_LENGTH),
		NODE_CONSTRAINT_COMPONENT(SHACL.NODE_CONSTRAINT_COMPONENT),
		NODE_CONSTRAINT_COMPONENT_NODE(SHACL.NODE_CONSTRAINT_COMPONENT_NODE),
		NODE_KIND(SHACL.NODE_KIND),
		NODE_KIND_CONSTRAINT_COMPONENT(SHACL.NODE_KIND_CONSTRAINT_COMPONENT),
		NODE_KIND_CONSTRAINT_COMPONENT_NODE_KIND(SHACL.NODE_KIND_CONSTRAINT_COMPONENT_NODE_KIND),
		NODE_SHAPE(SHACL.NODE_SHAPE),
		NOT_CONSTRAINT_COMPONENT(SHACL.NOT_CONSTRAINT_COMPONENT),
		NOT_CONSTRAINT_COMPONENT_NOT(SHACL.NOT_CONSTRAINT_COMPONENT_NOT),
		OR_CONSTRAINT_COMPONENT(SHACL.OR_CONSTRAINT_COMPONENT),
		OR_CONSTRAINT_COMPONENT_OR(SHACL.OR_CONSTRAINT_COMPONENT_OR),
		PARAMETER(SHACL.PARAMETER),
		PARAMETERIZABLE(SHACL.PARAMETERIZABLE),
		PATTERN_CONSTRAINT_COMPONENT(SHACL.PATTERN_CONSTRAINT_COMPONENT),
		PATTERN_CONSTRAINT_COMPONENT_FLAGS(SHACL.PATTERN_CONSTRAINT_COMPONENT_FLAGS),
		PATTERN_CONSTRAINT_COMPONENT_PATTERN(SHACL.PATTERN_CONSTRAINT_COMPONENT_PATTERN),
		PREFIX_DECLARATION(SHACL.PREFIX_DECLARATION),
		PROPERTY_CONSTRAINT_COMPONENT(SHACL.PROPERTY_CONSTRAINT_COMPONENT),
		PROPERTY_CONSTRAINT_COMPONENT_PROPERTY(SHACL.PROPERTY_CONSTRAINT_COMPONENT_PROPERTY),
		PROPERTY_GROUP(SHACL.PROPERTY_GROUP),
		PROPERTY_SHAPE(SHACL.PROPERTY_SHAPE),
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT(SHACL.QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT),
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MAX_COUNT(
				SHACL.QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MAX_COUNT),
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE(
				SHACL.QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE),
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT(
				SHACL.QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT),
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT(SHACL.QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT),
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MIN_COUNT(
				SHACL.QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MIN_COUNT),
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE(
				SHACL.QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE),
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT(
				SHACL.QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT),
		RESULT_ANNOTATION(SHACL.RESULT_ANNOTATION),
		SPARQL_ASK_EXECUTABLE(SHACL.SPARQL_ASK_EXECUTABLE),
		SPARQL_ASK_VALIDATOR(SHACL.SPARQL_ASK_VALIDATOR),
		SPARQL_CONSTRAINT(SHACL.SPARQL_CONSTRAINT),
		SPARQL_CONSTRAINT_COMPONENT(SHACL.SPARQL_CONSTRAINT_COMPONENT),
		SPARQL_CONSTRAINT_COMPONENT_SPARQL(SHACL.SPARQL_CONSTRAINT_COMPONENT_SPARQL),
		SPARQL_CONSTRUCT_EXECUTABLE(SHACL.SPARQL_CONSTRUCT_EXECUTABLE),
		SPARQL_EXECUTABLE(SHACL.SPARQL_EXECUTABLE),
		SPARQL_FUNCTION(SHACL.SPARQL_FUNCTION),
		SPARQL_SELECT_EXECUTABLE(SHACL.SPARQL_SELECT_EXECUTABLE),
		SPARQL_SELECT_VALIDATOR(SHACL.SPARQL_SELECT_VALIDATOR),
		SPARQL_TARGET(SHACL.SPARQL_TARGET),
		SPARQL_TARGET_TYPE(SHACL.SPARQL_TARGET_TYPE),
		SPARQL_UPDATE_EXECUTABLE(SHACL.SPARQL_UPDATE_EXECUTABLE),
		SPARQL_VALUES_DERIVER(SHACL.SPARQL_VALUES_DERIVER),
		SEVERITY(SHACL.SEVERITY),
		SHAPE(SHACL.SHAPE),
		TARGET(SHACL.TARGET),
		TARGET_TYPE(SHACL.TARGET_TYPE),
		UNIQUE_LANG_CONSTRAINT_COMPONENT(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT),
		UNIQUE_LANG_CONSTRAINT_COMPONENT_UNIQUE_LANG(SHACL.UNIQUE_LANG_CONSTRAINT_COMPONENT_UNIQUE_LANG),
		VALIDATION_REPORT(SHACL.VALIDATION_REPORT),
		VALIDATION_RESULT(SHACL.VALIDATION_RESULT),
		VALIDATOR(SHACL.VALIDATOR),
		VALUES_DERIVER(SHACL.VALUES_DERIVER),
		VIOLATION(SHACL.VIOLATION),
		WARNING(SHACL.WARNING),
		XONE_CONSTRAINT_COMPONENT(SHACL.XONE_CONSTRAINT_COMPONENT),
		XONE_CONSTRAINT_COMPONENT_XONE(SHACL.XONE_CONSTRAINT_COMPONENT_XONE),
		ALTERNATIVE_PATH(SHACL.ALTERNATIVE_PATH),
		AND(SHACL.AND),
		ANNOTATION_PROPERTY(SHACL.ANNOTATION_PROPERTY),
		ANNOTATION_VALUE(SHACL.ANNOTATION_VALUE),
		ANNOTATION_VAR_NAME(SHACL.ANNOTATION_VAR_NAME),
		ASK(SHACL.ASK),
		CLASS(SHACL.CLASS),
		CLOSED(SHACL.CLOSED),
		CONFORMS(SHACL.CONFORMS),
		CONSTRUCT(SHACL.CONSTRUCT),
		DATATYPE(SHACL.DATATYPE),
		DEACTIVATED(SHACL.DEACTIVATED),
		DECLARE(SHACL.DECLARE),
		DEFAULT_VALUE(SHACL.DEFAULT_VALUE),
		DERIVED_VALUES(SHACL.DERIVED_VALUES),
		DESCRIPTION(SHACL.DESCRIPTION),
		DETAIL(SHACL.DETAIL),
		DISJOINT(SHACL.DISJOINT),
		EQUALS(SHACL.EQUALS),
		FLAGS(SHACL.FLAGS),
		FOCUS_NODE(SHACL.FOCUS_NODE),
		GROUP(SHACL.GROUP),
		HAS_VALUE(SHACL.HAS_VALUE),
		IGNORED_PROPERTIES(SHACL.IGNORED_PROPERTIES),
		IN(SHACL.IN),
		INVERSE_PATH(SHACL.INVERSE_PATH),
		LABEL_TEMPLATE(SHACL.LABEL_TEMPLATE),
		LANGUAGE_IN(SHACL.LANGUAGE_IN),
		LESS_THAN(SHACL.LESS_THAN),
		LESS_THAN_OR_EQUALS(SHACL.LESS_THAN_OR_EQUALS),
		MAX_COUNT(SHACL.MAX_COUNT),
		MAX_EXCLUSIVE(SHACL.MAX_EXCLUSIVE),
		MAX_INCLUSIVE(SHACL.MAX_INCLUSIVE),
		MAX_LENGTH(SHACL.MAX_LENGTH),
		MESSAGE(SHACL.MESSAGE),
		MIN_COUNT(SHACL.MIN_COUNT),
		MIN_EXCLUSIVE(SHACL.MIN_EXCLUSIVE),
		MIN_INCLUSIVE(SHACL.MIN_INCLUSIVE),
		MIN_LENGTH(SHACL.MIN_LENGTH),
		NAME(SHACL.NAME),
		NAMESPACE_PROP(SHACL.NAMESPACE_PROP),
		NODE(SHACL.NODE),
		NODE_KIND_PROP(SHACL.NODE_KIND_PROP),
		NODE_VALIDATOR(SHACL.NODE_VALIDATOR),
		NOT(SHACL.NOT),
		ONE_OR_MORE_PATH(SHACL.ONE_OR_MORE_PATH),
		OPTIONAL(SHACL.OPTIONAL),
		OR(SHACL.OR),
		ORDER(SHACL.ORDER),
		PARAMETER_PROP(SHACL.PARAMETER_PROP),
		PATH(SHACL.PATH),
		PATTERN(SHACL.PATTERN),
		PREFIX_PROP(SHACL.PREFIX_PROP),
		PREFIXES(SHACL.PREFIXES),
		PROPERTY(SHACL.PROPERTY),
		PROPERTY_VALIDATOR(SHACL.PROPERTY_VALIDATOR),
		QUALIFIED_MAX_COUNT(SHACL.QUALIFIED_MAX_COUNT),
		QUALIFIED_MIN_COUNT(SHACL.QUALIFIED_MIN_COUNT),
		QUALIFIED_VALUE_SHAPE(SHACL.QUALIFIED_VALUE_SHAPE),
		QUALIFIED_VALUE_SHAPES_DISJOINT(SHACL.QUALIFIED_VALUE_SHAPES_DISJOINT),
		RESULT(SHACL.RESULT),
		RESULT_ANNOTATION_PROP(SHACL.RESULT_ANNOTATION_PROP),
		RESULT_MESSAGE(SHACL.RESULT_MESSAGE),
		RESULT_PATH(SHACL.RESULT_PATH),
		RESULT_SEVERITY(SHACL.RESULT_SEVERITY),
		RETURN_TYPE(SHACL.RETURN_TYPE),
		SELECT(SHACL.SELECT),
		SEVERITY_PROP(SHACL.SEVERITY_PROP),
		SHAPES_GRAPH(SHACL.SHAPES_GRAPH),
		SHAPES_GRAPH_WELL_FORMED(SHACL.SHAPES_GRAPH_WELL_FORMED),
		SOURCE_CONSTRAINT(SHACL.SOURCE_CONSTRAINT),
		SOURCE_CONSTRAINT_COMPONENT(SHACL.SOURCE_CONSTRAINT_COMPONENT),
		SOURCE_SHAPE(SHACL.SOURCE_SHAPE),
		SPARQL(SHACL.SPARQL),
		TARGET_PROP(SHACL.TARGET_PROP),
		TARGET_CLASS(SHACL.TARGET_CLASS),
		TARGET_NODE(SHACL.TARGET_NODE),
		TARGET_OBJECTS_OF(SHACL.TARGET_OBJECTS_OF),
		TARGET_SUBJECTS_OF(SHACL.TARGET_SUBJECTS_OF),
		UNIQUE_LANG(SHACL.UNIQUE_LANG),
		UPDATE(SHACL.UPDATE),
		VALIDATOR_PROP(SHACL.VALIDATOR_PROP),
		VALUE(SHACL.VALUE),
		XONE(SHACL.XONE),
		ZERO_OR_MORE_PATH(SHACL.ZERO_OR_MORE_PATH),
		ZERO_OR_ONE_PATH(SHACL.ZERO_OR_ONE_PATH),
		RSX_targetShape(RSX.targetShape);

		private final IRI iri;

		Predicates(IRI iri) {
			this.iri = iri;
		}

		public org.eclipse.rdf4j.model.IRI getIRI() {
			return iri;
		}
	}

}
