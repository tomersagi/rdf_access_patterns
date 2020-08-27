package dk.aau.cs.shacl.shapes;

import dk.aau.cs.shacl.Main;
import dk.aau.cs.shacl.utils.ConfigManager;
import dk.aau.cs.shacl.utils.RdfUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;

public class MetadataShapes {
    private List<Statement> metadataGraphShapeStatement = new ArrayList<>();

    public void annotate() {
        System.out.println("Annotating with Metadata Shape...");
        buildMetadataGraphShapeStatements();
        addMetadataStatementsToShapesModel();
    }

    private void buildMetadataGraphShapeStatements() {

        String shape = "RDFGraphShape";
        metadataGraphShapeStatement.add(ResourceFactory.createStatement(new ResourceImpl(Main.getShapesPrefixURL() + shape),
                RDF.type, new ResourceImpl("http://www.w3.org/ns/shacl#NodeShape")));

        String query = "SELECT DISTINCT ?p (COUNT(DISTINCT ?s) as ?distinctSubject) (COUNT(DISTINCT ?o) as ?distinctObject) WHERE { ?s ?p ?o} GROUP BY  ?p";

        ResultSet result = RdfUtils.runAQuery(query, Main.getRdfModelIRI());
        while (result.hasNext()) {
            QuerySolution row = result.next();

            if (row.get("p").asResource().getNameSpace().equals(ConfigManager.getProperty("datasetPrefixNameSpace"))) {
                metadataGraphShapeStatement.add(ResourceFactory.createStatement(
                        new ResourceImpl(Main.getShapesPrefixURL() + shape),
                        new PropertyImpl("http://www.w3.org/ns/shacl#property"),
                        new ResourceImpl(Main.getShapesPrefixURL() + row.get("p").asResource().getLocalName())));

                metadataGraphShapeStatement.add(ResourceFactory.createStatement(
                        new ResourceImpl(Main.getShapesPrefixURL() + row.get("p").asResource().getLocalName()),
                        RDF.type,
                        new ResourceImpl("http://www.w3.org/ns/shacl#PropertyShape")));

                metadataGraphShapeStatement.add(ResourceFactory.createStatement(
                        new ResourceImpl(Main.getShapesPrefixURL() + row.get("p").asResource().getLocalName()),
                        new PropertyImpl("http://www.w3.org/ns/shacl#nodeKind"),
                        new ResourceImpl("http://www.w3.org/ns/shacl#IRI")));

                metadataGraphShapeStatement.add(ResourceFactory.createStatement(
                        new ResourceImpl(Main.getShapesPrefixURL() + row.get("p").asResource().getLocalName()),
                        new PropertyImpl("http://www.w3.org/ns/shacl#path"),
                        row.get("p").asResource()
                ));

                metadataGraphShapeStatement.add(ResourceFactory.createStatement(
                        new ResourceImpl(Main.getShapesPrefixURL() + row.get("p").asResource().getLocalName()),
                        new PropertyImpl(Main.getShapesStatsPrefixURL() + "distinctSubjectCount"),
                        row.get("distinctSubject").asLiteral()
                ));

                metadataGraphShapeStatement.add(ResourceFactory.createStatement(
                        new ResourceImpl(Main.getShapesPrefixURL() + row.get("p").asResource().getLocalName()),
                        new PropertyImpl(Main.getShapesStatsPrefixURL() + "distinctObjectCount"),
                        row.get("distinctObject").asLiteral()
                ));
            }

        }

    }

    private void addMetadataStatementsToShapesModel() {
        Dataset dataset = RdfUtils.getTDBDataset();
        dataset.begin(ReadWrite.WRITE);
        Model graphModel = dataset.getNamedModel(Main.getShapesModelIRI());
        graphModel.add(metadataGraphShapeStatement);
        graphModel.close();
        dataset.commit();
        dataset.end();
        dataset.close();
    }
}
