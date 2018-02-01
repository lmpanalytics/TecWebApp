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
 * Bean that controls the PHE data
 */
@Named
@RequestScoped

public class PlateHeatExchangerBean implements Serializable {

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
    private Map<String, List<Double>> mTypeFamilyMap;
    private Map<String, List<Double>> hTypeFamilyMap;
    private Map<String, List<Double>> pTypeFamilyMap;

    private Double cPlatePotential;
    private Double mPlatePotential;
    private Double hPlatePotential;
    private Double pPlatePotential;

    private Double cGasketPotential;
    private Double mGasketPotential;
    private Double hGasketPotential;
    private Double pGasketPotential;

    /**
     * Creates a new instance of PlateHeatExchangerBean
     */
    public PlateHeatExchangerBean() {
    }

    @PostConstruct
    public void init() {
        System.out.println("I'm in the 'PlateHeatExchangerBean.init()' method.");

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
        // Initialize the C-type family map
        cTypeFamilyMap = new LinkedHashMap<>();
        cTypeFamilyMap.put("C-Gasket", dataList);
        cTypeFamilyMap.put("C-Plate", dataList);

        // Initialize the M-type family map
        mTypeFamilyMap = new LinkedHashMap<>();
        mTypeFamilyMap.put("M-Gasket", dataList);
        mTypeFamilyMap.put("M-Plate", dataList);

        // Initialize the H-type family map
        hTypeFamilyMap = new LinkedHashMap<>();
        hTypeFamilyMap.put("H-Gasket", dataList);
        hTypeFamilyMap.put("H-Plate", dataList);

        // Initialize the P-type family map
        pTypeFamilyMap = new LinkedHashMap<>();
        pTypeFamilyMap.put("P-Gasket", dataList);
        pTypeFamilyMap.put("P-Plate", dataList);

    }

    @PreDestroy
    public void destroyMe() {

//		neoDbProvider.closeNeo4jDriver();
//		System.out.println("Neo4jDriver in the PlateHeatExchangerBean have been disposed of.");
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
        return tx;
    }

    /**
     * Constructs the Cypher query transaction text used in calculating the
     * total potential of plates consumed for a specific equipment function
     *
     * @param functionLabelName e.g., Piston
     * @return the Cypher query transaction text
     */
    private String makePlatePotentialsQueryStatement(String functionLabelName) {
        String tx = "MATCH (f1: " + functionLabelName
                + ")-[r: IN]->(eq: Equipment)-[:IB_ROUTE]->(e :Entity)-[:IN]->(cg: CustGrp) "
                + "MATCH q = (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                + "WHERE " + clientSelectionConstraints + " eq.constructionYear <= " + CONSUMPTION_HURDLE_YEAR + " "
                + "WITH f1.id AS eqID1, eq.runningHoursPA AS runHours, f1.serviceInterval AS serviceInterval, f1.plateQty AS plateQty "
                // Calculate Potential
                + "WITH ((runHours / serviceInterval) * plateQty) AS Potential RETURN SUM(Potential) AS Potential";

        return tx;
    }

    /**
     * Constructs the Cypher query transaction text used in calculating the
     * total potential of gaskets consumed for a specific equipment function
     *
     * @param functionLabelName e.g., Piston
     * @return the Cypher query transaction text
     */
    private String makeGasketPotentialsQueryStatement(String functionLabelName) {
        String tx = "MATCH (f1: " + functionLabelName
                + ")-[r: IN]->(eq: Equipment)-[:IB_ROUTE]->(e :Entity)-[:IN]->(cg: CustGrp) "
                + "MATCH q = (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                + "WHERE " + clientSelectionConstraints + " eq.constructionYear <= " + CONSUMPTION_HURDLE_YEAR + " "
                + "WITH f1.id AS eqID1, eq.runningHoursPA AS runHours, f1.serviceInterval AS serviceInterval, f1.gasketQty AS gasketQty "
                // Calculate Potential
                + "WITH ((runHours / serviceInterval) * gasketQty) AS Potential RETURN SUM(Potential) AS Potential";

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
            // Aggregate C-Gasket consumption grouped per customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("PHE_C-GASKET");

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
                double tempPotential = getcGasketPotential();
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
                cTypeFamilyMap.replace("C-Gasket", valueList);

            }

