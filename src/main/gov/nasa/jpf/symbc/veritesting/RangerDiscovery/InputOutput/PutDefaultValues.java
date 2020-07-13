package gov.nasa.jpf.symbc.veritesting.RangerDiscovery.InputOutput;

import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.DiscoverContract;
import gov.nasa.jpf.symbc.veritesting.RangerDiscovery.Util.DiscoveryUtil;
import gov.nasa.jpf.symbc.veritesting.VeritestingUtil.Pair;
import gov.nasa.jpf.symbc.veritesting.ast.def.Stmt;
import gov.nasa.jpf.symbc.veritesting.ast.transformations.Environment.DynamicRegion;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.AstMapVisitor;
import gov.nasa.jpf.symbc.veritesting.ast.visitors.ExprVisitor;
import jkind.lustre.NamedType;
import za.ac.sun.cs.green.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class PutDefaultValues extends AstMapVisitor {

    public PutDefaultValues(ExprVisitor<Expression> exprVisitor, List<Pair<String, NamedType>> subList) {
        super(exprVisitor);
        ((PutDefaultValuesExpr) (eva.theVisitor)).varList = firstAsList(subList);
    }



    public List<String> firstAsList(List<Pair<String, NamedType>> pairList) {
        List firstList = new ArrayList();
        for (Pair p : pairList) {
            firstList.add(p.getFirst());
        }
        return firstList;
    }

    public static DynamicRegion execute(List<Pair<String, NamedType>> subList) {
        PutDefaultValuesExpr putDefaultValuesExpr = new PutDefaultValuesExpr();
        Stmt dynStmtWithDefault = DiscoverContract.dynRegion.dynStmt.accept(new PutDefaultValues(putDefaultValuesExpr, subList));

        return new DynamicRegion(DiscoverContract.dynRegion, dynStmtWithDefault, DiscoverContract.dynRegion.spfCaseList, DiscoverContract.dynRegion.regionSummary, DiscoverContract.dynRegion.spfPredicateSummary, DiscoverContract.dynRegion.earlyReturnResult);
    }
}
