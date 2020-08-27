package dk.aau.cs.shacl.query;

import org.apache.jena.graph.Triple;

public class TripleWithStats {
    public double getCardinality() {
        return cardinality;
    }

    double cardinality;

    public double getDistinctSubjects() {
        return distinctSubjects;
    }

    double distinctSubjects;

    public double getDistinctObjects() {
        return distinctObjects;
    }

    double distinctObjects;

    public Triple getTriple() {
        return triple;
    }

    Triple triple;

    @Override
    public String toString() {
        return "TripleWithStats{" +
                "cardinality=" + cardinality +
                ", distinctSubjects=" + distinctSubjects +
                ", distinctObjects=" + distinctObjects +
                ", triple=" + triple +
                '}';
    }

    public TripleWithStats(double cardinality, double distinctSubjects, double distinctObjects, Triple triple) {
        this.cardinality = cardinality;
        this.distinctSubjects = distinctSubjects;
        this.distinctObjects = distinctObjects;
        this.triple = triple;
    }
}
