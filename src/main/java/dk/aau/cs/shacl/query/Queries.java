package dk.aau.cs.shacl.query;

import java.util.ArrayList;
import java.util.List;

public class Queries {
    public static String linearQueryA = "PREFIX mo: <http://purl.org/ontology/mo/> PREFIX dc: <http://purl.org/dc/elements/1.1/> SELECT * WHERE { ?s a mo:MusicalWork ; dc:title  ?o . } LIMIT 10";;
    public static String linearQueryB = "PREFIX mo: <http://purl.org/ontology/mo/>\n" +
            "PREFIX mbz: <http://dbtune.org/musicbrainz/resource/vocab/> \n" +
            "PREFIX bio: <http://purl.org/vocab/bio/0.1/>\n" +
            "PREFIX cmno: <http://purl.org/ontology/classicalmusicnav#>\n" +
            "\n" +
            "SELECT ?a ?b ?c WHERE \n" +
            "{   \n" +
            "  ?a mbz:alias  \"Amy Beach\" .\n" +
            "  ?b cmno:hasInfluenced ?a .\n" +
            "  ?c mo:composer ?b . \n" +
            "  ?c bio:date ?d.\n" +
            "} LIMIT 10";


    public static String linearQueryCustom = "PREFIX mo: <http://purl.org/ontology/mo/>\n" +
            "PREFIX mbz: <http://dbtune.org/musicbrainz/resource/vocab/> \n" +
            "PREFIX bio: <http://purl.org/vocab/bio/0.1/>\n" +
            "PREFIX cmno: <http://purl.org/ontology/classicalmusicnav#>\n" +
            "\n" +
            "SELECT ?a ?b ?c WHERE \n" +
            "{   \n" +
            "  ?b cmno:hasInfluenced ?a .\n" +
            "  ?c mo:composer ?b . \n" +
            "  ?c bio:date ?d.\n" +
            "  ?a mbz:alias  \"Amy Beach\" .\n" +
            "} OFFSET 0 LIMIT 10";


    public static String linearQueryC = "PREFIX bio: <http://purl.org/vocab/bio/0.1/> SELECT * WHERE { ?a bio:place  \"Salzburg\" . } LIMIT 10";

    public static String linearQueryD = "PREFIX mo: <http://purl.org/ontology/mo/> \n " +
            "PREFIX bio: <http://purl.org/vocab/bio/0.1/> \n" +
            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
            "SELECT * WHERE {\n" +
            "?a foaf:name \"Albinoni, Tomaso Giovanni\" .\n" +
            "?b mo:composer ?a .\n" +
            "?c bio:date \"1880\"^^xsd:integer. }";

    public static String starQueryA = "PREFIX cmno: <http://purl.org/ontology/classicalmusicnav#> \n " +
            "PREFIX type: <http://dbtune.org/classical/resource/type/> \n" +
            "SELECT * WHERE {\n" +
            "?p1 cmno:hasInfluenced ?p2 .\n" +
            "?p3 cmno:hasInfluenced ?p1 . \n" +
            "?p1 a type:Composer . }" ;

    public static String starQueryB = "PREFIX cmno: <http://purl.org/ontology/classicalmusicnav#> \n " +
            "PREFIX type: <http://dbtune.org/classical/resource/type/> \n" +
            "SELECT * WHERE {\n" +
            "?p1 cmno:hasInfluenced ?p2 .\n" +
            "?p1 a type:Composer . }" ;

    public static String snowFlakeQuery = "PREFIX mo: <http://purl.org/ontology/mo/>\n" +
            "SELECT DISTINCT* WHERE {    \n" +
            "    ?p1 mo:composed_in ?p2 .  \n" +
            "    ?p1 a mo:MusicalWork .\n" +
            "  \t?p1 mo:composed_in ?p3.\n" +
            "  \t?p3 a mo:Composition . \n" +
            "  \t?p2 mo:produced_work ?p4 . \n" +
            "  \t?p4 a mo:MusicalWork .\n" +
            "}\n" +
            "LIMIT 10";
    public static String extremeCaseQuery = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX type: <http://dbtune.org/classical/resource/type/>\n" +
            "PREFIX bio: <http://purl.org/vocab/bio/0.1/>\n" +
            "select * where { \n" +
            "    ?c1 foaf:name ?name .\n" +
            "    ?c2 foaf:name ?name .\n" +
            "    ?c2 owl:sameAs ?c1 .\n" +
            "    ?c2 foaf:name \"John Massaro\".\n" +
            "    ?c1 a type:Pianist .\n" +
            "} limit 100 ";

    public static String selectAllQuery = "SELECT * WHERE { ?s ?p ?o . } ";

    private List<String> listOfQueries = new ArrayList<>();

    public Queries(){
        listOfQueries.add(selectAllQuery);
        //listOfQueries.add(extremeCaseQuery);
        //listOfQueries.add(snowFlakeQuery);
        //listOfQueries.add(linearQueryA);
        //listOfQueries.add(linearQueryB);
        listOfQueries.add(linearQueryC);
        //listOfQueries.add(linearQueryD);
        listOfQueries.add(starQueryA);
        listOfQueries.add(starQueryB);
    }

    public List<String> getListOfQueries() {
        return listOfQueries;
    }
}
