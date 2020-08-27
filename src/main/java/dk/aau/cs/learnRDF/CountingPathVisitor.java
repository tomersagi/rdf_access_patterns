package dk.aau.cs.learnRDF;

import org.apache.jena.sparql.path.*;

public class CountingPathVisitor extends PathVisitorBase{
        private int length=0;
        private boolean hasStar=false;

        @Override
        public void visit(P_Link pathNode) {
            length++;
        }

        @Override
        public void visit(P_ReverseLink pathNode) {
            length++;
        }

        @Override
        public void visit(P_NegPropSet pathNotOneOf) {
            //no op
        }

        @Override
        public void visit(P_Inverse inversePath) {
            CountingPathVisitor v = new CountingPathVisitor();
            inversePath.getSubPath().visit(v);
            length+=v.getLength();
            hasStar=(hasStar || v.hasStar);
        }

        @Override
        public void visit(P_Mod pathMod) {
            //no op
        }

        @Override
        public void visit(P_FixedLength pFixedLength) {
            length+=pFixedLength.getCount();
        }


        @Override
        public void visit(P_Multi pathMulti) {
            CountingPathVisitor v = new CountingPathVisitor();
            pathMulti.visit(v);
            length+=v.getLength();
            hasStar=(hasStar || v.hasStar);
        }

        @Override
        public void visit(P_Shortest pathShortest) {
            hasStar=true;
            //This can be considered * reachability, right?
        }

        @Override
        public void visit(P_ZeroOrOne path) {
            CountingPathVisitor v = new CountingPathVisitor();
            path.getSubPath().visit(v);
            length+=v.getLength();
            hasStar=(hasStar || v.hasStar);
        }

        @Override
        public void visit(P_ZeroOrMore1 path) {
            hasStar=true;
        }

        @Override
        public void visit(P_ZeroOrMoreN path) {
            hasStar=true;
        }

        @Override
        public void visit(P_OneOrMore1 path) {
            hasStar=true;
        }

        @Override
        public void visit(P_OneOrMoreN path) {
            hasStar=true;
        }

        @Override
        public void visit(P_Alt pathAlt) {
            CountingPathVisitor v1 = new CountingPathVisitor();
            pathAlt.getRight().visit(v1);
            CountingPathVisitor v2 = new CountingPathVisitor();
            pathAlt.getLeft().visit(v2);
            length+=Math.max(v1.getLength(),v2.getLength());
            hasStar=(hasStar || v1.hasStar || v2.hasStar);
        }

        @Override
        public void visit(P_Seq pathSeq) {
            CountingPathVisitor v = new CountingPathVisitor();
            pathSeq.getLeft().visit(v);
            pathSeq.getRight().visit(v);
            length+=v.getLength();
            hasStar=(hasStar || v.hasStar);
        }

    public int getLength() {
        return length;
    }

    public boolean isHasStar() {
        return hasStar;
    }
}
