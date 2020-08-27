package dk.aau.cs.shacl.query;

import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.main.StageGenerator;

public class MyStageGenerator implements StageGenerator {
    public MyStageGenerator(StageGenerator orig) {
        System.out.println("Inside MyStageGenerator");
    }

    @Override
    public QueryIterator execute(BasicPattern pattern, QueryIterator input, ExecutionContext execCxt) {
        System.out.println(pattern.toString());
        System.out.println("Inside MyStageGenerator Query iterator");
//        EvaluateSPARQLQuery queryEvaluator = new EvaluateSPARQLQuery(Queries.snowFlakeQuery);
//
//        Op newOpIshere = queryEvaluator.useShapesToGenerateOptimizedAlgebra();
//        System.out.println("Printing new Op");
//        System.out.println(newOpIshere);
//        //System.out.println(Algebra.optimize(queryOp));
//
//        Dataset datasetNew = RdfUtils.getTDBDataset();
//        datasetNew.begin(ReadWrite.READ);
//        System.out.println(newOpIshere);
//        Model graphModelNew = datasetNew.getNamedModel(Main.getRdfModelIRI());
//        // ---- Execute expression
//        newOpIshere = Algebra.optimize(newOpIshere);
//
//        return Algebra.exec(newOpIshere, graphModelNew.getGraph()) ;

        return null;
    }
}
