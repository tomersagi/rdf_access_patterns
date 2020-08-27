package dk.aau.cs.shacl.query;

import dk.aau.cs.shacl.Main;
import dk.aau.cs.shacl.shapes.Statistics;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDF;

public class RanksCalculator {
    private double countDistinctSubjects = 0d;
    private double countDistinctObjects = 0d;

    public RanksCalculator(EvaluateSPARQLQuery evaluateSPARQLQuery) {
    }

    double getRankWhenObjIsNotVar(SubQuery sq, Triple triple, double rank) {
        // ?s    ?p   objA          -->4. c_t / c_o  ...  TotalCount / distinct
        // subjA ?p   objA          -->6. c_t / c_s . c_o
        if (triple.getPredicate().isVariable()) {

            if (triple.getSubject().isVariable()) {
                rank = (double) Main.getAllTriplesCount() / (double) Main.getDistinctObjectsCount();
            } else {
                rank = (double) Main.getAllTriplesCount() / ((double) Main.getDistinctSubjectsCount() * (double) Main.getDistinctObjectsCount());
            }
            countDistinctSubjects = Main.getDistinctSubjectsCount();
            countDistinctObjects = Main.getDistinctObjectsCount();
        }

        if (!triple.getPredicate().isVariable()) {
            // ?s     rdf:type objA         -->11. getTotalSubjectCount
            // subjA  rdf:type objA         -->12. Assumption here 1
            if (triple.getPredicate().getURI().equals(RDF.type.toString())) {
                if (triple.getSubject().isVariable()) {
                    /*for (Statistics stats : (sq.getShapes().get(triple))) {
                        //rank += stats.getSubjectCount().doubleValue();
                        System.out.println(stats.getSubjectCount().doubleValue());
                    }*/
                    if (sq.getShapes().get(triple).size() > 0)
                        rank = sq.getShapes().get(triple).stream().mapToDouble(Statistics::getSubjectCount).max().getAsDouble();
                    //rank = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getSubjectCount))).get().getSubjectCount();
                    countDistinctObjects = (Main.getDistinctRdfTypeObjectsCount());
                } else {
                    rank = 1;
                    countDistinctObjects = 1;
                }
                countDistinctSubjects = rank;

            } else {
                // ?s predA objA            -->7. getTotalSubjectCount/getDistinctCountPredicate
                // subjA predA objA         -->8. getPredicateCount/getSubjectCount
                if (triple.getSubject().isVariable()) {
                    double nom, den;
                    /*for (Statistics stats : (sq.getShapes().get(triple))) {
                        //nom += stats.getSubjectCount().doubleValue();
                        //den += stats.getDistinctCount().doubleValue();
                        //System.out.println( stats.toString());
                    }*/
                    nom = sq.getShapes().get(triple).stream().mapToDouble(Statistics::getSubjectCount).max().getAsDouble();
                    den = calculateDistinctObjectCount(sq, triple);
                    //nom = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getSubjectCount))).get().getSubjectCount();
                    //den = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getDistinctCount))).get().getDistinctCount();

                    countDistinctSubjects = calculateDistinctSubjectCount(sq, triple);
                    countDistinctObjects = den;
                    rank = nom / den;
                } else {
                    double nom = 0, den = 0;
                   /* for (Statistics stats : (sq.getShapes().get(triple))) {
                        nom += stats.getTotalCount().doubleValue();
                        den += stats.getSubjectCount().doubleValue();
                    }*/
                    nom = sq.getShapes().get(triple).stream().mapToDouble(Statistics::getTotalCount).max().getAsDouble();
                    den = sq.getShapes().get(triple).stream().mapToDouble(Statistics::getSubjectCount).max().getAsDouble();

                    countDistinctSubjects = calculateDistinctSubjectCount(sq, triple);
                    countDistinctObjects = calculateDistinctObjectCount(sq, triple);
                    //nom = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getTotalCount))).get().getTotalCount();
                    //den = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getSubjectCount))).get().getSubjectCount();
                    rank = nom / den;
                }
            }
        }
        return rank;
    }

