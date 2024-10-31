package sat;

import owl.ltl.Biconditional;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.GOperator;
import owl.ltl.Literal;
import owl.ltl.MOperator;
import owl.ltl.ROperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.XOperator;
import owl.ltl.visitors.Visitor;

public class SyntaxOperatorReplacer implements Visitor<Formula>{
	
	@Override
	public Formula apply(Formula formula) {
		return formula.accept(this);
	}

	@Override
	public Formula visit(Biconditional biconditional) {
		Formula left = biconditional.left.accept(this);
		Formula right = biconditional.right.accept(this);
		//return new Biconditional(left, right);
		return Biconditional.of(left, right);
	}

	@Override
	public Formula visit(BooleanConstant booleanConstant) {
		return booleanConstant;
	}

	@Override
	public Formula visit(Conjunction conjunction) {
		return Conjunction.of(conjunction.children.stream().map(x -> x.accept(this)));
	}

	@Override
	public Formula visit(Disjunction disjunction) {		
		return Disjunction.of(disjunction.children.stream().map(x -> x.accept(this)));
	}

	@Override
	public Formula visit(FOperator fOperator) {
		Formula operand = fOperator.operand.accept(this);
		//return new FOperator(operand);
		return FOperator.of(operand);
	}

	@Override
	public Formula visit(GOperator gOperator) {
		Formula operand = gOperator.operand.accept(this);
		//return new GOperator(operand);
		return GOperator.of(operand);
	}

	@Override
	public Formula visit(Literal literal) {
		return literal;
	}

	@Override
	public Formula visit(MOperator mOperator) {
		Formula left = mOperator.left.accept(this);
		Formula right = mOperator.right.accept(this);
		//return new MOperator(left, right);
		return MOperator.of(left, right);
	}

	@Override
	public Formula visit(ROperator rOperator) {
		Formula left = rOperator.left.accept(this);
		Formula right = rOperator.right.accept(this);
		//return new ROperator(left, right);
		return ROperator.of(left, right);
	}

	@Override
	public Formula visit(UOperator uOperator) {
		Formula left = uOperator.left.accept(this);
		Formula right = uOperator.right.accept(this);
		//return new UOperator(left, right);
		return UOperator.of(left, right);
	}

	@Override
	public Formula visit(WOperator wOperator) {
		Formula left = wOperator.left.accept(this);
		Formula right = wOperator.right.accept(this);
		//return new WOperator(left, right);
		return WOperator.of(left, right);
	}

	@Override
	public Formula visit(XOperator xOperator) {
		Formula operand = xOperator.operand.accept(this);
		//return new XOperator(operand);
		return XOperator.of(operand);
	}
	
}
