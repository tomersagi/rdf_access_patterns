package dk.aau.cs.shacl.query;

import org.apache.jena.graph.Triple;

import java.util.List;

public class Star {
    public double starCardinality;
    public List<TripleWithStats> triples;

    public Star(double starCardinality, List<TripleWithStats> triples) {
        this.starCardinality = starCardinality;
        this.triples = triples;
    }

    @Override
    public String toString() {
        return "Star{" +
                "\n starCardinality=" + starCardinality +
                ", \n triples=" + triples +
                '}';
    }
}

