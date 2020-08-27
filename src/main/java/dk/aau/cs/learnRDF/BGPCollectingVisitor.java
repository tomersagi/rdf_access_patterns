package dk.aau.cs.learnRDF;

import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.op.OpBGP;

import java.util.ArrayList;
import java.util.List;

public class BGPCollectingVisitor extends OpVisitorBase {
    List<BGPVisitor> bgps = new ArrayList<>();
    @Override
    public void visit(OpBGP node) {
        BGPVisitor bgpv = new BGPVisitor();
        node.visit(bgpv);
        bgps.add(bgpv);
    }

}
