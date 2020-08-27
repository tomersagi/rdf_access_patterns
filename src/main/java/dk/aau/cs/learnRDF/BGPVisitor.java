package dk.aau.cs.learnRDF;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import java.util.*;

public class BGPVisitor extends OpVisitorBase {

        protected Set<Triple> triples = new HashSet<>();
        protected Set<TriplePath> triplePaths = new HashSet<>();
        protected Set<Expr> filters = new HashSet<>();

    public List<BGPVisitor> getBgps() {
        return bgps;
    }

    protected final List<BGPVisitor> bgps =  new ArrayList<>();

    public Map<Var, NodeValue> getToBind() {
        return toBind;
    }

    protected Map<Var, NodeValue> toBind = new HashMap<>();

    public BGPVisitor() {super();}

        public Set<Triple> getTriples() {
                return triples;
        }

        @Override
        public void visit(OpUnion union) {
        BGPVisitor leftV = new BGPVisitor();
        BGPVisitor rightV = new BGPVisitor();
        union.getLeft().visit(leftV);
        union.getRight().visit(rightV);
        this.bgps.add(leftV);
        this.bgps.add(rightV);
        }

        @Override
        public void visit(OpLeftJoin leftjoin) {
                super.visit(leftjoin);
        }

        @Override
        public void visit(OpFilter filter) {
                this.filters.addAll(filter.getExprs().getList());
                for (Expr exp : filter.getExprs()) {
                    ExistToBGPExprVisitor visitor = new ExistToBGPExprVisitor();
                    exp.visit(visitor);
                    for (Op0 op : visitor.getBGPs()) {
                        BGPVisitor bgpv =  new BGPVisitor();
                        op.visit(bgpv);
                        this.bgps.add(bgpv);
                    }

                }

        }

        @Override
        public void visit(OpService service) {
                super.visit(service);
        }

        @Override
        public void visit(OpExtend opExtend)              {
            VarExprList l = opExtend.getVarExprList();
            if (l.size()==1)  //Only bind simple variable replacements
                for (Map.Entry<Var, Expr> entry : l.getExprs().entrySet())
                    if (entry.getValue().isConstant())
                        toBind.put(entry.getKey(), entry.getValue().getConstant());
        }


        @Override
        public void visit(OpJoin node) {
            super.visit(node);
        }

        @Override
        public void visit(OpPath opPath) {
            triplePaths.add(opPath.getTriplePath());
        }
        @Override
        public void visit(OpBGP node) {
            BasicPattern bp = node.getPattern();
            for (Triple t : bp) triples.add(t);
        }

    public static void main (String[] args) {

//        String queryIn = args[0];
//        try {
//            Query q = QueryFactory.read(queryIn);
//            Op op = (new AlgebraGenerator()).compile(q);
//            BGPVisitor bgpv = new BGPVisitor();
//            OpWalker.walk(op, bgpv);
//            ArrayList<BGP> bgps = bgpv.getBGPs();
//            for (BGP bgp : bgps) {
//                System.out.print(bgp);
//            }
//        } catch (org.apache.jena.query.QueryParseException e) {
//            System.out.println("error while reading "+queryIn);
//        }
    }

    public Set<TriplePath> getTriplePaths() {
        return triplePaths;
    }

    public Set<Expr> getFilters() {
            return this.filters;
    }
}

