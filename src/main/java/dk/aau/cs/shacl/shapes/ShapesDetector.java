package dk.aau.cs.shacl.shapes;

import dk.aau.cs.shacl.Main;
import dk.aau.cs.shacl.utils.RdfUtils;
import dk.aau.cs.shacl.utils.Tuple3;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.*;

/**
 * This class is to detect or find the relevant shapes for the bgps of the query
 */
public class ShapesDetector {
    private final String shapesURLPrefix = Main.getShapesPrefixURL();
    private final String model = Main.getRdfModelIRI();
    private ArrayList<HashSet<Triple>> bgps;
    private HashMap<Triple, List<Tuple3<String, String, String>>> bgpShapes;
    private Set<String> shapesHashSet = new HashSet<String>();

    public ShapesDetector(ArrayList<HashSet<Triple>> bgps) {
        this.bgps = bgps;
        findShapes();
    }

    public Set<String> getShapesHashSet() {
        return shapesHashSet;
    }

    public ShapesDetector(HashSet<Triple> triples) {
        bgpShapes = new HashMap<Triple, List<Tuple3<String, String, String>>>();
        getRelevantShapesForThisTriple(triples);
    }


    private void findShapes() {
        bgpShapes = new HashMap<Triple, List<Tuple3<String, String, String>>>();
        //shapesMapperSet = new HashMap<Triple, Set<String>>();


        for (HashSet<Triple> triples : bgps) {
            getRelevantShapesForThisTriple(triples);
        }

    }

    private void getRelevantShapesForThisTriple(HashSet<Triple> triples) {
        HashSet<Triple> ts = new HashSet<Triple>(triples);
        ts.forEach(triple -> {
            //System.out.println("Triple: " + triple.getSubject() + " - " + triple.getPredicate() + " - " + triple.getObject());
            List<Tuple3<String, String, String>> queryRelatives = new ArrayList<>();
            if (triple.getSubject().isURI()) {
                //System.out.println(triple.getSubject() + " is subject's URI");
                //if (!triple.getSubject().getURI().equals(RDF.type.getURI())) {
                //this.uriIsClass(triple.getSubject().getURI())
                if (this.uriIsClass(triple.getSubject().getURI())) {
                    queryRelatives.add(new Tuple3<>(
                            triple.getSubject().getURI(),
                            triple.getSubject().getLocalName(),
                            shapesURLPrefix + triple.getSubject().getLocalName() + "Shape")
                    );
                    shapesHashSet.add(shapesURLPrefix + triple.getSubject().getLocalName() + "Shape");
                }
                //this.uriClassBelongings(triple.getSubject().getURI());
                this.uriIsInstanceOfClass(triple.getSubject().getURI()).forEachRemaining(t -> {
                    queryRelatives.add(new Tuple3<>(
                            t.get("type").toString(),
                            t.getResource("type").getLocalName(),
                            shapesURLPrefix + t.getResource("type").getLocalName() + "Shape")
                    );
                    shapesHashSet.add(shapesURLPrefix + t.getResource("type").getLocalName() + "Shape");
                });
                //}
            }

            if (triple.getPredicate().isURI()) {
                //System.out.println(triple.getPredicate() + " is predicate's URI");

                if (!triple.getPredicate().getURI().equals(RDF.type.getURI())) {
                    //System.out.println(triple);
                    List<Resource> list = RdfUtils.getSubjectsFromModel("http://www.w3.org/ns/shacl#path", triple.getPredicate().toString(), Main.getShapesModelIRI());
                    list.forEach(t -> {

                        String nodeShapeLocalName = (RdfUtils.getSubjectsFromModel("http://www.w3.org/ns/shacl#property",t.toString(), Main.getShapesModelIRI()).get(0)).getLocalName();
                        //System.out.println(nodeShapeLocalName.split("Shape")[0]);
                        String query = "SELECT ?classIRI { <" + shapesURLPrefix  +nodeShapeLocalName + "> <http://www.w3.org/ns/shacl#targetClass> ?classIRI . FILTER (regex(str(?classIRI), \""+ nodeShapeLocalName.split("Shape")[0] + "\" )) }";
                        //System.out.println(targetClassOfShapeWithLocalName(query));

                        queryRelatives.add(new Tuple3<>(targetClassOfShapeWithLocalName(query), nodeShapeLocalName.split("Shape")[0], t.toString()));

                        //System.out.println(RdfUtils.getSubjectsFromModel("http://www.w3.org/ns/shacl#property",t.toString(), Main.getShapesModelIRI()));
                    });
                }
            }

            if (triple.getObject().isURI()) {
                //System.out.println(triple.getObject() + " is object's URI");
                //if (!triple.getObject().getURI().equals(RDF.type)) {

                //this.uriIsClass(triple.getObject().getURI());

                if (this.uriIsClass(triple.getObject().getURI())) {
                    queryRelatives.add(new Tuple3<>(
                            triple.getObject().getURI(),
                            triple.getObject().getLocalName(),
                            shapesURLPrefix + triple.getObject().getLocalName() + "Shape")
                    );
                    shapesHashSet.add(shapesURLPrefix + triple.getObject().getLocalName() + "Shape");
                }
                //this.uriClassBelongings(triple.getObject().getURI());
                //this.uriIsInstanceOfClass(triple.getObject().getURI());
                //}
            }
            //System.out.println(queryRelatives.toString());
            bgpShapes.put(triple, queryRelatives);
            //shapesMapperSet.put(triple, shapesHashSet);
        });
    }

    private boolean uriIsClass(String uri) {
        //ASK { ?s a ?type. FILTER EXISTS {?s a mo:MusicalWork .}}
        //SELECT DISTINCT ?type WHERE { ?s a ?type. FILTER EXISTS {?s a mo:MusicalWork .}}
        //System.out.println("Find out if URI is of Class");
        String q = "ASK { FILTER EXISTS {?s a <" + uri + "> . }}";
        //System.out.println(q);
        return RdfUtils.runAskQuery(q, model);
    }

    private ResultSet uriClassBelongings(String uri) {
        //SELECT DISTINCT ?type { ?s <URI> e.g. dc:title ?x . ?s a ?type .}
        //System.out.println("Find out class belongings of a URI");
        String q = "SELECT DISTINCT ?type { ?s <" + uri + "> ?x . ?s a ?type . }";
        return RdfUtils.runAQuery(q, model);
    }

    private ResultSet uriIsInstanceOfClass(String uri) {
        //System.out.println("URI is an instance of some class");
        String q = "SELECT ?type WHERE { <" + uri + "> a ?type . }";
        //System.out.println(q);
        return RdfUtils.runAQuery(q, model);
    }

    private String targetClassOfShapeWithLocalName(String query) {
        String result = "";
        ResultSet x = RdfUtils.runAQuery(query, Main.getShapesModelIRI());
        while(x.hasNext()){
            result = x.next().get("classIRI").toString();
        }
        return   result;
    }

    public HashMap<Triple, List<Tuple3<String, String, String>>> getBgpShapes() {
        return bgpShapes;
    }
}
