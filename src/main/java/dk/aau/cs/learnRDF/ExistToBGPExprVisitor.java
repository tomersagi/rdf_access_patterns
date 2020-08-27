package dk.aau.cs.learnRDF;

import org.apache.jena.sparql.algebra.op.Op0;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.expr.*;

import java.util.ArrayList;
import java.util.List;

public class ExistToBGPExprVisitor extends ExprVisitorBase {
    List<Op0> bgps = new ArrayList<>();
    @Override
    public void visit(ExprFunction1 func) {
        if (func.getArg().isFunction()) {
            ExistToBGPExprVisitor subVisitor = new ExistToBGPExprVisitor();
            func.getArg().visit(subVisitor);
            bgps.addAll(subVisitor.bgps);
        }

    }

    @Override
    public void visit(ExprFunction2 func) {
        for (Expr exp : func.getArgs()) {
            if (exp.isFunction()) {
                ExistToBGPExprVisitor subVisitor = new ExistToBGPExprVisitor();
                exp.visit(subVisitor);
                bgps.addAll(subVisitor.bgps);
            }
        }
    }

    @Override
    public void visit(ExprFunction3 func) {
        for (Expr exp : func.getArgs()) {
            if (exp.isFunction()) {
                ExistToBGPExprVisitor subVisitor = new ExistToBGPExprVisitor();
                exp.visit(subVisitor);
                bgps.addAll(subVisitor.bgps);
            }
        }
    }

    @Override
    public void visit(ExprFunctionN func) {
        for (Expr exp : func.getArgs()) {
            if (exp.isFunction()) {
                ExistToBGPExprVisitor subVisitor = new ExistToBGPExprVisitor();
                exp.visit(subVisitor);
                bgps.addAll(subVisitor.bgps);
            }
        }
    }

    @Override
    public void visit(ExprFunctionOp op) {
        if (op.getGraphPattern() instanceof OpBGP)
            this.bgps.add((OpBGP) op.getGraphPattern());
        else {
            if(op.getGraphPattern() instanceof OpFilter && ((OpFilter)op.getGraphPattern()).getSubOp() instanceof OpBGP)
                this.bgps.add((OpBGP) ((OpFilter)op.getGraphPattern()).getSubOp());
            else {
                if (op.getGraphPattern() instanceof OpPath) {
                    this.bgps.add((OpPath) op.getGraphPattern());
                    return;
                }
                if (!(op.getGraphPattern() instanceof OpFilter)){
                    throw new IllegalStateException("We have a unexpected operator! Expected `OpFilter` found " + op.getGraphPattern().getClass().getName() );
                }

                for (Expr exp : ((OpFilter)op.getGraphPattern()).getExprs()) {
                    ExistToBGPExprVisitor subVisitor = new ExistToBGPExprVisitor();
                    exp.visit(subVisitor);
                    bgps.addAll(subVisitor.bgps);
                }
            }
        }
    }

    /**
     *
     * @return list of OpBGP or OpPath (both extend Op0)
     */
    public List<Op0> getBGPs() {
        return this.bgps;
    }

}
