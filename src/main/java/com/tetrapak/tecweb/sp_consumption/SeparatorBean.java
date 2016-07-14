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
 *         Bean that controls the Separator data
 */

@Named
@RequestScoped

public class SeparatorBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	CustomerSetBean customerSetBean;

	@Inject
	NeoDbProvider neoDbProvider;

	// ADD CLASS SPECIFIC MAPS AND FIELDS HERE
	private final int CONSUMPTION_HURDLE_YEAR = LocalDate.now().minusYears(3).getYear();
	private String cluster;
	private String marketGroup;
	private String customerGroup;
	private String customerNumber;

	private String clientSelectionConstraints;

	private Map<String, List<Double>> kitFamilyMap;

	private Double iMKitPotential;
	private Double mSKitPotential;
	private Double oWMCkitPotential;
	private Double fFkitPotential;

	/**
	 * Creates a new instance of SeparatorBean
	 */
	public SeparatorBean() {
	}

	@PostConstruct
	public void init() {
		System.out.println("I'm in the 'SeparatorBean.init()' method.");

		cluster = customerSetBean.getSelectedCluster();
		marketGroup = customerSetBean.getSelectedMarketGroup();
		customerGroup = customerSetBean.getSelectedCustGroup();
		customerNumber = customerSetBean.getSelectedCustNumber();

		makeClientSelectionConstraints();

		List<Double> dataList = new ArrayList<>();
		dataList.add(0d);
		dataList.add(0d);
		dataList.add(0d);

		// INITIALIZE CLASS SPECIFIC MAPS HERE
		// Initialize the kit family map
		kitFamilyMap = new LinkedHashMap<>();
		kitFamilyMap.put("Intermediate service kit", dataList);
		kitFamilyMap.put("Major service kit", dataList);
		kitFamilyMap.put("OWMC service kit", dataList);
		kitFamilyMap.put("Foundation feet service kit", dataList);
	}

	@PreDestroy
	public void destroyMe() {

		neoDbProvider.closeNeo4jDriver();
		System.out.println("Neo4jDriver in the SeparatorBean have been disposed of.");
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
				+ "WITH f1.id AS eqID1, eq.runningHoursPA AS runHours, f1.serviceInterval AS serviceInterval "
				// Calculate Potential
				+ "WITH (runHours / serviceInterval) AS Potential RETURN SUM(Potential) AS Potential";

		return tx;
	}

	// GETTERS HERE

	/**
	 * @return the kitFamilyMap
	 */
	public Map<String, List<Double>> getKitFamilyMap() {

		// System.out.println("I'm in the getKitFamilyMap");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {
			// Aggregate Intermediate service kit consumption grouped per
			// customer group
			List<Double> valueList = new ArrayList<>();

			String tx = makeFamilyMapQueryStatement("SEP_INTERMEDIATE SERVICE KIT");

			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();

				// Add the calculated potential to the value list
				double tempPotential = getiMKitPotential();
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
				kitFamilyMap.replace("Intermediate service kit", valueList);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption = kitFamilyMap.get("Intermediate service kit").get(1);
			if (consumption == 0d) {
				valueList.add(new BigDecimal(String.valueOf(getiMKitPotential())).setScale(1, BigDecimal.ROUND_HALF_UP)
						.doubleValue());
				valueList.add(0d);
				valueList.add(0d);
				kitFamilyMap.replace("Intermediate service kit", valueList);
			}

			// *****************************************************************************************************

			// Aggregate Major service kit consumption grouped per customer
			// group
			List<Double> valueList1 = new ArrayList<>();

			String tx1 = makeFamilyMapQueryStatement("SEP_MAJOR SERVICE KIT");

			StatementResult result1 = session.run(tx1);

			while (result1.hasNext()) {
				Record r = result1.next();

				// Add the calculated potential to the value list
				double tempPotential = getmSKitPotential();
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
				kitFamilyMap.replace("Major service kit", valueList1);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption1 = kitFamilyMap.get("Major service kit").get(1);
			if (consumption1 == 0d) {
				valueList1.add(new BigDecimal(String.valueOf(getmSKitPotential())).setScale(1, BigDecimal.ROUND_HALF_UP)
						.doubleValue());
				valueList1.add(0d);
				valueList1.add(0d);
				kitFamilyMap.replace("Major service kit", valueList1);
			}

			// *****************************************************************************************************

			// Aggregate OWMC service kit consumption grouped per customer group
			List<Double> valueList2 = new ArrayList<>();

			String tx2 = makeFamilyMapQueryStatement("SEP_OWMC KIT");

			StatementResult result2 = session.run(tx2);

			while (result2.hasNext()) {
				Record r = result2.next();

				// Add the calculated potential to the value list
				double tempPotential = getoWMCkitPotential();
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
				kitFamilyMap.replace("OWMC service kit", valueList2);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption2 = kitFamilyMap.get("OWMC service kit").get(1);
			if (consumption2 == 0d) {
				valueList2.add(new BigDecimal(String.valueOf(getoWMCkitPotential()))
						.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
				valueList2.add(0d);
				valueList2.add(0d);
				kitFamilyMap.replace("OWMC service kit", valueList2);
			}

			// *****************************************************************************************************

			// Aggregate Foundation feet service kit consumption grouped per
			// customer group
			List<Double> valueList3 = new ArrayList<>();

			String tx3 = makeFamilyMapQueryStatement("SEP_FOUNDATION FEET KIT");

			StatementResult result3 = session.run(tx3);

			while (result3.hasNext()) {
				Record r = result3.next();

				// Add the calculated potential to the value list
				double tempPotential = getfFkitPotential();
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
				kitFamilyMap.replace("Foundation feet service kit", valueList3);

			}

			/*
			 * If there has been no consumption then the above code doesn't add
			 * any calculated potential. This case is handled below.
			 */
			Double consumption3 = kitFamilyMap.get("Foundation feet service kit").get(1);
			if (consumption3 == 0d) {
				valueList3.add(new BigDecimal(String.valueOf(getfFkitPotential())).setScale(1, BigDecimal.ROUND_HALF_UP)
						.doubleValue());
				valueList3.add(0d);
				valueList3.add(0d);
				kitFamilyMap.replace("Foundation feet service kit", valueList3);
			}

			// *****************************************************************************************************

		} catch (ClientException e) {
			System.err.println("Exception in 'getkitFamilyMap()':" + e);
		} finally {
			// neoDbProvider.closeNeo4jDriver();

		}
		return kitFamilyMap;
	}

	/**
	 * @return the iMKitPotential
	 */
	public double getiMKitPotential() {

		if (iMKitPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("IMkit");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'iMKitPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getiMKitPotential()':" + e);
			} finally {
			}
			iMKitPotential = tempPotential;
		}

		return iMKitPotential;
	}

	/**
	 * @return the mSKitPotential
	 */
	public double getmSKitPotential() {

		if (mSKitPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("MSkit");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'mSKitPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getmSKitPotential()':" + e);
			} finally {
			}
			mSKitPotential = tempPotential;
		}
		return mSKitPotential;
	}

	/**
	 * @return the oWMCkitPotential
	 */
	public double getoWMCkitPotential() {

		if (oWMCkitPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("OWMCkit");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'oWMCkitPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getoWMCkitPotential()':" + e);
			} finally {
			}
			oWMCkitPotential = tempPotential;
		}
		return oWMCkitPotential;
	}

	/**
	 * @return the fFkitPotential
	 */
	public double getfFkitPotential() {

		if (fFkitPotential == null) {

			// code query here
			double tempPotential = 0d;
			try (Session session = NeoDbProvider.getDriver().session()) {

				String tx = makePotentialsQueryStatement("FFkit");

				StatementResult result = session.run(tx);

				while (result.hasNext()) {
					Record r = result.next();
					tempPotential = r.get("Potential").asDouble();
				}

				// System.out.printf("%s > Queried Potential for
				// 'fFkitPotential' is %s\n", LocalDateTime.now(),
				// tempPotential);

			} catch (ClientException e) {
				System.err.println("Exception in 'getfFkitPotential()':" + e);
			} finally {
			}
			fFkitPotential = tempPotential;
		}
		return fFkitPotential;
	}

}