            // *****************************************************************************************************
            // Aggregate C-Plate consumption grouped per customer group
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("PHE_C-PLATE");

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
                double tempPotential = getcPlatePotential();
                valueList1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("C-Plate", valueList1);

            }

            // *************************PROCESS_PLATE_WITH_GASKET*************************
            // Aggregate C-Gasket consumption grouped per customer group
            List<Double> valueListPWG = new ArrayList<>();
            List<Double> valueListCombined = new ArrayList<>();

            String txPWG = makeFamilyMapQueryStatement("PHE_C-PLATE_WITH_GASKET");

            StatementResult resultPWG = session.run(txPWG, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG.hasNext()) {
                Record r = resultPWG.next();

                // Add the calculated potential to the value list
                double tempPotential = getcGasketPotential();
                valueListPWG.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList.size(); i++) {
                    double d = valueList.get(i);
                    double dPWG = valueListPWG.get(i);
                    // System.out.printf("cGasket %s, cGasket
                    // FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("C-Gasket", valueListCombined);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *****************************************************************************************************
            // Aggregate C-Plate consumption grouped per customer group
            List<Double> valueListPWG1 = new ArrayList<>();
            List<Double> valueListCombined1 = new ArrayList<>();

            String txPWG1 = makeFamilyMapQueryStatement("PHE_C-PLATE_WITH_GASKET");

            StatementResult resultPWG1 = session.run(txPWG1, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG1.hasNext()) {
                Record r = resultPWG1.next();

                // Add the calculated potential to the value list
                double tempPotential = getcPlatePotential();
                valueListPWG1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG1
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG1.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList1.size(); i++) {
                    double d = valueList1.get(i);
                    double dPWG = valueListPWG1.get(i);
                    // System.out.printf("cPlate %s, cPlate FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                cTypeFamilyMap.replace("C-Plate", valueListCombined1);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *******************************************END_PWG*****************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getcTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return cTypeFamilyMap;
    }

    /**
     * @return the mTypeFamilyMap
     */
    public Map<String, List<Double>> getmTypeFamilyMap() {
        // System.out.println("I'm in the getmTypeFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            // Aggregate M-Gasket consumption grouped per customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("PHE_M-GASKET");

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
                double tempPotential = getmGasketPotential();
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
                mTypeFamilyMap.replace("M-Gasket", valueList);

            }

            // *****************************************************************************************************
            // Aggregate M-Plate consumption grouped per customer group
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("PHE_M-PLATE");

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
                double tempPotential = getmPlatePotential();
                valueList1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                mTypeFamilyMap.replace("M-Plate", valueList1);

            }

            // *************************PROCESS_PLATE_WITH_GASKET*************************
            // Aggregate M-Gasket consumption grouped per customer group
            List<Double> valueListPWG = new ArrayList<>();
            List<Double> valueListCombined = new ArrayList<>();

            String txPWG = makeFamilyMapQueryStatement("PHE_M-PLATE_WITH_GASKET");

            StatementResult resultPWG = session.run(txPWG, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG.hasNext()) {
                Record r = resultPWG.next();

                // Add the calculated potential to the value list
                double tempPotential = getmGasketPotential();
                valueListPWG.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList.size(); i++) {
                    double d = valueList.get(i);
                    double dPWG = valueListPWG.get(i);
                    // System.out.printf("mGasket %s, mGasket
                    // FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                mTypeFamilyMap.replace("M-Gasket", valueListCombined);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *****************************************************************************************************
            // Aggregate M-Plate consumption grouped per customer group
            List<Double> valueListPWG1 = new ArrayList<>();
            List<Double> valueListCombined1 = new ArrayList<>();

            String txPWG1 = makeFamilyMapQueryStatement("PHE_M-PLATE_WITH_GASKET");

            StatementResult resultPWG1 = session.run(txPWG1, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG1.hasNext()) {
                Record r = resultPWG1.next();

                // Add the calculated potential to the value list
                double tempPotential = getmPlatePotential();
                valueListPWG1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG1
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG1.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList1.size(); i++) {
                    double d = valueList1.get(i);
                    double dPWG = valueListPWG1.get(i);
                    // System.out.printf("mPlate %s, mPlate FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                mTypeFamilyMap.replace("M-Plate", valueListCombined1);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *******************************************END_PWG*****************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getmTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return mTypeFamilyMap;
    }

    /**
     * @return the hTypeFamilyMap
     */
    public Map<String, List<Double>> gethTypeFamilyMap() {
        // System.out.println("I'm in the gethTypeFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            // Aggregate H-Gasket consumption grouped per customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("PHE_H-GASKET");

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
                double tempPotential = gethGasketPotential();
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
                hTypeFamilyMap.replace("H-Gasket", valueList);

            }

            // *****************************************************************************************************
            // Aggregate H-Plate consumption grouped per customer group
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("PHE_H-PLATE");

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
                double tempPotential = gethPlatePotential();
                valueList1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                hTypeFamilyMap.replace("H-Plate", valueList1);

            }

            // *************************PROCESS_PLATE_WITH_GASKET*************************
            // Aggregate H-Gasket consumption grouped per customer group
            List<Double> valueListPWG = new ArrayList<>();
            List<Double> valueListCombined = new ArrayList<>();

            String txPWG = makeFamilyMapQueryStatement("PHE_H-PLATE_WITH_GASKET");

            StatementResult resultPWG = session.run(txPWG, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG.hasNext()) {
                Record r = resultPWG.next();

                // Add the calculated potential to the value list
                double tempPotential = gethGasketPotential();
                valueListPWG.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList.size(); i++) {
                    double d = valueList.get(i);
                    double dPWG = valueListPWG.get(i);
                    // System.out.printf("hGasket %s, hGasket
                    // FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                hTypeFamilyMap.replace("H-Gasket", valueListCombined);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *****************************************************************************************************
            // Aggregate H-Plate consumption grouped per customer group
            List<Double> valueListPWG1 = new ArrayList<>();
            List<Double> valueListCombined1 = new ArrayList<>();

            String txPWG1 = makeFamilyMapQueryStatement("PHE_H-PLATE_WITH_GASKET");

            StatementResult resultPWG1 = session.run(txPWG1, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG1.hasNext()) {
                Record r = resultPWG1.next();

                // Add the calculated potential to the value list
                double tempPotential = gethPlatePotential();
                valueListPWG1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG1
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG1.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList1.size(); i++) {
                    double d = valueList1.get(i);
                    double dPWG = valueListPWG1.get(i);
                    // System.out.printf("hPlate %s, hPlate FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                hTypeFamilyMap.replace("H-Plate", valueListCombined1);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *******************************************END_PWG*****************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'gethTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return hTypeFamilyMap;
    }

    /**
     * @return the pTypeFamilyMap
     */
    public Map<String, List<Double>> getpTypeFamilyMap() {
        // System.out.println("I'm in the getpTypeFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            // Aggregate P-Gasket consumption grouped per customer group
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("PHE_P-GASKET");

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
                double tempPotential = getpGasketPotential();
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
                pTypeFamilyMap.replace("P-Gasket", valueList);

            }

            // *****************************************************************************************************
            // Aggregate P-Plate consumption grouped per customer group
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("PHE_P-PLATE");

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
                double tempPotential = getpPlatePotential();
                valueList1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pTypeFamilyMap.replace("P-Plate", valueList1);

            }

            // *************************PROCESS_PLATE_WITH_GASKET*************************
            // Aggregate P-Gasket consumption grouped per customer group
            List<Double> valueListPWG = new ArrayList<>();
            List<Double> valueListCombined = new ArrayList<>();

            String txPWG = makeFamilyMapQueryStatement("PHE_P-PLATE_WITH_GASKET");

            StatementResult resultPWG = session.run(txPWG, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG.hasNext()) {
                Record r = resultPWG.next();

                // Add the calculated potential to the value list
                double tempPotential = getpGasketPotential();
                valueListPWG.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList.size(); i++) {
                    double d = valueList.get(i);
                    double dPWG = valueListPWG.get(i);
                    // System.out.printf("pGasket %s, pGasket
                    // FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pTypeFamilyMap.replace("P-Gasket", valueListCombined);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *****************************************************************************************************
            // Aggregate P-Plate consumption grouped per customer group
            List<Double> valueListPWG1 = new ArrayList<>();
            List<Double> valueListCombined1 = new ArrayList<>();

            String txPWG1 = makeFamilyMapQueryStatement("PHE_P-PLATE_WITH_GASKET");

            StatementResult resultPWG1 = session.run(txPWG1, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (resultPWG1.hasNext()) {
                Record r = resultPWG1.next();

                // Add the calculated potential to the value list
                double tempPotential = getpPlatePotential();
                valueListPWG1.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueListPWG1
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueListPWG1.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueListPWG1
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());

                // ******* CODE BLOCK to merge potentials and sales *******
                double calcPot = 0d;
                double calcSales = 0d;
                double calcRatio = 0d;
                for (int i = 0; i < valueList1.size(); i++) {
                    double d = valueList1.get(i);
                    double dPWG = valueListPWG1.get(i);
                    // System.out.printf("pPlate %s, pPlate FromPlateWGasket
                    // %s", d, dPWG);
                    // Calculate the potential
                    if (i == 0) {
                        // if potentials are zero or the same, then use the
                        // 'd'
                        // potential
                        if (d == dPWG) {
                            calcPot = d;
                        } else {
                            calcPot = d + dPWG;
                        }
                        // Calculate actual sales by adding them together
                    } else if (i == 1) {
                        calcSales = d + dPWG;
                    }
                }

                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (calcPot != 0d) {
                    calcRatio = calcSales / calcPot;
                }

                // Add to combined list
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcPot)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcSales)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                valueListCombined1.add(
                        new BigDecimal(String.valueOf(calcRatio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pTypeFamilyMap.replace("P-Plate", valueListCombined1);

                // System.out.printf("Potential is %s, sales is %s, ratio is
                // %s", calcPot, calcSales, calcRatio);
                // ******* CODE BLOCK END *******
            }

            // *******************************************END_PWG*****************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getpTypeFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return pTypeFamilyMap;
    }

    /**
     * @return the cPlatePotential
     */
    public double getcPlatePotential() {

        if (cPlatePotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePlatePotentialsQueryStatement("CPlate");

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
                // 'cPlatePotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getcPlatePotential()':" + e);
            } finally {
            }
            cPlatePotential = tempPotential;
        }
        return cPlatePotential;
    }

    /**
     * @return the mPlatePotential
     */
    public double getmPlatePotential() {

        if (mPlatePotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePlatePotentialsQueryStatement("MPlate");

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
                // 'mPlatePotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getmPlatePotential()':" + e);
            } finally {
            }
            mPlatePotential = tempPotential;
        }
        return mPlatePotential;
    }

    /**
     * @return the hPlatePotential
     */
    public double gethPlatePotential() {

        if (hPlatePotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePlatePotentialsQueryStatement("HPlate");

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
                // 'hPlatePotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'gethPlatePotential()':" + e);
            } finally {
            }
            hPlatePotential = tempPotential;
        }
        return hPlatePotential;
    }

    /**
     * @return the pPlatePotential
     */
    public double getpPlatePotential() {

        if (pPlatePotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePlatePotentialsQueryStatement("PPlate");

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
                // 'pPlatePotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getpPlatePotential()':" + e);
            } finally {
            }
            pPlatePotential = tempPotential;
        }
        return pPlatePotential;
    }

    /**
     * @return the cGasketPotential
     */
    public double getcGasketPotential() {

        if (cGasketPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeGasketPotentialsQueryStatement("CGasket");

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
                // 'cGasketPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getcGasketPotential()':" + e);
            } finally {
            }
            cGasketPotential = tempPotential;
        }
        return cGasketPotential;
    }

    /**
     * @return the mGasketPotential
     */
    public double getmGasketPotential() {

        if (mGasketPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeGasketPotentialsQueryStatement("MGasket");

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
                // 'mGasketPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getmGasketPotential()':" + e);
            } finally {
            }
            mGasketPotential = tempPotential;
        }
        return mGasketPotential;
    }

    /**
     * @return the hGasketPotential
     */
    public double gethGasketPotential() {

        if (hGasketPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makeGasketPotentialsQueryStatement("HGasket");

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
                // 'hGasketPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'gethGasketPotential()':" + e);
            } finally {
            }
            hGasketPotential = tempPotential;
        }
        return hGasketPotential;
    }

    /**
     * @return the pGasketPotential
     */
    public double getpGasketPotential() {

        if (pGasketPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {
                ;

                String tx = makeGasketPotentialsQueryStatement("PGasket");

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
                // 'cGasketPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getpGasketPotential()':" + e);
            } finally {
            }
            pGasketPotential = tempPotential;
        }
        return pGasketPotential;
    }

}
