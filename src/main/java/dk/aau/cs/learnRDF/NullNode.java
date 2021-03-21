package dk.aau.cs.learnRDF;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.graph.Node_Concrete;

public class NullNode extends Node_Concrete {

    public NullNode() {
        super("null");

    }

    @Override
    public Object visitWith(NodeVisitor v) {
        return null;
    }

    @Override
    public boolean isConcrete() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NullNode;
    }
}
