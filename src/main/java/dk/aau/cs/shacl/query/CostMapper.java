package dk.aau.cs.shacl.query;

import java.util.HashMap;

public class CostMapper {


    public String decider(HashMap<String, Double> hashMap) {
        double cost, cost1, cost2;
        String order = "";

        // 4 stars
        if (hashMap.size() == 6) {
            cost = costComparator(hashMap.get("1.2"), hashMap.get("3.4"));
            order = func("1.2", hashMap.get("1.2"), "3.4", hashMap.get("3.4"));

            cost1 = costComparator(hashMap.get("1.3"), hashMap.get("2.4"));
            if (cost1 < cost) {
                cost = cost1;
                order = func("1.3", hashMap.get("1.3"), "2.4", hashMap.get("2.4"));
            }

            cost2 = costComparator(hashMap.get("2.3"), hashMap.get("1.4"));
            if (cost2 < cost) {
                //cost = cost2;
                order = func("2.3", hashMap.get("2.3"), "1.4", hashMap.get("1.4"));
            }
        }


        // 3 stars
        if (hashMap.size() == 3) {
            System.out.println("size 3");
            System.out.println(hashMap);
            cost = hashMap.get("1.2");
            order = "1.2.3";
            if (hashMap.get("1.3") < cost) {
                cost = hashMap.get("1.3");
                order = "1.3.2";
            }

            if (hashMap.get("2.3") < cost) {
                cost = hashMap.get("2.3");
                order = "2.3.1";
            }
        }

        return order;
    }

    private String func(String pairA, Double costPairA, String pairB, Double costPairB) {
        //this.globalCost = costPairA * costPairB;
        String order = "";
        if (costPairA > costPairB)
            order = pairB + "." + pairA;
        else
            order = pairA + "." + pairB;
        return order;
    }

    private static Double costComparator(Double v1, Double v2) {
        return v1 * v2;
    }


}
