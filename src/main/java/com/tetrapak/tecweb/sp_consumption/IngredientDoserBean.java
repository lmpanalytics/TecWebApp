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
import org.neo4j.driver.v1.exceptions.ClientException;

/**
 * @author SEPALMM
 *
 *         Bean that controls the Ingredient Doser data
 *
 */

@Named
@RequestScoped

public class IngredientDoserBean implements Serializable {

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
	private String customerNumber;

	private String clientSelectionConstraints;

	private Map<String, List<Double>> serviceKitsFamilyMap;
	private Map<String, List<Double>> hopperFamilyMap;
	private Map<String, List<Double>> mixerFamilyMap;
	private Map<String, List<Double>> pumpFamilyMap;
	private Map<String, List<Double>> coverFamilyMap;
	private Map<String, List<Double>> agitatorFamilyMap;
	private Map<String, List<Double>> singlePartsFamilyMap;

	private Double kit3000Potential;
	private Double kit6000Potential;
	private Double kit12000Potential;

	private Double hopperKit3000Potential;
	private Double hopperKit6000Potential;
	private Double hopperKit12000Potential;

	private Double mixerKit3000Potential;
	private Double mixerKit6000Potential;
	private Double mixerKit12000Potential;

	private Double pumpKit3000Potential;
	private Double pumpKit6000Potential;
	private Double pumpKit12000Potential;

	private Double coverKit3000Potential;

	private Double agitatorKit6000Potential;
	private Double agitatorKit12000Potential;

	private Double lamellaPotential;
	private Double dasherPotential;
	private Double pumpHousePotential;

	/**
	 * Creates a new instance of IngredientDoserBean
	 */
	public IngredientDoserBean() {
		// TODO Auto-generated constructor stub
	}

	@PostConstruct
	public void init() {
		System.out.println("I'm in the 'IngredientDoserBean.init()' method.");

		cluster = customerSetBean.getSelectedCluster();
		marketGroup = customerSetBean.getSelectedMarketGroup();
		market = customerSetBean.getSelectedMarket();
		customerGroup = customerSetBean.getSelectedCustGroup();
		customerNumber = customerSetBean.getSelectedCustNumber();

		makeClientSelectionConstraints();

		List<Double> dataList = new ArrayList<>();
		dataList.add(0d);
		dataList.add(0d);
		dataList.add(0d);

		// INITIALIZE CLASS SPECIFIC MAPS AND FIELDS HERE
		// Initialize the 'Service kits' family map
		serviceKitsFamilyMap = new LinkedHashMap<>();
		serviceKitsFamilyMap.put("3000 service kit", dataList);
		serviceKitsFamilyMap.put("6000 service kit", dataList);
		serviceKitsFamilyMap.put("12000 service kit", dataList);

		// Initialize the 'Hopper' family map
		hopperFamilyMap = new LinkedHashMap<>();
		hopperFamilyMap.put("3000 hopper kit", dataList);
		hopperFamilyMap.put("6000 hopper kit", dataList);
		hopperFamilyMap.put("12000 hopper kit", dataList);

		// Initialize the 'Mixer' family map
		mixerFamilyMap = new LinkedHashMap<>();
		mixerFamilyMap.put("3000 mixer kit", dataList);
		mixerFamilyMap.put("6000 mixer kit", dataList);
		mixerFamilyMap.put("12000 mixer kit", dataList);

		// Initialize the 'Pump' family map
		pumpFamilyMap = new LinkedHashMap<>();
		pumpFamilyMap.put("3000 pump kit", dataList);
		pumpFamilyMap.put("6000 pump kit", dataList);
		pumpFamilyMap.put("12000 pump kit", dataList);

		// Initialize the 'Cover' family map
		coverFamilyMap = new LinkedHashMap<>();
		coverFamilyMap.put("3000 cover kit", dataList);

		// Initialize the 'Agitator' family map
		agitatorFamilyMap = new LinkedHashMap<>();
		agitatorFamilyMap.put("6000 agitator kit", dataList);
		agitatorFamilyMap.put("12000 agitator kit", dataList);

		// Initialize the 'Single parts' family map
		singlePartsFamilyMap = new LinkedHashMap<>();
		singlePartsFamilyMap.put("Lamella set", dataList);
		singlePartsFamilyMap.put("Dasher", dataList);
		singlePartsFamilyMap.put("Pump house", dataList);

	}

