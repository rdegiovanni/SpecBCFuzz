package fuzz.mutation.visitors;

import java.util.HashSet;
import java.util.Set;

import owl.ltl.Biconditional;
import owl.ltl.BooleanConstant;
import owl.ltl.Conjunction;
import owl.ltl.Disjunction;
import owl.ltl.FOperator;
import owl.ltl.Formula;
import owl.ltl.FrequencyG;
import owl.ltl.GOperator;
import owl.ltl.HOperator;
import owl.ltl.Literal;
import owl.ltl.MOperator;
import owl.ltl.OOperator;
import owl.ltl.ROperator;
import owl.ltl.SOperator;
import owl.ltl.TOperator;
import owl.ltl.UOperator;
import owl.ltl.WOperator;
import owl.ltl.XOperator;
import owl.ltl.YOperator;
import owl.ltl.ZOperator;
import owl.ltl.visitors.Visitor;

public class PropositionVariablesExtractor implements Visitor<Set<Literal>> {
    @Override
    public Set<Literal> apply(Formula formula) {
        return formula.accept(this);
    }

    @Override
    public Set<Literal> visit(Biconditional biconditional) {
        Set<Literal> left = biconditional.left.accept(this);
        Set<Literal> right = biconditional.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(BooleanConstant booleanConstant) {
        return new HashSet<>();
    }

    @Override
    public Set<Literal> visit(Conjunction conjunction) {
        Set<Literal> result = new HashSet<Literal>();
        for(Formula c : conjunction.children)
            result.addAll(c.accept(this));
        return result;
    }

    @Override
    public Set<Literal> visit(Disjunction disjunction) {
        Set<Literal> result = new HashSet<Literal>();
        for(Formula c : disjunction.children)
            result.addAll(c.accept(this));
        return result;
    }

    @Override
    public Set<Literal> visit(FOperator fOperator) {
        return fOperator.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(FrequencyG freq) {
        return freq.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(GOperator gOperator) {
        return gOperator.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(Literal literal) {
        Set<Literal> res = new HashSet<>();
        res.add(literal);
        return res;
    }

    @Override
    public Set<Literal> visit(MOperator mOperator) {
        Set<Literal> left = mOperator.left.accept(this);
        Set<Literal> right = mOperator.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(ROperator rOperator) {
        Set<Literal> left = rOperator.left.accept(this);
        Set<Literal> right = rOperator.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(UOperator uOperator) {
        Set<Literal> left = uOperator.left.accept(this);
        Set<Literal> right = uOperator.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(WOperator wOperator) {
        Set<Literal> left = wOperator.left.accept(this);
        Set<Literal> right = wOperator.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(OOperator oOperator) {
        return oOperator.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(HOperator hOperator) {
        return hOperator.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(TOperator tOperator) {
        Set<Literal> left = tOperator.left.accept(this);
        Set<Literal> right = tOperator.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(SOperator sOperator) {
        Set<Literal> left = sOperator.left.accept(this);
        Set<Literal> right = sOperator.right.accept(this);
        left.addAll(right);
        return left;
    }

    @Override
    public Set<Literal> visit(YOperator yOperator) {
        return yOperator.operand.accept(this);
    }

    @Override
    public Set<Literal> visit(ZOperator zOperator) {
        return zOperator.operand.accept(this);
    }	
}
