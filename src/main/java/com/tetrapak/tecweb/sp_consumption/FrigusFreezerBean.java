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
 * Bean that controls the FrigusFreezer data
 */
@Named
@RequestScoped

public class FrigusFreezerBean implements Serializable {

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

    private Map<String, List<Double>> serviceKitsFamilyMap;
    private Map<String, List<Double>> pumpFamilyMap;
    private Map<String, List<Double>> pumpSparePartsFamilyMap;
    private Map<String, List<Double>> scraperFamilyMap;
    private Map<String, List<Double>> coolingFamilyMap;
    private Map<String, List<Double>> shaftFamilyMap;
    private Map<String, List<Double>> cylinderFamilyMap;
    private Map<String, List<Double>> dasherFamilyMap;

    private Double kit1000Potential;
    private Double kit3000Potential;
    private Double kit6000Potential;
    private Double kit12000Potential;

    private Double newPumpPotential;
    private Double exchangePumpPotential;

    private Double kitExtendedPotential;
    private Double starWheelPotential;
    private Double coverPotential;
    private Double impellerPotential;

    private Double gasketKitPotential;
    private Double bushingPotential;

    private Double scraperKnifePotential;
    private Double sparePartsPotential;
    private Double shaftSealPotential;

    private Double cylKitPotential;
    private Double gasketPotential;

    private Double dasherKitPotential;
    private Double singlePartPotential;

    /**
     * Creates a new instance of FrigusFreezerBean
     */
    public FrigusFreezerBean() {
    }

    @PostConstruct
    public void init() {
        System.out.println("I'm in the 'FrigusFreezerBean.init()' method.");

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
        // Initialize the 'Service kits' family map
        serviceKitsFamilyMap = new LinkedHashMap<>();
        serviceKitsFamilyMap.put("1000 service kit", dataList);
        serviceKitsFamilyMap.put("3000 service kit", dataList);
        serviceKitsFamilyMap.put("6000 service kit", dataList);
        serviceKitsFamilyMap.put("12000 service kit", dataList);

        // Initialize the 'Pump' family map
        pumpFamilyMap = new LinkedHashMap<>();
        pumpFamilyMap.put("New pump", dataList);
        pumpFamilyMap.put("Exchange pump", dataList);

        // Initialize the 'Pump spare parts' family map
        pumpSparePartsFamilyMap = new LinkedHashMap<>();
        pumpSparePartsFamilyMap.put("Kit extended", dataList);
        pumpSparePartsFamilyMap.put("Star wheel", dataList);
        pumpSparePartsFamilyMap.put("Cover", dataList);
        pumpSparePartsFamilyMap.put("Impeller", dataList);

        pumpSparePartsFamilyMap.put("Gasket kit", dataList);
        pumpSparePartsFamilyMap.put("Bushing", dataList);

        // Initialize the 'Scraper' family map
        scraperFamilyMap = new LinkedHashMap<>();
        scraperFamilyMap.put("Scraper knife", dataList);

        // Initialize the 'Cooling' family map
        coolingFamilyMap = new LinkedHashMap<>();
        coolingFamilyMap.put("Spare part", dataList);

        // Initialize the 'Shaft' family map
        shaftFamilyMap = new LinkedHashMap<>();
        shaftFamilyMap.put("Shaft seal", dataList);

        // Initialize the 'Cylinder' family map
        cylinderFamilyMap = new LinkedHashMap<>();
        cylinderFamilyMap.put("Cylinder kit", dataList);
        cylinderFamilyMap.put("Gasket", dataList);

        // Initialize the 'Dasher' family map
        dasherFamilyMap = new LinkedHashMap<>();
        dasherFamilyMap.put("Dasher kit", dataList);
        dasherFamilyMap.put("Single part", dataList);

    }