	@PreDestroy
	public void destroyMe() {
//		neoDbProvider.closeNeo4jDriver();
//		System.out.println("Neo4jDriver in the IngredientDoserBean have been disposed of.");
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
		String txCustomerNumber;

		if (cluster.equals("ALL CLUSTERS")) {
			txCluster = "";
		} else {
			txCluster = "c.id = '" + cluster + "' AND ";
		}
		if (marketGroup.equals("ALL MARKET GROUPS")) {
			txMarketGroup = "";
		} else {
			txMarketGroup = "mg.name = '" + marketGroup + "' AND ";
		}
		if (market.equals("ALL MARKETS")) {
			txMarket = "";
		} else {
			txMarket = "m.name = '" + market + "' AND ";
		}
		if (customerGroup.equals("ALL CUSTOMER GROUPS")) {
			txCustomerGroup = "";
		} else {
			txCustomerGroup = "cg.name = '" + customerGroup + "' AND ";
		}
		if (customerNumber.equals("ALL CUSTOMER NUMBERS")) {
			txCustomerNumber = "";
		} else {
			txCustomerNumber = "e.id = '" + customerNumber + "' AND ";
		}
		clientSelectionConstraints = txCluster + txMarketGroup + txMarket + txCustomerGroup + txCustomerNumber;
		// System.out.format("****************** THIS IS THE
		// clientSelectionConstraints *******************\n%s\n ",
		// clientSelectionConstraints);
	}

	/**
	 * Constructs the Cypher query transaction text used in calculating the
	 * total quantity of parts consumed for a specific parts family
	 * 
	 * @param partFamilyName
	 *            e.g., HOM_PISTON
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
	 * @param functionLabelName
	 *            e.g., Piston
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
			 * Aggregate '3000 service kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_3000 SERVICE KIT");

			StatementResult result1 = session.run(tx1);

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

			String tx2 = makeFamilyMapQueryStatement("IDO_6000 SERVICE KIT");

			StatementResult result2 = session.run(tx2);

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

			String tx3 = makeFamilyMapQueryStatement("IDO_12000 SERVICE KIT");

			StatementResult result3 = session.run(tx3);

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
	 * @return the hopperFamilyMap
	 */
	public Map<String, List<Double>> getHopperFamilyMap() {
		// System.out.println("I'm in the getHopperFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			/*
			 * Aggregate '3000 hopper kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_3000 HOPPER KIT");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getHopperKit3000Potential();
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
				hopperFamilyMap.replace("3000 hopper kit", valueList1);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '6000 hopper kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("IDO_6000 HOPPER KIT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getHopperKit6000Potential();
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
				hopperFamilyMap.replace("6000 hopper kit", valueList2);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '12000 hopper kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList3 = new ArrayList<>();

			String tx3 = makeFamilyMapQueryStatement("IDO_12000 HOPPER KIT");

			StatementResult result3 = session.run(tx3);

			while (result3.hasNext()) {
				Record r = result3.next();

				// Add the calculated potential to the value list
				double tempPotential = getHopperKit12000Potential();
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
				hopperFamilyMap.replace("12000 hopper kit", valueList3);

			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getHopperFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}
		return hopperFamilyMap;
	}

