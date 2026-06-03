package org.eclipse.rdf4j.query.algebra;

public class HasLang extends UnaryValueOperator {
	public HasLang() {
	}

	public HasLang(ValueExpr arg) {
		super(arg);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "HasLang".hashCode();
	}

	@Override
	public HasLang clone() {
		return (HasLang) super.clone();
	}
}
