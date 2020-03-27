package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.dynamicRepairDefinition;

import gov.nasa.jpf.symbc.veritesting.ast.def.*;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprMapVisitor;

import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprVisitor;
import za.ac.sun.cs.green.expr.*;

import java.util.ArrayList;
import java.util.List;

public class VariableRangeExprVisitor extends ExprMapVisitor implements ExprVisitor<Expression> {

    public static List<Integer> rangeValues;

    public static boolean inVarOfInterestScope;

    public VariableRangeExprVisitor() {
        super();
        inVarOfInterestScope = false; // trying to excluding assignment statements that are initially not what we are looking for.
        rangeValues = new ArrayList<>(); // all possible values encountered for the variable we are looking for.
    }

    @Override
    // we are not implementing simplification at that point. So we  should not count constants in an operation as a possible value, since it is not reduced down to a single constant.
    // just clear everything to abort safely.
    public Expression visit(Operation expr) {
        // if we ever visited an operation then we assume that it can't be reduced and we assume there isn't a possible range of value to be collected for that variable. Thus we clear everything.
        rangeValues = new ArrayList<>();
        inVarOfInterestScope = false;
        VariableRangeVisitor.interestedVarName = new ArrayList<>();
        return expr;
    }

    @Override
    public Expression visit(IntConstant expr) {
        if (inVarOfInterestScope)
            rangeValues.add(expr.getValue());

        return expr;
    }

    @Override
    public Expression visit(IntVariable expr) {
        if (!VariableRangeVisitor.relatedSymInput.equals(expr.toString())) {
            rangeValues = new ArrayList<>();
            inVarOfInterestScope = false;
            VariableRangeVisitor.interestedVarName = new ArrayList<>();
        }
        else{
            return expr;
        }
        return expr;
    }

    @Override
    public Expression visit(GammaVarExpr expr) {
        return new GammaVarExpr(expr.condition,
                eva.accept(expr.thenExpr),
                eva.accept(expr.elseExpr));
    }

    @Override
    public Expression visit(IfThenElseExpr expr) {
        return new IfThenElseExpr(expr.condition,
                eva.accept(expr.thenExpr),
                eva.accept(expr.elseExpr));
    }

    @Override
    public Expression visit(WalaVarExpr expr) {
        //visiting a WalaVarExpr on the right handside means that we still need to collect the definition of this new expr as well.
        if ((inVarOfInterestScope) && (!VariableRangeVisitor.interestedVarName.contains(expr.toString())))
            VariableRangeVisitor.interestedVarName.add(expr.toString());
        return expr;
    }

    @Override
    public Expression visit(AstVarExpr expr) {
        if ((inVarOfInterestScope) && (!VariableRangeVisitor.interestedVarName.contains(expr.toString())))
            VariableRangeVisitor.interestedVarName.add(expr.toString());
        return expr;
    }

    @Override
    public Expression visit(FieldRefVarExpr expr) {
        if ((inVarOfInterestScope) && (!VariableRangeVisitor.interestedVarName.contains(expr.toString())))
            VariableRangeVisitor.interestedVarName.add(expr.toString());
        return expr;
    }

    @Override
    public Expression visit(ArrayRefVarExpr expr) {
        if ((inVarOfInterestScope) && (!VariableRangeVisitor.interestedVarName.contains(expr.toString())))
            VariableRangeVisitor.interestedVarName.add(expr.toString());
        return expr;
    }


}