    double getRankWhenObjIsVar(SubQuery sq, Triple triple, double rank) {
        // ?s    ?p   ?o          -->1. c_t
        // subjA ?p   ?o          -->2. c_t / c_s
        if (triple.getPredicate().isVariable()) {
            if (triple.getSubject().isVariable()) {
                rank = Main.getAllTriplesCount();
            } else {
                rank = (double) Main.getAllTriplesCount() / (double) Main.getDistinctSubjectsCount();
            }
            countDistinctSubjects = Main.getDistinctSubjectsCount();
            countDistinctObjects = Main.getDistinctObjectsCount();
        }

        if (!triple.getPredicate().isVariable()) {
            // ?s     rdf:type ?o         -->9. Count of rdf:type triples
            // subjA  rdf:type ?o         -->10. countRdfTypeTriples/ No. of subjects in the dataset with predicate rdf:type
            if (triple.getPredicate().getURI().equals(RDF.type.toString())) {
                if (triple.getSubject().isVariable()) {
                    rank = Main.getDistinctRdfTypeCount();
                } else {
                    rank = (double) Main.getDistinctRdfTypeCount() / Main.getDistinctRdfTypeSubjCount();
                    /*for (Statistics stats : (sq.getShapes().get(triple))) {
                        rank += stats.getSubjectCount()/ stats.getSubjectCount().doubleValue();
                    }*/
                }
                countDistinctSubjects = Main.getDistinctRdfTypeSubjCount();
                countDistinctObjects = Main.getDistinctRdfTypeObjectsCount();
            } else {
                // ?s predA ?o            -->3. No. of triples in the dataset with predA
                // subjA predA ?o         -->5. No. of subjects in the dataset with predA
                if (triple.getSubject().isVariable()) {
                    /*System.out.println(triple);
                    for (Statistics stats : (sq.getShapes().get(triple))) {
                        System.out.println(stats.toString());
                        //rank += stats.getTotalCount().doubleValue();
                    }
                    System.out.println();*/
                    rank = sq.getShapes().get(triple).stream().mapToDouble(Statistics::getTotalCount).max().getAsDouble();

                } else {
                    double nom = 0, den = 0;
                    //System.out.println("subjA predA ?o");
                    /*for (Statistics stats : (sq.getShapes().get(triple))) {
                        nom += stats.getSubjectCount().doubleValue();
                        den += stats.getDistinctCount().doubleValue();
                        //System.out.println("Nom: "+ nom + "  -  Den: " + den);
                    }*/
                    nom = sq.getShapes().get(triple).stream().mapToDouble(Statistics::getSubjectCount).max().getAsDouble();
                    den = calculateDistinctObjectCount(sq, triple);

                    //nom = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getSubjectCount))).get().getSubjectCount();
                    //den = (sq.getShapes().get(triple).stream().max(Comparator.comparing(Statistics::getDistinctCount))).get().getDistinctCount();

                    rank = nom / den;
                    //System.out.println("Rank " + rank);
                }
                countDistinctSubjects = calculateDistinctSubjectCount(sq, triple);
                countDistinctObjects = calculateDistinctObjectCount(sq, triple);
            }
        }
        return rank;
    }

    private double calculateDistinctSubjectCount(SubQuery sq, Triple triple) {
        return sq.getShapes().get(triple).stream().mapToDouble(Statistics::getDistinctSubjectCount).max().getAsDouble();
    }

    private double calculateDistinctObjectCount(SubQuery sq, Triple triple) {
        return sq.getShapes().get(triple).stream().mapToDouble(Statistics::getDistinctObjectCount).max().getAsDouble();
    }


    double getCountDistinctSubjects() {
        return countDistinctSubjects;
    }

    double getCountDistinctObjects() {
        return countDistinctObjects;
    }

}