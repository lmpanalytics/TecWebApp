/**
 * 
 */
package com.tetrapak.tecweb.sp_consumption;

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
 *         Bean that controls the Homogeniser data
 */

@Named
@RequestScoped

/*
 * Beans that use session, application, or conversation scope must be
 * serializable, but beans that use request scope do not have to be
 * serializable.
 */
public class HomogeniserBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	CustomerSetBean customerSetBean;

	@Inject
	NeoDbProvider neoDbProvider;

	private final int CONSUMPTION_HURDLE_YEAR = LocalDate.now().minusYears(3).getYear();
	private String cluster;
	private String marketGroup;
	private String customerGroup;
	private String customerNumber;

	private String clientSelectionConstraints;

	private Map<String, List<Double>> pistonFamilyMap;
	private Map<String, List<Double>> homoDeviceFamilyMap;
	private Map<String, List<Double>> valveFamilyMap;
	private Map<String, List<Double>> crankcaseFamilyMap;

	private Double pistonPotential;
	private Double pistonSealPotential;
	private Double compressionRingPotential;

	private Double forcerPotential;
	private Double impactRingPotential;
	private Double forcerSeatPotential;

	private Double valvePotential;
	private Double valveSealingPotential;
	private Double valveSeatPotential;

	private Double plainBearingPotential;
	private Double rollerBearingPotential;
	private Double bellowPotential;

	/**
	 * Creates a new instance of HomogeniserBean
	 */
	public HomogeniserBean() {
	}

	@PostConstruct
	public void init() {
		System.out.println("I'm in the 'HomogeniserBean.init()' method.");

		cluster = customerSetBean.getSelectedCluster();
		marketGroup = customerSetBean.getSelectedMarketGroup();
		customerGroup = customerSetBean.getSelectedCustGroup();
		customerNumber = customerSetBean.getSelectedCustNumber();

		makeClientSelectionConstraints();

		List<Double> dataList = new ArrayList<>();
		dataList.add(0d);
		dataList.add(0d);
		dataList.add(0d);

		// Initialize the Piston family map
		pistonFamilyMap = new LinkedHashMap<>();
		pistonFamilyMap.put("Piston", dataList);
		pistonFamilyMap.put("Piston seal", dataList);
		pistonFamilyMap.put("Compression ring", dataList);

		// Initialize the Homo device family map
		homoDeviceFamilyMap = new LinkedHashMap<>();
		homoDeviceFamilyMap.put("Forcer", dataList);
		homoDeviceFamilyMap.put("Impact ring", dataList);
		homoDeviceFamilyMap.put("Seat", dataList);

		// Initialize the Valve family map
		valveFamilyMap = new LinkedHashMap<>();
		valveFamilyMap.put("Valve", dataList);
		valveFamilyMap.put("Valve sealing", dataList);
		valveFamilyMap.put("Valve seat", dataList);

		// Initialize the Crankcase family map
		crankcaseFamilyMap = new LinkedHashMap<>();
		crankcaseFamilyMap.put("Plain bearing", dataList);
		crankcaseFamilyMap.put("Roller bearing", dataList);
		crankcaseFamilyMap.put("Bellow", dataList);

	}

	@PreDestroy
	public void destroyMe() {

		neoDbProvider.closeNeo4jDriver();
		System.out.println("Neo4jDriver in the HomogeniserBean have been disposed of.");
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
		clientSelectionConstraints = txCluster + txMarketGroup + txCustomerGroup + txCustomerNumber;
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
				+ "MATCH (e)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
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
				+ "MATCH q = (e)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(c: Cluster)-[:IN]->(:GlobalMarket) "
				+ "WHERE " + clientSelectionConstraints + " eq.constructionYear <= " + CONSUMPTION_HURDLE_YEAR + " "
				+ "WITH f1.id AS eqID1, eq.runningHoursPA AS runHours, f1.serviceInterval AS serviceInterval, r.qty AS partQty "
				// Calculate Potential
				+ "WITH ((runHours / serviceInterval) * partQty) AS Potential RETURN SUM(Potential) AS Potential";

		return tx;
	}

	/**
	 * @return the pistonFamilyMap
	 */
	public Map<String, List<Double>> getPistonFamilyMap() {

		// System.out.println("I'm in the getPistonFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {
			// Aggregate Piston consumption grouped per customer group
			List<Double> valueList = new ArrayList<>();

			String tx = makeFamilyMapQueryStatement("HOM_PISTON");

			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();

				// Add the calculated potential to the value list
				double tempPotential = getPistonPotential();
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
				pistonFamilyMap.replace("Piston", valueList);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption = pistonFamilyMap.get("Piston").get(1);
			if (consumption == 0d) {
				valueList.add(new BigDecimal(String.valueOf(getPistonPotential())).setScale(1, BigDecimal.ROUND_HALF_UP)
						.doubleValue());
				valueList.add(0d);
				valueList.add(0d);
				pistonFamilyMap.replace("Piston", valueList);
			}

			// *****************************************************************************************************

			// Aggregate Piston seal consumption grouped per customer group
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("HOM_PISTON SEAL");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getPistonSealPotential();
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
				pistonFamilyMap.replace("Piston seal", valueList1);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption1 = pistonFamilyMap.get("Piston seal").get(1);
			if (consumption1 == 0d) {
				valueList1.add(new BigDecimal(String.valueOf(getPistonSealPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList1.add(0d);
				valueList1.add(0d);
				pistonFamilyMap.replace("Piston seal", valueList1);
			}

			// *****************************************************************************************************

			// Aggregate Compression ring consumption grouped per customer group
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("HOM_COMPRESSION RING");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getCompressionRingPotential();
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
				pistonFamilyMap.replace("Compression ring", valueList2);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption2 = pistonFamilyMap.get("Compression ring").get(1);
			if (consumption2 == 0d) {
				valueList2.add(new BigDecimal(String.valueOf(getCompressionRingPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList2.add(0d);
				valueList2.add(0d);
				pistonFamilyMap.replace("Compression ring", valueList2);
			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getPistonFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}

		return pistonFamilyMap;
	}

	/**
	 * @return the homoDeviceFamilyMap
	 */
	public Map<String, List<Double>> getHomoDeviceFamilyMap() {

		// System.out.println("I'm in the getHomoDeviceFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {
			// Aggregate Forcer consumption grouped per customer group
			List<Double> valueList = new ArrayList<>();

			String tx = makeFamilyMapQueryStatement("HOM_FORCER");

			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();

				// Add the calculated potential to the value list
				double tempPotential = getForcerPotential();
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
				homoDeviceFamilyMap.replace("Forcer", valueList);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption = homoDeviceFamilyMap.get("Forcer").get(1);
			if (consumption == 0d) {
				valueList.add(new BigDecimal(String.valueOf(getForcerPotential())).setScale(1, BigDecimal.ROUND_HALF_UP)
						.doubleValue());
				valueList.add(0d);
				valueList.add(0d);
				homoDeviceFamilyMap.replace("Forcer", valueList);
			}

			// *****************************************************************************************************

			// Aggregate Impact ring consumption grouped per customer group
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("HOM_IMPACT RING");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getImpactRingPotential();
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
				homoDeviceFamilyMap.replace("Impact ring", valueList1);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption1 = homoDeviceFamilyMap.get("Impact ring").get(1);
			if (consumption1 == 0d) {
				valueList1.add(new BigDecimal(String.valueOf(getImpactRingPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList1.add(0d);
				valueList1.add(0d);
				homoDeviceFamilyMap.replace("Impact ring", valueList1);
			}

			// *****************************************************************************************************

			// Aggregate Seat consumption grouped per customer group
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("HOM_SEAT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getForcerSeatPotential();
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
				homoDeviceFamilyMap.replace("Seat", valueList2);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption2 = homoDeviceFamilyMap.get("Seat").get(1);
			if (consumption2 == 0d) {
				valueList2.add(new BigDecimal(String.valueOf(getForcerSeatPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList2.add(0d);
				valueList2.add(0d);
				homoDeviceFamilyMap.replace("Seat", valueList2);
			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getHomoDeviceFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}

		return homoDeviceFamilyMap;
	}

	/**
	 * @return the valveFamilyMap
	 */
	public Map<String, List<Double>> getValveFamilyMap() {

		// System.out.println("I'm in the getValveFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {
			// Aggregate Valve consumption grouped per customer group
			List<Double> valueList = new ArrayList<>();

			String tx = makeFamilyMapQueryStatement("HOM_VALVE");

			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();

				// Add the calculated potential to the value list
				double tempPotential = getValvePotential();
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
				valveFamilyMap.replace("Valve", valueList);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption = valveFamilyMap.get("Valve").get(1);
			if (consumption == 0d) {
				valueList.add(new BigDecimal(String.valueOf(getValvePotential())).setScale(1, BigDecimal.ROUND_HALF_UP)
						.doubleValue());
				valueList.add(0d);
				valueList.add(0d);
				valveFamilyMap.replace("Valve", valueList);
			}

			// *****************************************************************************************************

			// Aggregate Valve sealing consumption grouped per customer group
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("HOM_VALVE SEALING");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getValveSealingPotential();
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
				valveFamilyMap.replace("Valve sealing", valueList1);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption1 = valveFamilyMap.get("Valve sealing").get(1);
			if (consumption1 == 0d) {
				valueList1.add(new BigDecimal(String.valueOf(getValveSealingPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList1.add(0d);
				valueList1.add(0d);
				valveFamilyMap.replace("Valve sealing", valueList1);
			}

			// *****************************************************************************************************

			// Aggregate Valve seat consumption grouped per customer group
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("HOM_VALVE SEAT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getValveSeatPotential();
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
				valveFamilyMap.replace("Valve seat", valueList2);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption2 = valveFamilyMap.get("Valve seat").get(1);
			if (consumption2 == 0d) {
				valueList2.add(new BigDecimal(String.valueOf(getValveSeatPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList2.add(0d);
				valueList2.add(0d);
				valveFamilyMap.replace("Valve seat", valueList2);
			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getValveFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}

		return valveFamilyMap;
	}

	/**
	 * @return the crankcaseFamilyMap
	 */
	public Map<String, List<Double>> getCrankcaseFamilyMap() {

		// System.out.println("I'm in the getCrankcaseFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {
			// Aggregate Plain bearing consumption grouped per customer group
			List<Double> valueList = new ArrayList<>();

			String tx = makeFamilyMapQueryStatement("HOM_PLAIN BEARING");

			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();

				// Add the calculated potential to the value list
				double tempPotential = getPlainBearingPotential();
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
				crankcaseFamilyMap.replace("Plain bearing", valueList);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption = crankcaseFamilyMap.get("Plain bearing").get(1);
			if (consumption == 0d) {
				valueList.add(new BigDecimal(String.valueOf(getPlainBearingPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList.add(0d);
				valueList.add(0d);
				crankcaseFamilyMap.replace("Plain bearing", valueList);
			}

			// *****************************************************************************************************

			// Aggregate Roller bearing consumption grouped per customer group
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("HOM_ROLLER BEARING");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getRollerBearingPotential();
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
				crankcaseFamilyMap.replace("Roller bearing", valueList1);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption1 = crankcaseFamilyMap.get("Roller bearing").get(1);
			if (consumption1 == 0d) {
				valueList1.add(new BigDecimal(String.valueOf(getRollerBearingPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList1.add(0d);
				valueList1.add(0d);
				crankcaseFamilyMap.replace("Roller bearing", valueList1);
			}

			// *****************************************************************************************************

			// Aggregate Bellow consumption grouped per customer group
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("HOM_BELLOWS");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getBellowPotential();
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
				crankcaseFamilyMap.replace("Bellow", valueList2);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption2 = crankcaseFamilyMap.get("Bellow").get(1);
			if (consumption2 == 0d) {
				valueList2.add(new BigDecimal(String.valueOf(getBellowPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList2.add(0d);
				valueList2.add(0d);
				crankcaseFamilyMap.replace("Bellow", valueList2);
			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getCrankcaseFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}

		return crankcaseFamilyMap;
	}

	/**
	 * @return the pistonPotential
	 */

	public double getPistonPotential() {

		if (pistonPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Piston");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'pistonPotential' is %s\n",
				// LocalDateTime.now(),tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPistonPotential()':" + e);
			} finally {
			}
			pistonPotential = tempPotential;
		}
		return pistonPotential;
	}

	/**
	 * @return the pistonSealPotential
	 */

	public double getPistonSealPotential() {

		if (pistonSealPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("PistonSeal");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'pistonSealPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPistonSealPotential()':" + e);
			} finally {
			}
			pistonSealPotential = tempPotential;
		}

		return pistonSealPotential;
	}

	/**
	 * @return the compressionRingPotential
	 */

	public double getCompressionRingPotential() {

		if (compressionRingPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("CompressionRing");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'compressionRingPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getCompressionRingPotential()':" + e);
			} finally {
			}
			compressionRingPotential = tempPotential;
		}

		return compressionRingPotential;
	}

	/**
	 * @return the forcerPotential
	 */

	public double getForcerPotential() {

		if (forcerPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Forcer");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'forcerPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getForcerPotential()':" + e);
			} finally {
			}
			forcerPotential = tempPotential;
		}

		return forcerPotential;
	}

	/**
	 * @return the impactRingPotential
	 */

	public double getImpactRingPotential() {

		if (impactRingPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("ImpactRing");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'impactRingPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getImpactRingPotential()':" + e);
			} finally {
			}
			impactRingPotential = tempPotential;
		}

		return impactRingPotential;
	}

	/**
	 * @return the forcerSeatPotential
	 */

	public double getForcerSeatPotential() {

		if (forcerSeatPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Seat");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'forcerSeatPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getForcerSeatPotential()':" + e);
			} finally {
			}
			forcerSeatPotential = tempPotential;
		}

		return forcerSeatPotential;
	}

	/**
	 * @return the valvePotential
	 */

	public double getValvePotential() {

		if (valvePotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Valve");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'valvePotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getValvePotential()':" + e);
			} finally {
			}
			valvePotential = tempPotential;
		}

		return valvePotential;
	}

	/**
	 * @return the valveSealingPotential
	 */

	public double getValveSealingPotential() {

		if (valveSealingPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("ValveSealing");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'valveSealingPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getValveSealingPotential()':" + e);
			} finally {
			}
			valveSealingPotential = tempPotential;
		}

		return valveSealingPotential;
	}

	/**
	 * @return the valveSeatPotential
	 */

	public double getValveSeatPotential() {

		if (valveSeatPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("ValveSeat");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'valveSeatPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getValveSeatPotential()':" + e);
			} finally {
			}
			valveSeatPotential = tempPotential;
		}

		return valveSeatPotential;
	}

	/**
	 * @return the plainBearingPotential
	 */

	public double getPlainBearingPotential() {

		if (plainBearingPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("PlainBearing");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'plainBearingPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getPlainBearingPotential()':" + e);
			} finally {
			}
			plainBearingPotential = tempPotential;
		}

		return plainBearingPotential;
	}

	/**
	 * @return the rollerBearingPotential
	 */

	public double getRollerBearingPotential() {

		if (rollerBearingPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("RollerBearing");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'rollerBearingPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getRollerBearingPotential()':" + e);
			} finally {
			}
			rollerBearingPotential = tempPotential;
		}

		return rollerBearingPotential;
	}

	/**
	 * @return the bellowPotential
	 */

	public double getBellowPotential() {

		if (bellowPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("Bellow");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'bellowPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getBellowPotential()':" + e);
			} finally {
			}
			bellowPotential = tempPotential;
		}

		return bellowPotential;
	}

}
