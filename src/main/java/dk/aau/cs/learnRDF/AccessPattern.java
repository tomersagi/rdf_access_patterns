package dk.aau.cs.learnRDF;

public enum AccessPattern {
    CONSTANTS_S("Constants","Subject"),
    CONSTANTS_P("Constants","Predicate"),
    CONSTANTS_O("Constants","Object"),
    CONSTANTS_SP("Constants","Subject and Predicate"),
    CONSTANTS_PO("Constants","Predicate and Object"),
    CONSTANTS_SO("Constants","Subject and Object"),
    CONSTANTS_SPO("Constants","Subject, Predicate, and Object"),
    RANGE_C("Range", "Closed"),
    RANGE_O("Range", "Open"),
    RANGE_S("Range", "Special"),
    TRAVERSAL_out1("Traversal", "1-hop s->o"),
    TRAVERSAL_in1("Traversal", "1-hop o->s"),
    TRAVERSAL_outK("Traversal", "K-hop s->o"), // K>2
    TRAVERSAL_inK("Traversal", "K-hop o->s"), // K>2
    TRAVERSAL_outP_STAR("Traversal", "P* s->o"),
    TRAVERSAL_inP_STAR("Traversal", "P* o->s"),
    TRAVERSAL_outSTAR("Traversal", "* s->o"),
    TRAVERSAL_inSTAR("Traversal", "* o->s"),
    RETURN_VAL("Return","All Values"),
    RETURN_DISTINCT("Return","Distinct Values"),
    RETURN_EXISTS("Return","Existence"),
    RETURN_AGG("Return","Aggregate"),
    PIVOT_S("Pivot","S"),
    PIVOT_OS("Pivot","OS"),
    PIVOT_O("Pivot","O"),
    PIVOT_OP("Pivot","OP"),
    PIVOT_SP("Pivot","SP"),
    WRITE_I("Write","Insert"),
    WRITE_D("Write","Delete")
    ;

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    private final String category;
    private final String type;

    AccessPattern(String category, String type) {
            this.category = category;
            this.type = type;
    }

    @Override
    public String toString() {
        return category + "," + type;
    }
}