	/**
	 * @return the mixerFamilyMap
	 */
	public Map<String, List<Double>> getMixerFamilyMap() {
		// System.out.println("I'm in the getMixerFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			/*
			 * Aggregate '3000 mixer kit' consumption grouped per customer group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_3000 MIXER KIT");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getMixerKit3000Potential();
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
				mixerFamilyMap.replace("3000 mixer kit", valueList1);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '6000 mixer kit' consumption grouped per customer group
			 */
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("IDO_6000 MIXER KIT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getMixerKit6000Potential();
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
				mixerFamilyMap.replace("6000 mixer kit", valueList2);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '12000 mixer kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList3 = new ArrayList<>();

			String tx3 = makeFamilyMapQueryStatement("IDO_12000 MIXER KIT");

			StatementResult result3 = session.run(tx3);

			while (result3.hasNext()) {
				Record r = result3.next();

				// Add the calculated potential to the value list
				double tempPotential = getMixerKit12000Potential();
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
				mixerFamilyMap.replace("12000 mixer kit", valueList3);

			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getMixerFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}
		return mixerFamilyMap;
	}

	/**
	 * @return the pumpFamilyMap
	 */
	public Map<String, List<Double>> getPumpFamilyMap() {
		// System.out.println("I'm in the getPumpFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			/*
			 * Aggregate '3000 pump kit' consumption grouped per customer group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_3000 PUMP KIT");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getPumpKit3000Potential();
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
				pumpFamilyMap.replace("3000 pump kit", valueList1);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '6000 pump kit' consumption grouped per customer group
			 */
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("IDO_6000 PUMP KIT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getPumpKit6000Potential();
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
				pumpFamilyMap.replace("6000 pump kit", valueList2);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '12000 pump kit' consumption grouped per customer group
			 */
			List<Double> valueList3 = new ArrayList<>();

			String tx3 = makeFamilyMapQueryStatement("IDO_12000 PUMP KIT");

			StatementResult result3 = session.run(tx3);

			while (result3.hasNext()) {
				Record r = result3.next();

				// Add the calculated potential to the value list
				double tempPotential = getPumpKit12000Potential();
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
				pumpFamilyMap.replace("12000 pump kit", valueList3);

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
	 * @return the coverFamilyMap
	 */
	public Map<String, List<Double>> getCoverFamilyMap() {
		// System.out.println("I'm in the getCoverFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			/*
			 * Aggregate '3000 cover kit' consumption grouped per customer group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_3000 COVER KIT");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getCoverKit3000Potential();
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
				coverFamilyMap.replace("3000 cover kit", valueList1);

			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getCoverFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}
		return coverFamilyMap;
	}

	/**
	 * @return the agitatorFamilyMap
	 */
	public Map<String, List<Double>> getAgitatorFamilyMap() {
		// System.out.println("I'm in the getAgitatorFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			/*
			 * Aggregate '6000 agitator kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_6000 AGITATOR KIT");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getAgitatorKit6000Potential();
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
				agitatorFamilyMap.replace("6000 agitator kit", valueList1);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate '12000 agitator kit' consumption grouped per customer
			 * group
			 */
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("IDO_12000 AGITATOR KIT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getAgitatorKit12000Potential();
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
				agitatorFamilyMap.replace("12000 agitator kit", valueList2);

			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getAgitatorFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}
		return agitatorFamilyMap;
	}

	/**
	 * @return the singlePartsFamilyMap
	 */
	public Map<String, List<Double>> getSinglePartsFamilyMap() {
		// System.out.println("I'm in the getSinglePartsFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			/*
			 * Aggregate 'Lamella set' consumption grouped per customer group
			 */
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("IDO_LAMELLA SET");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getLamellaPotential();
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
				singlePartsFamilyMap.replace("Lamella set", valueList1);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate 'Dasher' consumption grouped per customer group
			 */
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("IDO_DASHER");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getDasherPotential();
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
				singlePartsFamilyMap.replace("Dasher", valueList2);

			}

			// *****************************************************************************************************

			/*
			 * Aggregate 'Pump house' consumption grouped per customer group
			 */
			List<Double> valueList3 = new ArrayList<>();

			String tx3 = makeFamilyMapQueryStatement("IDO_PUMP HOUSE");

			StatementResult result3 = session.run(tx3);

			while (result3.hasNext()) {
				Record r = result3.next();

				// Add the calculated potential to the value list
				double tempPotential = getPumpHousePotential();
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
				singlePartsFamilyMap.replace("Pump house", valueList3);

			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getSinglePartsFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}
		return singlePartsFamilyMap;
	}

	/**
	 * @return the kit3000Potential
	 */
	public Double getKit3000Potential() {
		if (kit3000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("DosKit3000");

				StatementResult result = session.run(tx);

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
	public Double getKit6000Potential() {
		if (kit6000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("DosKit6000");

				StatementResult result = session.run(tx);

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
	public Double getKit12000Potential() {
		if (kit12000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("DosKit12000");

				StatementResult result = session.run(tx);

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
	 * @return the hopperKit3000Potential
	 */
	public Double getHopperKit3000Potential() {
		if (hopperKit3000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("HopperKit3000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'hopperKit3000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getHopperKit3000Potential()':" + e);
			} finally {
			}
			hopperKit3000Potential = tempPotential;
		}
		return hopperKit3000Potential;
	}

	/**
	 * @return the hopperKit6000Potential
	 */
	public Double getHopperKit6000Potential() {
		if (hopperKit6000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("HopperKit6000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'hopperKit6000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getHopperKit6000Potential()':" + e);
			} finally {
			}
			hopperKit6000Potential = tempPotential;
		}
		return hopperKit6000Potential;
	}

	/**
	 * @return the hopperKit12000Potential
	 */
	public Double getHopperKit12000Potential() {
		if (hopperKit12000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("HopperKit12000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'hopperKit12000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getHopperKit12000Potential()':" + e);
			} finally {
			}
			hopperKit12000Potential = tempPotential;
		}
		return hopperKit12000Potential;
	}

	/**
	 * @return the mixerKit3000Potential
	 */
	public Double getMixerKit3000Potential() {
		if (mixerKit3000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("MixerKit3000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'mixerKit3000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getMixerKit3000Potential()':" + e);
			} finally {
			}
			mixerKit3000Potential = tempPotential;
		}
		return mixerKit3000Potential;
	}

	/**
	 * @return the mixerKit6000Potential
	 */
	public Double getMixerKit6000Potential() {
		if (mixerKit6000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("MixerKit6000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'mixerKit6000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getMixerKit6000Potential()':" + e);
			} finally {
			}
			mixerKit6000Potential = tempPotential;
		}
		return mixerKit6000Potential;
	}

	/**
	 * @return the mixerKit12000Potential
	 */
	public Double getMixerKit12000Potential() {
		if (mixerKit12000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("MixerKit12000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'mixerKit12000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getMixerKit12000Potential()':" + e);
			} finally {
			}
			mixerKit12000Potential = tempPotential;
		}
		return mixerKit12000Potential;
	}

	/**
	 * @return the pumpKit3000Potential
	 */
	public Double getPumpKit3000Potential() {
		if (pumpKit3000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("PumpKit3000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'pumpKit3000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPumpKit3000Potential()':" + e);
			} finally {
			}
			pumpKit3000Potential = tempPotential;
		}
		return pumpKit3000Potential;
	}

	/**
	 * @return the pumpKit6000Potential
	 */
	public Double getPumpKit6000Potential() {
		if (pumpKit6000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("PumpKit6000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'pumpKit6000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPumpKit6000Potential()':" + e);
			} finally {
			}
			pumpKit6000Potential = tempPotential;
		}
		return pumpKit6000Potential;
	}

