package main.java.com.tetrapak.tecweb.sp_consumption;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author SEPALMM
 *
 * This is a set of customers to select from.
 */
@Named
@Stateful
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
@DeclareRoles({"CENTRAL_TEAM", "TPPC", "BUIC", "ECA", "GC", "GMEA", "NCSA", "SAEAO"})

public class CustomerSetBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Resource
    SessionContext ctx;

    @Inject
    NeoDbProvider neoDbProvider;

    private Set<String> clusterSet;
    private Set<String> marketGroupSet;
    private Set<String> marketSet;
    private Set<String> custGroupSet;
    private Map<String, String> custNumberMap;

    private String selectedCluster = "";
    private String selectedMarketGroup = "";
    private String selectedMarket = "";
    private String selectedCustGroup = "";

    private String[] selectedCustomers; // From jsf page, user selections
    private String[] selectedIDs;
    private String[] selectedCustNames;
    private String selectedIDsString;
    private String selectedCustNamesString;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerSetBean.class);

    private boolean isCentralTeamUser;
    private boolean isTPPC_User;
    private boolean isBUIC_User;
    private boolean isECA_User;
    private boolean isGC_User;
    private boolean isGMEA_User;
    private boolean isNCSA_User;
    private boolean isSAEAO_User;

    private boolean renderTPPC;
    private boolean renderBUIC;

    // Creates a new instance of CustomerListBean
    public CustomerSetBean() {

    }

    @PostConstruct
    public void init() {
        clusterSet = new LinkedHashSet<>();
        marketGroupSet = new LinkedHashSet<>();
        marketSet = new LinkedHashSet<>();
        custGroupSet = new LinkedHashSet<>();
        custNumberMap = new LinkedHashMap<>();

        marketGroupSet.add("ALL MARKET GROUPS");
        marketSet.add("ALL MARKETS");
        custGroupSet.add("ALL CUSTOMER GROUPS");
        custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

        // Initiate user group classifiers
        isCentralTeamUser();
        isTPPC_User();
        isBUIC_User();
        isECA_User();
        isGC_User();
        isGMEA_User();
        isNCSA_User();
        isSAEAO_User();

        // Initiate rendering of KC selections
        isRenderTPPC();
        isRenderBUIC();

        // Initiate selection filtering
        queryClusterSet();
        queryMarketGroupSet();
        queryMarketSet();
        queryCustGroupSet();
        queryCustNumberMap();

    }

    /**
     * Make collection of all Clusters
     *
     * @return the clusterSet
     */
    @RolesAllowed({"CENTRAL_TEAM", "TPPC", "BUIC", "ECA", "GC", "GMEA", "NCSA", "SAEAO"})
    public Set<String> queryClusterSet() {

        marketGroupSet.clear();
        marketGroupSet.add("ALL MARKET GROUPS");

        marketSet.clear();
        marketSet.add("ALL MARKETS");

        custGroupSet.clear();
        custGroupSet.add("ALL CUSTOMER GROUPS");

        custNumberMap.clear();
        custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            String tx = null;
            StatementResult result = null;
            if (isCentralTeamUser || isTPPC_User || isBUIC_User) {
                clusterSet.add("ALL CLUSTERS");
                tx = "MATCH (c:Cluster) RETURN distinct c.id AS name ORDER BY name";
                // System.out.format("tx query text: %s", tx);
                result = session.run(tx);
                while (result.hasNext()) {
                    Record r = result.next();
                    String key = r.get("name").asString();
                    clusterSet.add(key);
                }
            }
            if (isECA_User) {
                clusterSet.add("E&CA");
            }
            if (isGC_User) {
                clusterSet.add("GC");
            }
            if (isGMEA_User) {
                clusterSet.add("GME&A");
            }
            if (isNCSA_User) {
                clusterSet.add("NC&SA");
            }
            if (isSAEAO_User) {
                clusterSet.add("SAEA&O");
            }

            System.out.printf("%s > Queried Cluster name set\n", LocalDateTime.now());
            System.out.printf("Size of clusterSet is %s.\n", clusterSet.size());
        } catch (ClientException e) {
            System.err.println("Exception in 'queryClusterSet()':" + e);
        } finally {
            //	neoDbProvider.closeNeo4jDriver();
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
        printSelectedCluster();
        marketGroupSet.clear();
        marketGroupSet.add("ALL MARKET GROUPS");

        marketSet.clear();
        marketSet.add("ALL MARKETS");

        custGroupSet.clear();
        custGroupSet.add("ALL CUSTOMER GROUPS");

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
            //	neoDbProvider.closeNeo4jDriver();
            // System.out.printf("size of marketGroupSet is %s::\n",
            // marketGroupSet.size());

        }

        return marketGroupSet;

    }

    /**
     * Make collection of all Markets
     *
     * @return the marketSet
     */
    public Set<String> queryMarketSet() {
        printSelectedMarketGroup();
        marketSet.clear();
        marketSet.add("ALL MARKETS");

        custGroupSet.clear();
        custGroupSet.add("ALL CUSTOMER GROUPS");

        custNumberMap.clear();
        custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

        // code query here
        // modeled according to
        // https://www.mathsisfun.com/combinatorics/combinations-permutations-calculator.html
        try (Session session = NeoDbProvider.getDriver().session()) {

            // Run single statements one-by-one, OR...
            String tx;
            if (getSelectedMarketGroup() == null || (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS"))) {
                tx = "MATCH (m: Market) RETURN distinct m.name AS name ORDER BY name";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")) {
                tx = "MATCH (m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE " + "cl.id = '"
                        + getSelectedCluster() + "'" + " RETURN distinct m.name AS name ORDER BY name";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")) {
                tx = "MATCH (m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE " + " mg.name = '"
                        + getSelectedMarketGroup() + "'" + " RETURN distinct m.name AS name ORDER BY name";
            } else {
                tx = "MATCH (m: Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE " + "cl.id = '"
                        + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup() + "'"
                        + " RETURN distinct m.name AS name ORDER BY name";
            }

            System.out.format("queryMarketSet, tx query text: %s", tx);
            StatementResult result = session.run(tx);

            while (result.hasNext()) {
                Record r = result.next();
                String key = r.get("name").asString();
                marketSet.add(key);
            }

            System.out.printf("%s > Queried Market name set\n", LocalDateTime.now());
            System.out.printf("Size of marketSet is %s.\n", marketSet.size());
        } catch (ClientException e) {
            System.err.println("Exception in 'queryMarketSet()':" + e);
            System.out.printf("Size of marketSet is %s.\n", marketSet.size());
        } finally {
            // System.out.printf("size of marketSet is %s::\n",
            // marketSet.size());

        }

        return marketSet;

    }

    /**
     * Make collection of all customer groups
     *
     * @return the custGroupSet
     */
    public Set<String> queryCustGroupSet() {
        printSelectedMarket();

        custGroupSet.clear();
        custGroupSet.add("ALL CUSTOMER GROUPS");

        custNumberMap.clear();
        custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {
            // Run single statements one-by-one, OR...
            String tx;
            if (getSelectedMarketGroup() == null || (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS"))) {
                tx = "MATCH (cg: CustGrp) RETURN distinct cg.name AS name ORDER BY name";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup() + "'"
                        + " RETURN distinct cg.name AS name ORDER BY name";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND m.name = '" + getSelectedMarket() + "'"
                        + " RETURN distinct cg.name AS name ORDER BY name";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "'" + " RETURN distinct cg.name AS name ORDER BY name";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " mg.name = '" + getSelectedMarketGroup() + "' AND m.name = '" + getSelectedMarket() + "'"
                        + " RETURN distinct cg.name AS name ORDER BY name";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " mg.name = '" + getSelectedMarketGroup() + "'"
                        + " RETURN distinct cg.name AS name ORDER BY name";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " m.name = '" + getSelectedMarket() + "'" + " RETURN distinct cg.name AS name ORDER BY name";
            } else {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup()
                        + "' AND m.name = '" + getSelectedMarket() + "'"
                        + " RETURN distinct cg.name AS name ORDER BY name";
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
            //	neoDbProvider.closeNeo4jDriver();
            // System.out.printf("size of custGroupSet is %s::\n",
            // custGroupSet.size());

        }

        return custGroupSet;

    }

    public Map<String, String> queryCustNumberMap() {
        printSelectedCustGroup();
        custNumberMap.clear();
        custNumberMap.put("ALL CUSTOMER NUMBERS", "ALL CUSTOMER NUMBERS");

        // code query here
        try (Session session = NeoDbProvider.getDriver().session()) {

            // Run single statements one-by-one, OR...
            String tx;
            if (getSelectedCustGroup() == null || (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS"))) {
                tx = "MATCH (e:Entity) RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID LIMIT 0";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup()
                        + "' AND m.name = '" + getSelectedMarket()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";

            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup()
                        + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND m.name = '" + getSelectedMarket()
                        + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND m.name = '" + getSelectedMarket()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (!getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " mg.name = '" + getSelectedMarketGroup() + "' AND m.name = '" + getSelectedMarket()
                        + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " mg.name = '" + getSelectedMarketGroup() + "' AND m.name = '" + getSelectedMarket()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " mg.name = '" + getSelectedMarketGroup() + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && !getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " mg.name = '" + getSelectedMarketGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " m.name = '" + getSelectedMarket() + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS")
                    && !getSelectedMarket().equals("ALL MARKETS")
                    && getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " m.name = '" + getSelectedMarket()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else if (getSelectedCluster().equals("ALL CLUSTERS")
                    && getSelectedMarketGroup().equals("ALL MARKET GROUPS") && getSelectedMarket().equals("ALL MARKETS")
                    && !getSelectedCustGroup().equals("ALL CUSTOMER GROUPS")) {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            } else {
                tx = "MATCH (cg: CustGrp)<-[:IN]-(e: Entity)-[:LINKED]->(m :Market)-[:IN]->(mg: MarketGrp)-[:IN]->(cl: Cluster) WHERE"
                        + " cl.id = '" + getSelectedCluster() + "' AND mg.name = '" + getSelectedMarketGroup()
                        + "' AND m.name = '" + getSelectedMarket() + "' AND cg.name = '" + getSelectedCustGroup()
                        + "' RETURN distinct e.id AS ID, e.name AS name, (e.id + \" (\" + e.name + \")\") AS compositeKey ORDER BY ID";
            }

            System.out.format("queryCustNumberSet, tx query text is: %s", tx);
            StatementResult result = session.run(tx);

            while (result.hasNext()) {
                Record r = result.next();
                String key = r.get("ID").asString();
                String name = r.get("name").asString();
                String compositeKey = r.get("compositeKey").asString();
                // custNumberMap.put(key, name);
                custNumberMap.put(compositeKey, name);

            }

            System.out.printf("%s > Queried Customer number map.\n", LocalDateTime.now());
            System.out.printf("Size of custNumberMap is %s.\n", custNumberMap.size());
        } catch (ClientException e) {
            System.err.println("Exception in 'getCustNumberMap()':" + e);
        } finally {
            //	neoDbProvider.closeNeo4jDriver();
            // System.out.printf("size of CustNumberMap is %s::\n",
            // custNumberMap.size());

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
     * @param clusterSet the clusterSet to set
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
     * @param marketGroupSet the marketGroupSet to set
     */
    public void setMarketGroupSet(Set<String> marketGroupSet) {
        this.marketGroupSet = marketGroupSet;
    }

    /**
     * @return the marketSet
     */
    public Set<String> getMarketSet() {
        return marketSet;
    }

    /**
     * @param marketSet the marketSet to set
     */
    public void setMarketSet(Set<String> marketSet) {
        this.marketSet = marketSet;
    }

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
     * @param custGroupSet the custGroupSet to set
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
     * @param selectedCluster the selectedCluster to set
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
     * @param selectedMarketGroup the selectedMarketGroup to set
     */
    public void setSelectedMarketGroup(String selectedMarketGroup) {
        this.selectedMarketGroup = selectedMarketGroup;
    }

    /**
     * @return the selectedMarket
     */
    public String getSelectedMarket() {
        return selectedMarket;
    }

    /**
     * @param selectedMarket the selectedMarket to set
     */
    public void setSelectedMarket(String selectedMarket) {
        this.selectedMarket = selectedMarket;
    }

    /**
     * @return the selectedCustGroup
     */
    public String getSelectedCustGroup() {
        return selectedCustGroup;
    }

    /**
     * @param selectedCustGroup the selectedCustGroup to set
     */
    public void setSelectedCustGroup(String selectedCustGroup) {
        this.selectedCustGroup = selectedCustGroup;
    }

    /**
     * Collect IDs of selected customers to string array to be used in a Cypher
     * statement. Called by user from jsf.
     */
    public void collectSelectedCustomers() {
        try {
            String custKey = "";
            boolean foundACN = false;
            int numberOfSelectedCustomers = selectedCustomers.length;
            selectedIDs = new String[numberOfSelectedCustomers];
            selectedCustNames = new String[numberOfSelectedCustomers];
            if (numberOfSelectedCustomers > 0) {

                // break loop if user selects combination containing 
                // "ALL CUSTOMER NUMBERS"
                for (int i = 0; i < selectedCustomers.length; i++) {
                    custKey = selectedCustomers[i];
                    if (custKey.equals("ALL CUSTOMER NUMBERS")) {
                        // Prepare to break the loop :)
                        selectedIDs = new String[1];
                        selectedIDs[0] = custKey;
                        selectedCustNames = new String[1];
                        selectedCustNames[0] = custKey;
                        foundACN = true;
                        break;
                    } else if (!foundACN) {
                        // Add user selections to array
//                      String id = custNumberMap.get(selectedCustomers[i]);
                        custKey = selectedCustomers[i];
                        LOGGER.info("Selected customer: {}", custKey);
                        // Remove all letters from composite key, 
                        // and keep the customer numbers
                        selectedIDs[i] = custKey.replaceAll("[^0-9]", "");
                        selectedCustNames[i] = custKey.replaceAll("[0-9]", "");
                    }
                }

                // Join strings from arrays for jsf output text
                StringJoiner sj1 = new StringJoiner("; ", "[", "]");
                for (String id : selectedIDs) {
                    sj1.add(id.trim());
                }
                if (sj1.toString().length() > 100) {
                    selectedIDsString = sj1.toString().substring(0, 101).concat("...");
                } else {
                    selectedIDsString = sj1.toString();
                }

                StringJoiner sj2 = new StringJoiner("; ", "[", "]");
                for (String id : selectedCustNames) {
                    sj2.add(id.replaceAll("\\s*\\(", "").replaceAll("\\)", ""));
                }
                if (sj2.toString().length() > 100) {
                    selectedCustNamesString = sj2.toString().substring(0, 101).concat("...");
                } else {
                    selectedCustNamesString = sj2.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not collect CustomerIDs to text array. "
                    + "Error message: {}", e.getMessage());
        }

    }

    public String[] getSelectedCustomers() {
        return selectedCustomers;
    }

    public void setSelectedCustomers(String[] selectedCustomers) {
        this.selectedCustomers = selectedCustomers;
    }

    /**
     *
     * @return selected customer IDs (customer numbers)
     */
    public String[] getSelectedIDs() {
        return selectedIDs;
    }

    public void setSelectedIDs(String[] selectedIDs) {
        this.selectedIDs = selectedIDs;
    }

    public String getSelectedIDsString() {
        return selectedIDsString;
    }

    public void setSelectedIDsString(String selectedIDsString) {
        this.selectedIDsString = selectedIDsString;
    }

    public String getSelectedCustNamesString() {
        return selectedCustNamesString;
    }

    public void setSelectedCustNamesString(String selectedCustNamesString) {
        this.selectedCustNamesString = selectedCustNamesString;
    }

    private void printSelectedCluster() {
        System.out.format("This is the selectedCluster: %s\n", selectedCluster);
    }

    private void printSelectedMarketGroup() {
        System.out.format("This is the selectedMarketGroup: %s\n", selectedMarketGroup);
    }

    private void printSelectedMarket() {
        System.out.format("This is the selectedMarket: %s\n", selectedMarket);
    }

    private void printSelectedCustGroup() {
        System.out.format("This is the selectedCustGroup: %s\n", selectedCustGroup);
    }

    /**
     * @return the isCentralTeamUser
     */
    public boolean isCentralTeamUser() {
        return isCentralTeamUser = ctx.isCallerInRole("CENTRAL_TEAM");
    }

    /**
     * @return the isTPPC_User
     */
    public boolean isTPPC_User() {
        return isTPPC_User = ctx.isCallerInRole("TPPC");
    }

    /**
     * @return the isBUIC_User
     */
    public boolean isBUIC_User() {
        return isBUIC_User = ctx.isCallerInRole("BUIC");
    }

    /**
     * @return the isECA_User
     */
    public boolean isECA_User() {
        return isECA_User = ctx.isCallerInRole("ECA");
    }

    /**
     * @return the isGC_User
     */
    public boolean isGC_User() {
        return isGC_User = ctx.isCallerInRole("GC");
    }

    /**
     * @return the isGMEA_User
     */
    public boolean isGMEA_User() {
        return isGMEA_User = ctx.isCallerInRole("GMEA");
    }

    /**
     * @return the isNCSA_User
     */
    public boolean isNCSA_User() {
        return isNCSA_User = ctx.isCallerInRole("NCSA");
    }

    /**
     * @return the isSAEAO_User
     */
    public boolean isSAEAO_User() {
        return isSAEAO_User = ctx.isCallerInRole("SAEAO");
    }

    /**
     * @return the renderTPPC
     */
    public boolean isRenderTPPC() {
        if (isCentralTeamUser || isTPPC_User || isECA_User || isGC_User || isGMEA_User || isNCSA_User || isSAEAO_User) {
            renderTPPC = true;
        }
        return renderTPPC;
    }

    /**
     * @return the renderBUIC
     */
    public boolean isRenderBUIC() {
        if (isCentralTeamUser || isBUIC_User || isECA_User || isGC_User || isGMEA_User || isNCSA_User || isSAEAO_User) {
            renderBUIC = true;
        }
        return renderBUIC;
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

}