    @PreDestroy
    public void destroyMe() {
//		neoDbProvider.closeNeo4jDriver();
//		System.out.println("Neo4jDriver in the FrigusFreezerBean have been disposed of.");
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
     * total potential of parts consumed for a specific equipment function
     *
     * @param functionLabelName e.g., Piston
     * @return the Cypher query transaction text
     */
    private String makePotentialsQueryStatement(String functionLabelName) {
        String tx = "MATCH (f1: " + functionLabelName
                + ")-[r: IN]->(eq: Equipment)-[:IB_ROUTE]->(e :Entity)-[:IN]->(cg: CustGrp) "
                + "MATCH q = (e)-[:LINKED]->(m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
                + "WHERE " + clientSelectionConstraints + " eq.constructionYear <= " + CONSUMPTION_HURDLE_YEAR + " "
                + "WITH f1.id AS eqID1, eq.runningHoursPA AS runHours, f1.serviceInterval AS serviceInterval, r.qty AS partQty "
                // Calculate Potential
                + "WITH ((runHours / serviceInterval) * partQty) AS Potential RETURN SUM(Potential) AS Potential";

        return tx;
    }

    // GETTERS HERE
    /**
     * @return the serviceKitsFamilyMap
     */
    public Map<String, List<Double>> getServiceKitsFamilyMap() {
        // System.out.println("I'm in the getServiceKitsFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate '1000 service kit' consumption grouped per customer
			 * group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_1000 SERVICE KIT");

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
                double tempPotential = getKit1000Potential();
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
                serviceKitsFamilyMap.replace("1000 service kit", valueList);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate '3000 service kit' consumption grouped per customer
			 * group
             */
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("FRE_3000 SERVICE KIT");

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
                double tempPotential = getKit3000Potential();
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
                serviceKitsFamilyMap.replace("3000 service kit", valueList1);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate '6000 service kit' consumption grouped per customer
			 * group
             */
            List<Double> valueList2 = new ArrayList<>();

            String tx2 = makeFamilyMapQueryStatement("FRE_6000 SERVICE KIT");

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
                double tempPotential = getKit6000Potential();
                valueList2.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList2
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                serviceKitsFamilyMap.replace("6000 service kit", valueList2);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate '12000 service kit' consumption grouped per customer
			 * group
             */
            List<Double> valueList3 = new ArrayList<>();

            String tx3 = makeFamilyMapQueryStatement("FRE_12000 SERVICE KIT");

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
                double tempPotential = getKit12000Potential();
                valueList3.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList3
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                serviceKitsFamilyMap.replace("12000 service kit", valueList3);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getServiceKitsFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return serviceKitsFamilyMap;
    }

    /**
     * @return the pumpFamilyMap
     */
    public Map<String, List<Double>> getPumpFamilyMap() {
        // System.out.println("I'm in the getPumpFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'New pump' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_NEW PUMP");

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
                double tempPotential = getNewPumpPotential();
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
                pumpFamilyMap.replace("New pump", valueList);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate 'Exchange pump' consumption grouped per customer group
             */
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("FRE_EXCHANGE PUMP");

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
                double tempPotential = getExchangePumpPotential();
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
                pumpFamilyMap.replace("Exchange pump", valueList1);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getPumpFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return pumpFamilyMap;
    }

    /**
     * @return the pumpSparePartsFamilyMap
     */
    public Map<String, List<Double>> getPumpSparePartsFamilyMap() {
        // System.out.println("I'm in the getPumpSparePartsFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'Kit extended' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_KIT EXTENDED");

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
                double tempPotential = getKitExtendedPotential();
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
                pumpSparePartsFamilyMap.replace("Kit extended", valueList);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate 'Star wheel' consumption grouped per customer group
             */
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("FRE_STAR WHEEL");

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
                double tempPotential = getStarWheelPotential();
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
                pumpSparePartsFamilyMap.replace("Star wheel", valueList1);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate 'Cover' consumption grouped per customer group
             */
            List<Double> valueList2 = new ArrayList<>();

            String tx2 = makeFamilyMapQueryStatement("FRE_COVER");

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
                double tempPotential = getCoverPotential();
                valueList2.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList2
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pumpSparePartsFamilyMap.replace("Cover", valueList2);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate 'Impeller' consumption grouped per customer group
             */
            List<Double> valueList3 = new ArrayList<>();

            String tx3 = makeFamilyMapQueryStatement("FRE_IMPELLER");

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
                double tempPotential = getImpellerPotential();
                valueList3.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList3
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pumpSparePartsFamilyMap.replace("Impeller", valueList3);

            }

            // *****************************************************************************************************
            /**
             * Aggregate 'Gasket kit' consumption grouped per customer group
             */
            List<Double> valueList4 = new ArrayList<>();

            String tx4 = makeFamilyMapQueryStatement("FRE_KIT");

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
                double tempPotential = getGasketKitPotential();
                valueList4.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
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
                    ratio = total / tempPotential;
                }
                valueList4
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pumpSparePartsFamilyMap.replace("Gasket kit", valueList4);

            }

            // *****************************************************************************************************
            /**
             * Aggregate 'Bushing' consumption grouped per customer group
             */
            List<Double> valueList5 = new ArrayList<>();

            String tx5 = makeFamilyMapQueryStatement("FRE_BUSHING");

            StatementResult result5 = session.run(tx5, Values.parameters(
                    "cluster", cluster,
                    "marketGroup", marketGroup,
                    "market", market,
                    "customerGroup", customerGroup,
                    "customerNumbers", customerNumbers
            ));

            while (result5.hasNext()) {
                Record r = result5.next();

                // Add the calculated potential to the value list
                double tempPotential = getBushingPotential();
                valueList5.add(new BigDecimal(String.valueOf(tempPotential)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        .doubleValue());
                /*
				 * Add total consumption divided by 3 (to get annual consumption
				 * from 36 months sales history) to the value list
                 */
                double total = r.get("TotalQty").asDouble() / 3d;

                valueList5
                        .add(new BigDecimal(String.valueOf(total)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                double ratio = 0d;
                // Calculate the consumption ratio (consumed/potential), and
                // handle division by zero exception
                if (valueList5.get(0) != 0d) {
                    ratio = total / tempPotential;
                }
                valueList5
                        .add(new BigDecimal(String.valueOf(ratio)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
                pumpSparePartsFamilyMap.replace("Bushing", valueList5);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getPumpSparePartsFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }

        return pumpSparePartsFamilyMap;
    }

    /**
     * @return the scraperFamilyMap
     */
    public Map<String, List<Double>> getScraperFamilyMap() {
        // System.out.println("I'm in the getScraperFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'Scraper knife' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_SCRAPER KNIFE");

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
                double tempPotential = getScraperKnifePotential();
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
                scraperFamilyMap.replace("Scraper knife", valueList);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getScraperFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return scraperFamilyMap;
    }

    /**
     * @return the coolingFamilyMap
     */
    public Map<String, List<Double>> getCoolingFamilyMap() {
        // System.out.println("I'm in the getCoolingFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'Spare part' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_SPARE PARTS");

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
                double tempPotential = getSparePartsPotential();
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
                coolingFamilyMap.replace("Spare part", valueList);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getCoolingFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return coolingFamilyMap;
    }

    /**
     * @return the shaftFamilyMap
     */
    public Map<String, List<Double>> getShaftFamilyMap() {
        // System.out.println("I'm in the getShaftFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'Shaft seal' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_SHAFT SEAL");

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
                double tempPotential = getShaftSealPotential();
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
                shaftFamilyMap.replace("Shaft seal", valueList);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getShaftFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return shaftFamilyMap;
    }

    /**
     * @return the cylinderFamilyMap
     */
    public Map<String, List<Double>> getCylinderFamilyMap() {
        // System.out.println("I'm in the getCylinderFamilyMap");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'Cylinder kit' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_CYLINDER KIT");

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
                double tempPotential = getCylKitPotential();
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
                cylinderFamilyMap.replace("Cylinder kit", valueList);

            }

            // *****************************************************************************************************
            /**
             * Aggregate 'Gasket' consumption grouped per customer group
             */
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("FRE_GASKETS");

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
                double tempPotential = getGasketPotential();
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
                cylinderFamilyMap.replace("Gasket", valueList1);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getCylinderFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return cylinderFamilyMap;
    }

    /**
     * @return the dasherFamilyMap
     */
    public Map<String, List<Double>> getDasherFamilyMap() {
        // System.out.println("I'm in the getDasherFamilyMap");

        // code query here
        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            /*
			 * Aggregate 'Dasher kit' consumption grouped per customer group
             */
            List<Double> valueList = new ArrayList<>();

            String tx = makeFamilyMapQueryStatement("FRE_DASHER KIT");

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
                double tempPotential = getDasherKitPotential();
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
                dasherFamilyMap.replace("Dasher kit", valueList);

            }

            // *****************************************************************************************************

            /*
			 * Aggregate 'Single part' consumption grouped per customer group
             */
            List<Double> valueList1 = new ArrayList<>();

            String tx1 = makeFamilyMapQueryStatement("FRE_SINGLE PARTS");

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
                double tempPotential = getSinglePartPotential();
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
                dasherFamilyMap.replace("Single part", valueList1);

            }

            // *****************************************************************************************************
        } catch (ClientException e) {
            System.err.println("Exception in 'getDasherFamilyMap()':" + e);
        } finally {
            // neoDbProvider.closeNeo4jDriver();

        }
        return dasherFamilyMap;
    }

    /**
     * @return the kit1000Potential
     */
    public double getKit1000Potential() {
        if (kit1000Potential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Kit1000");

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
                // 'kit1000Potential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getKit1000Potential()':" + e);
            } finally {
            }
            kit1000Potential = tempPotential;
        }
        return kit1000Potential;
    }

    /**
     * @return the kit3000Potential
     */
    public double getKit3000Potential() {
        if (kit3000Potential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Kit3000");

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
                // 'kit3000Potential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getKit3000Potential()':" + e);
            } finally {
            }
            kit3000Potential = tempPotential;
        }
        return kit3000Potential;
    }

    /**
     * @return the kit6000Potential
     */
    public double getKit6000Potential() {
        if (kit6000Potential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Kit6000");

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
                // 'kit6000Potential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getKit6000Potential()':" + e);
            } finally {
            }
            kit6000Potential = tempPotential;
        }
        return kit6000Potential;
    }

    /**
     * @return the kit12000Potential
     */
    public double getKit12000Potential() {
        if (kit12000Potential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Kit12000");

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
                // 'kit12000Potential' is %s\n",
                // LocalDateTime.now(),tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getKit12000Potential()':" + e);
            } finally {
            }
            kit12000Potential = tempPotential;
        }
        return kit12000Potential;
    }

    /**
     * @return the newPumpPotential
     */
    public double getNewPumpPotential() {
        if (newPumpPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("NewPump");

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
                // 'newPumpPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getNewPumpPotential()':" + e);
            } finally {
            }
            newPumpPotential = tempPotential;
        }
        return newPumpPotential;
    }

    /**
     * @return the exchangePumpPotential
     */
    public double getExchangePumpPotential() {
        if (exchangePumpPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("ExchangePump");

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
                // 'exchangePumpPotential' is %s\n",
                // LocalDateTime.now(),tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getExchangePumpPotential()':" + e);
            } finally {
            }
            exchangePumpPotential = tempPotential;
        }
        return exchangePumpPotential;
    }

    /**
     * @return the kitExtendedPotential
     */
    public double getKitExtendedPotential() {
        if (kitExtendedPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("ExtendedKit");

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
                // 'kitExtendedPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getKitExtendedPotential()':" + e);
            } finally {
            }
            kitExtendedPotential = tempPotential;
        }
        return kitExtendedPotential;
    }

    /**
     * @return the starWheelPotential
     */
    public double getStarWheelPotential() {
        if (starWheelPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("StarWheel");

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
                // 'starWheelPotential' is %s\n",
                // LocalDateTime.now(),tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getStarWheelPotential()':" + e);
            } finally {
            }
            starWheelPotential = tempPotential;
        }
        return starWheelPotential;
    }

    /**
     * @return the coverPotential
     */
    public double getCoverPotential() {
        if (coverPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Cover");

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
                // 'coverPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getCoverPotential()':" + e);
            } finally {
            }
            coverPotential = tempPotential;
        }
        return coverPotential;
    }

    /**
     * @return the impellerPotential
     */
    public double getImpellerPotential() {
        if (impellerPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Impeller");

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
                // 'impellerPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getImpellerPotential()':" + e);
            } finally {
            }
            impellerPotential = tempPotential;
        }
        return impellerPotential;
    }

    /**
     * @return the gasketKitPotential
     */
    public double getGasketKitPotential() {
        if (gasketKitPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("GasketKit");

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
                // 'gasketKitPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getGasketKitPotential()':" + e);
            } finally {
            }
            gasketKitPotential = tempPotential;
        }
        return gasketKitPotential;
    }

    /**
     * @return the bushingPotential
     */
    public double getBushingPotential() {
        if (bushingPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Bushing");

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
                // 'bushingPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getBushingPotential()':" + e);
            } finally {
            }
            bushingPotential = tempPotential;
        }
        return bushingPotential;
    }

    /**
     * @return the scraperKnifePotential
     */
    public double getScraperKnifePotential() {
        if (scraperKnifePotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("ScraperKnife");

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
                // 'scraperKnifePotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getScraperKnifePotential()':" + e);
            } finally {
            }
            scraperKnifePotential = tempPotential;
        }
        return scraperKnifePotential;
    }

    /**
     * @return the sparePartsPotential
     */
    public double getSparePartsPotential() {
        if (sparePartsPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("SpareParts");

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
                // 'sparePartsPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getSparePartsPotential()':" + e);
            } finally {
            }
            sparePartsPotential = tempPotential;
        }
        return sparePartsPotential;
    }

    /**
     * @return the shaftSealPotential
     */
    public double getShaftSealPotential() {
        if (shaftSealPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("ShaftSeal");

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
                // 'shaftSealPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getShaftSealPotential()':" + e);
            } finally {
            }
            shaftSealPotential = tempPotential;
        }
        return shaftSealPotential;
    }

    /**
     * @return the cylKitPotential
     */
    public double getCylKitPotential() {
        if (cylKitPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("CylinderKit");

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
                // 'cylKitPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getCylKitPotential()':" + e);
            } finally {
            }
            cylKitPotential = tempPotential;
        }
        return cylKitPotential;
    }

    /**
     * @return the gasketPotential
     */
    public double getGasketPotential() {
        if (gasketPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("Gaskets");

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
                // 'gasketPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getGasketPotential()':" + e);
            } finally {
            }
            gasketPotential = tempPotential;
        }
        return gasketPotential;
    }

    /**
     * @return the dasherKitPotential
     */
    public double getDasherKitPotential() {
        if (dasherKitPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("DasherKit");

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
                // 'dasherKitPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getDasherKitPotential()':" + e);
            } finally {
            }
            dasherKitPotential = tempPotential;
        }
        return dasherKitPotential;
    }

    /**
     * @return the singlePartPotential
     */
    public double getSinglePartPotential() {
        if (singlePartPotential == null) {

            // code query here
            double tempPotential = 0d;
            try (Session session = NeoDbProvider.getDriver().session()) {

                String tx = makePotentialsQueryStatement("SingleParts");

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
                // 'singlePartPotential' is %s\n", LocalDateTime.now(),
                // tempPotential);
            } catch (ClientException e) {
                System.err.println("Exception in 'getSinglePartPotential()':" + e);
            } finally {
            }
            singlePartPotential = tempPotential;
        }
        return singlePartPotential;
    }

}