	/**
	 * @return the pumpKit12000Potential
	 */
	public Double getPumpKit12000Potential() {
		if (pumpKit12000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("PumpKit12000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'pumpKit12000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPumpKit12000Potential()':" + e);
			} finally {
			}
			pumpKit12000Potential = tempPotential;
		}
		return pumpKit12000Potential;
	}

	/**
	 * @return the coverKit3000Potential
	 */
	public Double getCoverKit3000Potential() {
		if (coverKit3000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("CoverKit3000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'coverKit3000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getCoverKit3000Potential()':" + e);
			} finally {
			}
			coverKit3000Potential = tempPotential;
		}
		return coverKit3000Potential;
	}

	/**
	 * @return the agitatorKit6000Potential
	 */
	public Double getAgitatorKit6000Potential() {
		if (agitatorKit6000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("AgitatorKit6000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'agitatorKit6000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getAgitatorKit6000Potential()':" + e);
			} finally {
			}
			agitatorKit6000Potential = tempPotential;
		}
		return agitatorKit6000Potential;
	}

	/**
	 * @return the agitatorKit12000Potential
	 */
	public Double getAgitatorKit12000Potential() {
		if (agitatorKit12000Potential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("AgitatorKit12000");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'agitatorKit12000Potential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getAgitatorKit12000Potential()':" + e);
			} finally {
			}
			agitatorKit12000Potential = tempPotential;
		}
		return agitatorKit12000Potential;
	}

	/**
	 * @return the lamellaPotential
	 */
	public Double getLamellaPotential() {
		if (lamellaPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Lamella");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'lamellaPotential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getLamellaPotential()':" + e);
			} finally {
			}
			lamellaPotential = tempPotential;
		}
		return lamellaPotential;
	}

	/**
	 * @return the dasherPotential
	 */
	public Double getDasherPotential() {
		if (dasherPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Dasher");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'dasherPotential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getDasherPotential()':" + e);
			} finally {
			}
			dasherPotential = tempPotential;
		}
		return dasherPotential;
	}

	/**
	 * @return the pumpHousePotential
	 */
	public Double getPumpHousePotential() {
		if (pumpHousePotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("PumpHouse");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'pumpHousePotential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPumpHousePotential()':" + e);
			} finally {
			}
			pumpHousePotential = tempPotential;
		}
		return pumpHousePotential;
	}

}
