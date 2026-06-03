package org.eclipse.rdf4j.query.algebra;

public class HasLangDir extends UnaryValueOperator {
	public HasLangDir() {
	}

	public HasLangDir(ValueExpr arg) {
		super(arg);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "HasLangDir".hashCode();
	}

	@Override
	public HasLangDir clone() {
		return (HasLangDir) super.clone();
	}
}
