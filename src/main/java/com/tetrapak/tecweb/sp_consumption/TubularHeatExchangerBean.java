/**
 *
 */
package main.java.com.tetrapak.tecweb.sp_consumption;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.exceptions.ClientException;

/**
 * @author SEPALMM
 *
 * Bean that controls the THE data
 */
@Named
@RequestScoped

public class TubularHeatExchangerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    CustomerSetBean customerSetBean;

    @Inject
    NeoDbProvider neoDbProvider;

    // ADD CLASS SPECIFIC MAPS AND FIELDS HERE
    private final int CONSUMPTION_HURDLE_YEAR = LocalDate.now().minusYears(3).getYear();
    private String cluster;
    private String marketGroup;
    private String market;
    private String customerGroup;
    private String[] customerNumbers;

    private String clientSelectionConstraints;

    private Map<String, List<Double>> cTypeFamilyMap;
    private Map<String, List<Double>> mtTypeFamilyMap;
    private Map<String, List<Double>> cMtTypeFamilyMap;

    private Double oRingHoldingCpotential;
    private Double oRingHoldingMTpotential;

    private Double oRingInsertNonRegCpotential;
    private Double oRingInsertRegCpotential;
    private Double oRingInsertNonRegMTpotential;
    private Double oRingInsertRegMTpotential;

    private Double oRingShellNonRegCpotential;
    private Double oRingShellRegCpotential;
    private Double oRingShellNonRegMTpotential;
    private Double oRingShellRegMTpotential;

    private Double prodSealCMTpotential;

    /**
     * Creates a new instance of TubularHeatExchangerBean
     */
    public TubularHeatExchangerBean() {
    }

    @PostConstruct
    public void init() {
        System.out.println("I'm in the 'TubularHeatExchangerBean.init()' method.");

        cluster = customerSetBean.getSelectedCluster();
        marketGroup = customerSetBean.getSelectedMarketGroup();
        market = customerSetBean.getSelectedMarket();
        customerGroup = customerSetBean.getSelectedCustGroup();
        customerNumbers = customerSetBean.getSelectedIDs();

        makeClientSelectionConstraints();

        List<Double> dataList = new ArrayList<>();
        dataList.add(0d);
        dataList.add(0d);
        dataList.add(0d);

        // INITIALIZE CLASS SPECIFIC MAPS AND FIELDS HERE
        // Initialize the 'O-rings for C-type models' family map
        cTypeFamilyMap = new LinkedHashMap<>();
        cTypeFamilyMap.put("O-ring insert, Non-regenerative", dataList);
        cTypeFamilyMap.put("O-ring shell, Non-regenerative", dataList);
        cTypeFamilyMap.put("O-ring insert, Regenerative", dataList);
        cTypeFamilyMap.put("O-ring shell, Regenerative", dataList);
        cTypeFamilyMap.put("O-ring holding", dataList);

        // Initialize the 'O-rings for MT-type models' family map
        mtTypeFamilyMap = new LinkedHashMap<>();
        // MT uses same type of o-ring for Non-regenerative as for Regenerative
        mtTypeFamilyMap.put("O-ring insert", dataList);
        mtTypeFamilyMap.put("O-ring shell", dataList);
        mtTypeFamilyMap.put("O-ring holding", dataList);

        // Initialize the 'Product seals for C & MT-type models' family map
        cMtTypeFamilyMap = new LinkedHashMap<>();
        cMtTypeFamilyMap.put("Product seal", dataList);

    }

    @PreDestroy
    public void destroyMe() {

//		neoDbProvider.closeNeo4jDriver();
//		System.out.println("Neo4jDriver in the TubularHeatExchangerBean have been disposed of.");
    }

    /**
     * This method puts together the sub-part of the Cypher query that defines
     * the client's search constraints of Cluster, Market group, Customer group
     * and Final customer number, e.g,:
     *
     * "c.id = 'E&CA' AND mg.name = 'NORDICS' AND cg.name = 'FALKOPING MEJERI'
     * AND e.id = '0000018140' AND "
     */
    private void makeClientSelectionConstraints() {
        String txCluster;
        String txMarketGroup;
        String txMarket;
        String txCustomerGroup;
        String txCustomerNumbers;
        boolean isAllCustNumbers = false;

        for (String id : customerNumbers) {
            if (id.equals("ALL CUSTOMER NUMBERS")) {
                isAllCustNumbers = true;
                break;
            }
        }

        if (cluster.equals("ALL CLUSTERS")) {
            txCluster = "";
        } else {
            txCluster = "c.id = $cluster AND ";
        }
        if (marketGroup.equals("ALL MARKET GROUPS")) {
            txMarketGroup = "";
        } else {
            txMarketGroup = "mg.name = $marketGroup AND ";
        }
        if (market.equals("ALL MARKETS")) {
            txMarket = "";
        } else {
            txMarket = "m.name = $market AND ";
        }
        if (customerGroup.equals("ALL CUSTOMER GROUPS")) {
            txCustomerGroup = "";
        } else {
            txCustomerGroup = "cg.name = $customerGroup AND ";
        }
        if (isAllCustNumbers) {
            txCustomerNumbers = "";
        } else {
            txCustomerNumbers = "e.id IN $customerNumbers AND ";
        }
        clientSelectionConstraints = txCluster + txMarketGroup + txMarket + txCustomerGroup + txCustomerNumbers;
//		 System.out.format("****************** THIS IS THE clientSelectionConstraints *******************\n%s\n ",
//		 clientSelectionConstraints);
    }

    /**
     * Constructs the Cypher query transaction text used in calculating the
     * total quantity of parts consumed for a specific parts family
     *
     * @param partFamilyName e.g., HOM_PISTON
     * @return the Cypher query transaction text
     */
    private String makeFamilyMapQueryStatement(String partFamilyName) {
        String tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)<-[r: ROUTE]-(:Part)-[:MEMBER_OF]->(pf: PartFamily) "
                + "MATCH (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                + "WHERE " + clientSelectionConstraints + " pf.name = '" + partFamilyName + "' "
                + "RETURN SUM(r.qty) AS TotalQty";

        // System.out.format("makeFamilyMapQueryStatement tx text: %s\n", tx);
        return tx;
    }

    /**
     * Constructs the Cypher query transaction text used in calculating the
     * total potential of O-Rings consumed for a specific equipment function
     *
     * @param functionLabelNameTube e.g., Ctube
     * @param functionLabelNameOring e.g., CoRing
     * @param tubeTypeQty e.g., holdingTubeQty
     * @param multiplier depending on tube type e.g., 2.0
     * @return the Cypher query transaction text
     */
    private String makeOringPotentialsQueryStatement(String functionLabelNameTube, String functionLabelNameOring,
            String tubeTypeQty, double multiplier) {

        String tx = "MATCH (f1: " + functionLabelNameTube + ")-[:IN]->(eq: Equipment)<-[:IN]-(f2: "
                + functionLabelNameOring + ")" + " MATCH (eq: Equipment)-[:IB_ROUTE]->(e :Entity)-[:IN]->(cg: CustGrp)"
                + " MATCH (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                + "WHERE " + clientSelectionConstraints + " eq.constructionYear <= " + CONSUMPTION_HURDLE_YEAR + " "
                + "WITH f1.id AS eqID1, f2.id AS eqID2, eq.runningHoursPA AS runHours, f1." + tubeTypeQty
                + " AS tubeQty, f2.serviceInterval AS serviceInterval "
                // Calculate O-Ring potential"
                + " WITH ((runHours / serviceInterval) * " + multiplier + " * tubeQty) AS Potential"
                + " RETURN SUM(Potential) AS Potential";

        // System.out.format("makeOringPotentialsQueryStatement tx text: %s\n",
        // tx);
        return tx;
    }

    // GETTERS HERE
    /**
     * @return the cTypeFamilyMap
     */
    public Map<String, List<Double>> getcTypeFamilyMap() {
        // System.out.println("I'm in the getcTypeFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            // Aggregate O-ring insert, Non-regenerative, consumption grouped
            // per customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("THE_O-RING INSERT (NON-REG)_C");

            StatementResult result = session.run(tx, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result.hasNext()) {
                Record r = result.next();

                // Add the calculated potential to the value list
                double tempPotential = getoRingInsertNonRegCpotential();

                valueList.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList.get(0) != 0d) {
                    // ratio = total / getoRingInsertNonRegCpotential();
                    ratio = total / tempPotential;
                }
                valueList
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("O-ring insert, Non-regenerative", valueList);

            }

            // *****************************************************************************************************
            // Aggregate O-ring shell, Non-regenerative, consumption grouped per
            // customer group
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("THE_O-RING SHELL (NON-REG)_C");

            StatementResult result1 = session.run(tx1, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result1.hasNext()) {
                Record r = result1.next();

                // Add the calculated potential to the value list
                double tempPotential1 = getoRingShellNonRegCpotential();
                valueList1.add(new BigDecimal(String.valueOf(tempPotential1)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList1
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList1.get(0) != 0d) {
                    ratio = total / tempPotential1;
                }
                valueList1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("O-ring shell, Non-regenerative", valueList1);

            }

            // *****************************************************************************************************
            // Aggregate O-ring insert, Regenerative, consumption grouped per
            // customer group
            List<Double> valueList2 = new ArrayList<>();

            String tx2 = makeFamilyMapQueryStatement("THE_O-RING INSERT (REG)_C");

            StatementResult result2 = session.run(tx2, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result2.hasNext()) {
                Record r = result2.next();

                // Add the calculated potential to the value list
                double tempPotential2 = getoRingInsertRegCpotential();
                valueList2.add(new BigDecimal(String.valueOf(tempPotential2)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList2
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList2.get(0) != 0d) {
                    ratio = total / tempPotential2;
                }
                valueList2
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("O-ring insert, Regenerative", valueList2);

            }

            // *****************************************************************************************************
            // Aggregate O-ring shell, Regenerative, consumption grouped per
            // customer group
            List<Double> valueList3 = new ArrayList<>();

            String tx3 = makeFamilyMapQueryStatement("THE_O-RING SHELL (REG)_C");

            StatementResult result3 = session.run(tx3, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result3.hasNext()) {
                Record r = result3.next();

                // Add the calculated potential to the value list
                double tempPotential3 = getoRingInsertRegCpotential();
                valueList3.add(new BigDecimal(String.valueOf(tempPotential3)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList3
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList3.get(0) != 0d) {
                    ratio = total / tempPotential3;
                }
                valueList3
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("O-ring shell, Regenerative", valueList3);

            }

            // *****************************************************************************************************
            // Aggregate O-ring holding, consumption grouped per customer group
            List<Double> valueList4 = new ArrayList<>();

            String tx4 = makeFamilyMapQueryStatement("THE_O-RING HOLDING_C");

            StatementResult result4 = session.run(tx4, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result4.hasNext()) {
                Record r = result4.next();

                // Add the calculated potential to the value list
                double tempPotential4 = getoRingHoldingCpotential();
                valueList4.add(new BigDecimal(String.valueOf(tempPotential4)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList4
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList4.get(0) != 0d) {
                    ratio = total / tempPotential4;
                }
                valueList4
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("O-ring holding", valueList4);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getcTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return cTypeFamilyMap;
    }

    /**
     * @return the mtTypeFamilyMap
     */
    public Map<String, List<Double>> getMtTypeFamilyMap() {
        // System.out.println("I'm in the getMtTypeFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {

            // Aggregate O-ring insert, consumption grouped per
            // customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("THE_O-RING INSERT_MT");

            StatementResult result = session.run(tx, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result.hasNext()) {
                Record r = result.next();

                // Add the calculated potential to the value list
                double potentialCombined = getoRingInsertNonRegMTpotential() + getoRingInsertRegMTpotential();
                valueList.add(new BigDecimal(String.valueOf(potentialCombined)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                // Add total consumption divided by 3 (to get annual
                // consumption
                // from 36 months sales history) to the value list
                double total = r.get("TotalQty").asDouble() / 3d;
                valueList
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList.get(0) != 0d) {
                    ratio = total / potentialCombined;
                }
                valueList
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                mtTypeFamilyMap.replace("O-ring insert", valueList);

            }

            // *****************************************************************************************************
            // Aggregate O-ring shell, consumption grouped per
            // customer group
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("THE_O-RING SHELL_MT");

            StatementResult result1 = session.run(tx1, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result1.hasNext()) {
                Record r = result1.next();

                // Add the calculated potential to the value list
                double potentialCombined1 = getoRingShellNonRegMTpotential() + getoRingShellRegMTpotential();
                valueList1.add(new BigDecimal(String.valueOf(potentialCombined1)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                // Add total consumption divided by 3 (to get annual consumption
                // from 36 months sales history) to the value list
                double total = r.get("TotalQty").asDouble() / 3d;
                valueList1
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList1.get(0) != 0d) {
                    ratio = total / potentialCombined1;
                }
                valueList1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                mtTypeFamilyMap.replace("O-ring shell", valueList1);

            }

            // *****************************************************************************************************
            // Aggregate O-ring holding, consumption grouped per customer group
            List<Double> valueList2 = new ArrayList<>();

            String tx2 = makeFamilyMapQueryStatement("THE_O-RING HOLDING_MT");

            StatementResult result2 = session.run(tx2, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result2.hasNext()) {
                Record r = result2.next();

                // Add the calculated potential to the value list
                double tempPotential2 = getoRingHoldingMTpotential();
                valueList2.add(new BigDecimal(String.valueOf(tempPotential2)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                // Add total consumption divided by 3 (to get annual consumption
                // from 36 months sales history) to the value list
                double total = r.get("TotalQty").asDouble() / 3d;
                valueList2
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList2.get(0) != 0d) {
                    ratio = total / tempPotential2;
                }
                valueList2
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                mtTypeFamilyMap.replace("O-ring holding", valueList2);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getMtTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return mtTypeFamilyMap;
    }

    /**
     * @return the cMtTypeFamilyMap
     */
    public Map<String, List<Double>> getcMtTypeFamilyMap() {
        // System.out.println("I'm in the cMtTypeFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            // Aggregate Product seal, consumption grouped
            // per customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("THE_PRODUCT SEAL_C");

            StatementResult result = session.run(tx, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result.hasNext()) {
                Record r = result.next();

                // Add the calculated potential to the value list
                double tempPotential = getProdSealCMTpotential();
                valueList.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueList
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cMtTypeFamilyMap.replace("Product seal", valueList);

            }

        } catch (ClientException e) {
            System.err.println("Exception in 'getcMtTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return cMtTypeFamilyMap;
    }

    /**
     * @return the oRingHoldingCpotential
     */
    public double getoRingHoldingCpotential() {
        if (oRingHoldingCpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("Ctube", "CoRing", "holdingTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingHoldingCpotential' is %s\n",
                // LocalDateTime.now(),tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingHoldingCpotential()':" + e);
            } finally {
            }
            oRingHoldingCpotential = tempPotential;
        }

        return oRingHoldingCpotential;
    }

    /**
     * @return the oRingHoldingMTpotential
     */
    public double getoRingHoldingMTpotential() {

        if (oRingHoldingMTpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("MTtube", "MToRing", "holdingTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingHoldingMTpotential' is %s\n",
                // LocalDateTime.now(),tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingHoldingMTpotential()':" + e);
            } finally {
            }
            oRingHoldingMTpotential = tempPotential;
        }

        return oRingHoldingMTpotential;
    }

    /**
     * @return the oRingInsertNonRegCpotential
     */
    public double getoRingInsertNonRegCpotential() {
        if (oRingInsertNonRegCpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("Ctube", "CoRing", "nonRegTubeQty", 4.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingInsertNonRegCpotential' is %s\n",LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingInsertNonRegCpotential()':" + e);
            } finally {
            }
            oRingInsertNonRegCpotential = tempPotential;
        }
        return oRingInsertNonRegCpotential;
    }

    /**
     * @return the oRingInsertRegCpotential
     */
    public double getoRingInsertRegCpotential() {

        if (oRingInsertRegCpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("Ctube", "CoRing", "regTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingInsertRegCpotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingInsertRegCpotential()':" + e);
            } finally {
            }
            oRingInsertRegCpotential = tempPotential;
        }

        return oRingInsertRegCpotential;
    }

    /**
     * @return the oRingInsertNonRegMTpotential
     */
    public double getoRingInsertNonRegMTpotential() {
        if (oRingInsertNonRegMTpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("MTtube", "MToRing", "nonRegTubeQty", 4.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingInsertNonRegMTpotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingInsertNonRegMTpotential()':" + e);
            } finally {
            }
            oRingInsertNonRegMTpotential = tempPotential;
        }
        return oRingInsertNonRegMTpotential;
    }

    /**
     * @return the oRingInsertRegMTpotential
     */
    public double getoRingInsertRegMTpotential() {

        if (oRingInsertRegMTpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("MTtube", "MToRing", "regTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingInsertRegMTpotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingInsertRegMTpotential()':" + e);
            } finally {
            }
            oRingInsertRegMTpotential = tempPotential;
        }

        return oRingInsertRegMTpotential;
    }

    /**
     * @return the oRingShellNonRegCpotential
     */
    public double getoRingShellNonRegCpotential() {

        if (oRingShellNonRegCpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("Ctube", "CoRing", "nonRegTubeQty", 4.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingShellNonRegCpotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingShellNonRegCpotential()':" + e);
            } finally {
            }
            oRingShellNonRegCpotential = tempPotential;
        }

        return oRingShellNonRegCpotential;
    }

    /**
     * @return the oRingShellRegCpotential
     */
    public double getoRingShellRegCpotential() {

        if (oRingShellRegCpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("Ctube", "CoRing", "regTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingShellRegCpotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingShellRegCpotential()':" + e);
            } finally {
            }
            oRingShellRegCpotential = tempPotential;
        }

        return oRingShellRegCpotential;
    }

    /**
     * @return the oRingShellNonRegMTpotential
     */
    public double getoRingShellNonRegMTpotential() {

        if (oRingShellNonRegMTpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("MTtube", "MToRing", "nonRegTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // tf("%s > Queried Potential for 'oRingShellNonRegCpotential'
                // is %s\n", LocalDateTime.now(), tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingShellNonRegMTpotential()':" + e);
            } finally {
            }
            oRingShellNonRegMTpotential = tempPotential;
        }
        return oRingShellNonRegMTpotential;
    }

    /**
     * @return the oRingShellRegMTpotential
     */
    public double getoRingShellRegMTpotential() {

        if (oRingShellRegMTpotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeOringPotentialsQueryStatement("MTtube", "MToRing", "regTubeQty", 2.0);

                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                }

                // System.out.printf("%s > Queried Potential for
                // 'oRingShellRegMTpotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getoRingShellRegMTpotential()':" + e);
            } finally {
            }
            oRingShellRegMTpotential = tempPotential;
        }

        return oRingShellRegMTpotential;
    }

    /**
     * @return the prodSealCMTpotential
     */
    public double getProdSealCMTpotential() {

        if (prodSealCMTpotential == null) {

            // code query here
            double tempPotential = 0d;
            double accPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = "MATCH (f10: Ctube)-[:IN]->(eq10: Equipment)<-[:IN]-(f20: Seal) "
                        + "MATCH (eq10: Equipment)-[:IB_ROUTE]->(e :Entity)-[:IN]->(cg: CustGrp) "
                        + "MATCH (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                        + "WHERE " + clientSelectionConstraints + " eq10.constructionYear <= " + CONSUMPTION_HURDLE_YEAR
                        + " "
                        + "WITH f10.id AS eqID10, f20.id AS eqID20, eq10.runningHoursPA AS runHours, (f10.nonRegTubeQty + f10.regTubeQty  + f10.holdingTubeQty) AS tubeQty, f20.serviceInterval AS serviceInterval "
                        // Calculate potential ProdSealC
                        + "WITH ((runHours / serviceInterval) * 2.0 * tubeQty) AS Potential "
                        + "RETURN SUM(Potential) AS Potential "
                        + "UNION "
                        + "MATCH (f15: MTtube)-[:IN]->(eq15: Equipment)<-[:IN]-(f25: Seal) "
                        + "MATCH (eq15: Equipment)-[:IB_ROUTE]->(e :Entity)-[:IN]->(cg: CustGrp) "
                        + "MATCH (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                        + "WHERE " + clientSelectionConstraints + " eq15.constructionYear <= " + CONSUMPTION_HURDLE_YEAR
                        + " "
                        + "WITH f15.id AS eqID15, f25.id AS eqID25, eq15.runningHoursPA AS runHours, (f15.nonRegTubeQty + f15.regTubeQty  + f15.holdingTubeQty) AS tubeQty, f25.serviceInterval AS serviceInterval "
                        // Calculate potential ProdSealMT
                        + "WITH ((runHours / serviceInterval) * 2.0 * tubeQty) AS Potential "
                        + "RETURN SUM(Potential) AS Potential";

                // System.out.format("getProdSealCMTpotential tx text: %s\n",
                // tx);
                StatementResult result = session.run(tx, Values.parameters(
                        "cluster", cluster,
                        "marketGroup", marketGroup,
                        "market", market,
                        "customerGroup", customerGroup,
                        "customerNumbers", customerNumbers
                ));

                while (result.hasNext()) {
                    Record r = result.next();
                    tempPotential = r.get("Potential").asDouble();
                    accPotential = accPotential + tempPotential;

                }
                // System.out.printf("%s > Queried Potential for
                // 'prodSealCMTpotential' is %s\n", LocalDateTime.now(),
                // accPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getProdSealCMTpotential()':" + e);
            } finally {
            }
            prodSealCMTpotential = accPotential;
        }
        return prodSealCMTpotential;
    }

}
