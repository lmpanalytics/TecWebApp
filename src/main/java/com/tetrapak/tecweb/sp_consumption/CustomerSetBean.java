package com.tetrapak.tecweb.sp_consumption;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.ClientException;

/**
 * @author SEPALMM
 * 
 *         This is a set of customers to select from.
 */
@Named
@SessionScoped
/*
 * Will keep the last selected customer number / group in memory across the
 * session
 */

/*
 * Beans that use session, application, or conversation scope must be
 * serializable, but beans that use request scope do not have to be
 * serializable.
 */

public class CustomerSetBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	NeoDbProvider neoDbProvider;

	private Set<String> clusterSet;
	private Set<String> marketGroupSet;
	private Set<String> custGroupSet;
//	private Set<String> custNumberSet;
	private Map<String, String> custNumberMap;

	private String selectedCluster;
	private String selectedMarketGroup;
	private String selectedCustGroup;
	private String selectedCustNumber;
	private String selectedCustName;

	// Creates a new instance of CustomerListBean
	public CustomerSetBean() {

	}

	@PostConstruct
	public void init() {
		clusterSet = new LinkedHashSet<>();
		marketGroupSet = new LinkedHashSet<>();
		custGroupSet = new LinkedHashSet<>();
//		custNumberSet = new LinkedHashSet<>();
		custNumberMap =  new LinkedHashMap<>();

		clusterSet.add("ALL CLUSTERS");
		marketGroupSet.add("ALL MARKET GROUPS");
		custGroupSet.add("ALL CUSTOMER GROUPS");
//		custNumberSet.add("ALL CUSTOMER NUMBERS");
		custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

		queryClusterSet();
		queryMarketGroupSet();
		queryCustGroupSet();
//		queryCustNumberSet();
		queryCustNumberMap();

	}

	/**
	 * Make collection of all Clusters
	 * 
	 * @return the clusterSet
	 */
	public Set<String> queryClusterSet() {
		marketGroupSet.clear();
		marketGroupSet.add("ALL MARKET GROUPS");

		custGroupSet.clear();
		custGroupSet.add("ALL CUSTOMER GROUPS");

//		custNumberSet.clear();
//		custNumberSet.add("ALL CUSTOMER NUMBERS");
		custNumberMap.clear();
		custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			// Run single statements one-by-one, OR...
			String tx = "MATCH (c:Cluster) RETURN distinct c.id AS name ORDER BY name";
			// System.out.format("tx query text: %s", tx);
			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();
				String key = r.get("name").asString();
				clusterSet.add(key);
			}

			System.out.printf("%s > Queried Cluster name set\n", LocalDateTime.now());
			System.out.printf("Size of clusterSet is %s.\n", clusterSet.size());
		} catch (ClientException e) {
			System.err.println("Exception in 'queryClusterSet()':" + e);
		} finally {
			neoDbProvider.closeNeo4jDriver();
			// System.out.printf("size of clusterSet is %s::\n",
			// clusterSet.size());

		}

		return clusterSet;

	}

	/**
	 * Make collection of all Market groups
	 * 
	 * @return the marketGroupSet
	 */
	public Set<String> queryMarketGroupSet() {
		marketGroupSet.clear();
		marketGroupSet.add("ALL MARKET GROUPS");

		custGroupSet.clear();
		custGroupSet.add("ALL CUSTOMER GROUPS");

//		custNumberSet.clear();
//		custNumberSet.add("ALL CUSTOMER NUMBERS");
		custNumberMap.clear();
		custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			// Run single statements one-by-one, OR...
			String tx;
			if (getSelectedCluster() == null || getSelectedCluster().equals("ALL CLUSTERS")) {
				tx = "MATCH (mg:MarketGrp) RETURN distinct mg.name AS name ORDER BY name";
			} else {
				tx = "MATCH (mg: MarketGrp)-[:IN]-(cl: Cluster) WHERE cl.id = '" + getSelectedCluster()
						+ "' RETURN mg.name AS name ORDER BY name";
			}
			System.out.format("queryMarketGroupSet, tx query text: %s", tx);
			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();
				String key = r.get("name").asString();
				marketGroupSet.add(key);
			}

			System.out.printf("%s > Queried Market group name set\n", LocalDateTime.now());
			System.out.printf("Size of marketGroupSet is %s.\n", marketGroupSet.size());
		} catch (ClientException e) {
			System.err.println("Exception in 'queryMarketGroupSet()':" + e);
			System.out.printf("Size of marketGroupSet is %s.\n", marketGroupSet.size());
		} finally {
			neoDbProvider.closeNeo4jDriver();
			// System.out.printf("size of marketGroupSet is %s::\n",
			// marketGroupSet.size());

		}
		
		return marketGroupSet;

	}

	/**
	 * Make collection of all customer groups
	 * 
	 * @return the custGroupSet
	 */
	public Set<String> queryCustGroupSet() {
		printSelectedMarketGroup();
		
		custGroupSet.clear();
		custGroupSet.add("ALL CUSTOMER GROUPS");

//		custNumberSet.clear();
//		custNumberSet.add("ALL CUSTOMER NUMBERS");
		custNumberMap.clear();
		custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {
			// Run single statements one-by-one, OR...
			String tx;
			if (getSelectedMarketGroup() == null || (getSelectedCluster().equals("ALL CLUSTERS") && getSelectedMarketGroup().equals("ALL MARKET GROUPS"))) {
				tx = "MATCH (cg: CustGrp) RETURN distinct cg.name AS name ORDER BY name";
			}else if(getSelectedMarketGroup().equals("ALL MARKET GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cl.id = '" + getSelectedCluster()	+ "' RETURN distinct cg.name AS name ORDER BY name";
			}else {
				tx = "MATCH (cg: CustGrp)-[*1..3]-(mg: MarketGrp) WHERE mg.name = '" + getSelectedMarketGroup()	+ "' RETURN distinct cg.name AS name ORDER BY name";
			}
			 System.out.format("queryCustGroupSet, tx query text: %s", tx);
			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();
				String key = r.get("name").asString();
				custGroupSet.add(key);
			}

			System.out.printf("%s > Queried Customer group name set\n", LocalDateTime.now());
			System.out.printf("Size of custGroupSet is %s.\n", custGroupSet.size());
		} catch (ClientException e) {
			System.err.println("Exception in 'getCustGroupSet()':" + e);
		} finally {
			neoDbProvider.closeNeo4jDriver();
			// System.out.printf("size of custGroupSet is %s::\n",
			// custGroupSet.size());

		}

		return custGroupSet;

	}

	/**
	 * Make collection of all customer numbers
	 * 
	 * @return the custNumberSet
	 */
	/*public Set<String> queryCustNumberSet() {
		printSelectedCustGroup();
		custNumberSet.clear();
		custNumberSet.add("ALL CUSTOMER NUMBERS");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			// Run single statements one-by-one, OR...
			String tx;
			if (getSelectedCustGroup() == null || (getSelectedCluster().equals("ALL CLUSTERS") && getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS"))) {				
				tx = "MATCH (e:Entity) RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cl.id = '" + getSelectedCluster() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedCluster().equals("ALL CLUSTERS") && getSelectedMarketGroup().equals("ALL MARKET GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cg.name = '" + getSelectedCustGroup() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedMarketGroup().equals("ALL MARKET GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cl.id = '"	+ getSelectedCluster() + "' AND cg.name = '"	+ getSelectedCustGroup() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE mg.name = '" + getSelectedMarketGroup() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			} else {
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp) WHERE cg.name = '"
						+ getSelectedCustGroup() + "' AND mg.name = '" + getSelectedMarketGroup()
						+ "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}
			 System.out.format("queryCustNumberSet, tx query text is: %s", tx);
			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();
				 String key = r.get("ID").asString();
				 String name = r.get("name").asString();
//				  String compositeKey = r.get("compositeKey").asString();
				  custNumberSet.add(key);
				 
			}

			System.out.printf("%s > Queried Customer number set.\n", LocalDateTime.now());
			System.out.printf("Size of custNumberSet is %s.\n", custNumberSet.size());
		} catch (ClientException e) {
			System.err.println("Exception in 'getCustNumberSet()':" + e);
		} finally {
			neoDbProvider.closeNeo4jDriver();
		//	System.out.printf("size of CustNumberSet is %s::\n",
		//	custNumberSet.size());

		}

		return custNumberSet;
	} */
	
	public Map<String, String> queryCustNumberMap() {
		printSelectedCustGroup();
		custNumberMap.clear();
		custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

		// code query here
		try (Session session = NeoDbProvider.getDriver().session()) {

			// Run single statements one-by-one, OR...
			String tx;
			if (getSelectedCustGroup() == null || (getSelectedCluster().equals("ALL CLUSTERS") && getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS"))) {				
				tx = "MATCH (e:Entity) RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID LIMIT 0";
			}else if(getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cl.id = '" + getSelectedCluster() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedCluster().equals("ALL CLUSTERS") && getSelectedMarketGroup().equals("ALL MARKET GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cg.name = '" + getSelectedCustGroup() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedMarketGroup().equals("ALL MARKET GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE cl.id = '"	+ getSelectedCluster() + "' AND cg.name = '"	+ getSelectedCustGroup() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}else if(getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")){
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE mg.name = '" + getSelectedMarketGroup() + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			} else {
				tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(:Market)-[:IN]->(mg: MarketGrp) WHERE cg.name = '"
						+ getSelectedCustGroup() + "' AND mg.name = '" + getSelectedMarketGroup()
						+ "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
			}
			 System.out.format("queryCustNumberSet, tx query text is: %s", tx);
			StatementResult result = session.run(tx);

			while (result.hasNext()) {
				Record r = result.next();
				 String key = r.get("ID").asString();
				 String name = r.get("name").asString();
//				  String compositeKey = r.get("compositeKey").asString();
				  custNumberMap.put(key, name);
				 
			}

			System.out.printf("%s > Queried Customer number map.\n", LocalDateTime.now());
			System.out.printf("Size of custNumberMap is %s.\n", custNumberMap.size());
		} catch (ClientException e) {
			System.err.println("Exception in 'getCustNumberMap()':" + e);
		} finally {
			neoDbProvider.closeNeo4jDriver();
//			 System.out.printf("size of CustNumberMap is %s::\n",
//			 custNumberMap.size());

		}

		return custNumberMap;
	}

	/**
	 * @return the clusterSet
	 */
	public Set<String> getClusterSet() {
		return clusterSet;
	}

	/**
	 * @param clusterSet
	 *            the clusterSet to set
	 */
	public void setClusterSet(Set<String> clusterSet) {
		this.clusterSet = clusterSet;
	}

	/**
	 * @return the marketGroupSet
	 */
	public Set<String> getMarketGroupSet() {
		return marketGroupSet;
	}

	/**
	 * @param marketGroupSet
	 *            the marketGroupSet to set
	 */
	public void setMarketGroupSet(Set<String> marketGroupSet) {
		this.marketGroupSet = marketGroupSet;
	}

	/**
	 * @return the custNumberSet
	 */
	/*public Set<String> getCustNumberSet() {
		return custNumberSet;
	}*/

	/**
	 * @param custNumberSet
	 *            the custNumberSet to set
	 */
	/*public void setCustNumberSet(Set<String> custNumberSet) {
		this.custNumberSet = custNumberSet;
	}*/
	
	

	/**
	 * @return the custNumberMap
	 */
	public Map<String, String> getCustNumberMap() {
		return custNumberMap;
	}

	/**
	 * @param custNumberMap the custNumberMap to set
	 */
	public void setCustNumberMap(Map<String, String> custNumberMap) {
		this.custNumberMap = custNumberMap;
	}

	/**
	 * @return the custGroupSet
	 */
	public Set<String> getCustGroupSet() {
		return custGroupSet;
	}

	/**
	 * @param custGroupSet
	 *            the custGroupSet to set
	 */
	public void setCustGroupSet(Set<String> custGroupSet) {
		this.custGroupSet = custGroupSet;
	}

	/**
	 * @return the selectedCluster
	 */
	public String getSelectedCluster() {
		return selectedCluster;
	}

	/**
	 * @param selectedCluster
	 *            the selectedCluster to set
	 */
	public void setSelectedCluster(String selectedCluster) {
		this.selectedCluster = selectedCluster;
	}

	/**
	 * @return the selectedMarketGroup
	 */
	public String getSelectedMarketGroup() {
		return selectedMarketGroup;
	}

	/**
	 * @param selectedMarketGroup
	 *            the selectedMarketGroup to set
	 */
	public void setSelectedMarketGroup(String selectedMarketGroup) {
		this.selectedMarketGroup = selectedMarketGroup;
	}

	/**
	 * @return the selectedCustGroup
	 */
	public String getSelectedCustGroup() {
		return selectedCustGroup;
	}

	/**
	 * @param selectedCustGroup
	 *            the selectedCustGroup to set
	 */
	public void setSelectedCustGroup(String selectedCustGroup) {
		this.selectedCustGroup = selectedCustGroup;
	}

	/**
	 * @return the selectedCustNumber
	 */
	public String getSelectedCustNumber() {
		return selectedCustNumber;
	}

	/**
	 * @param selectedCustNumber
	 *            the selectedCustNumber to set
	 */
	public void setSelectedCustNumber(String selectedCustNumber) {
		this.selectedCustNumber = selectedCustNumber;
	}
	
	/**
	 * @return the selectedCustName
	 */
	public String getSelectedCustName() {
		return selectedCustName = custNumberMap.get(selectedCustNumber);
	}

	/**
	 * @param selectedCustName the selectedCustName to set
	 */
	public void setSelectedCustName(String selectedCustName) {
		this.selectedCustName = selectedCustName;
	}

	private void printSelectedMarketGroup() {
		System.out.format("This is the selectedMarketGroup: %s\n", selectedMarketGroup);
	}
	
	private void printSelectedCustGroup() {
		System.out.format("This is the selectedCustGroup: %s\n", selectedCustGroup);
	}
	

}
