/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EqualsJoinValue;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class OrConstraintComponent extends LogicalOperatorConstraintComponent {
	List<Shape> or;

	public OrConstraintComponent(Resource id, ShapeSource shapeSource, Shape.ParseSettings parseSettings, Cache cache) {
		super(id);
		or = ShaclAstLists.toList(shapeSource, id, Resource.class)
				.stream()
				.map(r -> new ShaclProperties(r, shapeSource))
				.map(p -> {
					if (p.getType() == SHACL.NODE_SHAPE) {
						return NodeShape.getInstance(p, shapeSource, parseSettings, cache);
					} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
						return PropertyShape.getInstance(p, shapeSource, parseSettings, cache);
					}
					throw new IllegalStateException("Unknown shape type for " + p.getId());
				})
				.collect(Collectors.toList());
	}

	public OrConstraintComponent(OrConstraintComponent orConstraintComponent) {
		super(orConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.OR, getId());
		if (!cycleDetection.contains(getId())) {
			cycleDetection.add(getId());
			or.forEach(o -> o.toModel(null, null, model, cycleDetection));
		}

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(or.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
		}
	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		for (Shape shape : or) {
			shape.setTargetChain(targetChain.setOptimizable(false));
		}
	}

	public List<Shape> getOr() {
		return Collections.unmodifiableList(or);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.OrConstraintComponent;
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		throw new ShaclUnsupportedException();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();
		PlanNodeProvider planNodeProvider;

		if (overrideTargetNode != null) {
			planNodeProvider = overrideTargetNode;
		} else {
			planNodeProvider = BufferedSplitter.getInstance(
					getAllTargetsPlan(connectionsGroup, validationSettings.getDataGraph(), scope,
							stableRandomVariableProvider, validationSettings),
					false);
		}

		PlanNode orPlanNodes = or.stream()
				.map(or -> or.generateTransactionalValidationPlan(
						connectionsGroup,
						validationSettings,
						planNodeProvider,
						scope
				)
				)
				.reduce((a, b) -> new EqualsJoinValue(a, b, false, connectionsGroup))
				.orElse(EmptyNode.getInstance());

		return Unique.getInstance(orPlanNodes, false, connectionsGroup);
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		PlanNode allTargets;

		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			allTargets = Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan, connectionsGroup), true,
					connectionsGroup);
		} else {
			allTargets = getTargetChain()
					.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, scope, true, null);

		}

		PlanNode planNode = or.stream()
				.map(or -> or.getAllTargetsPlan(connectionsGroup, dataGraph, scope,
						new StatementMatcher.StableRandomVariableProvider(), validationSettings))
				.distinct()
				.reduce((nodes, nodes2) -> UnionNode.getInstanceDedupe(connectionsGroup, nodes, nodes2))
				.orElse(EmptyNode.getInstance());

		return Unique.getInstance(UnionNode.getInstanceDedupe(connectionsGroup, allTargets, planNode), false,
				connectionsGroup);
	}

	@Override
	public ConstraintComponent deepClone() {

		OrConstraintComponent constraintComponent = new OrConstraintComponent(this);
		constraintComponent.or = or.stream()
				.map(ConstraintComponent::deepClone)
				.map(a -> ((Shape) a))
				.collect(Collectors.toList());
		return constraintComponent;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		for (Shape c : or) {
			if (c.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(Variable<Value> subject,
			Variable<Value> object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		return buildSparqlValidNodes_rsx_targetShape_inner(subject, object, rdfsSubClassOfReasoner, scope,
				stableRandomVariableProvider, or,
				getTargetChain(),
				SparqlFragment::union, SparqlFragment::or);

	}

	@Override
	public List<Literal> getDefaultMessage() {
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		OrConstraintComponent that = (OrConstraintComponent) o;

		return or.equals(that.or);
	}

	@Override
	public int hashCode() {
		return or.hashCode() + "OrConstraintComponent".hashCode();
	}
}
