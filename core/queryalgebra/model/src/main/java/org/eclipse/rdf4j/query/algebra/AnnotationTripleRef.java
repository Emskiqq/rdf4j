package org.eclipse.rdf4j.query.algebra;

public class AnnotationTripleRef extends ReifiedTripleRef {

	public AnnotationTripleRef() {
	}

	public AnnotationTripleRef(Var subjectVar, Var predicateVar, Var objectVar, Var exprVar, Var reifVar) {
		super(subjectVar, predicateVar, objectVar, exprVar, reifVar);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof AnnotationTripleRef) {
			AnnotationTripleRef o = (AnnotationTripleRef) other;
			return getSubjectVar().equals(o.getSubjectVar()) && getPredicateVar().equals(o.getPredicateVar())
					&& getObjectVar().equals(o.getObjectVar()) && getReifVar().equals(o.getReifVar());
		}
		return false;
	}

	@Override
	public TripleRef clone() {
		return super.clone();
	}
}
