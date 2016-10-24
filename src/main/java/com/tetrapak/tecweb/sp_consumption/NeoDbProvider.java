package com.tetrapak.tecweb.sp_consumption;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.ClientException;

/**
 * Session Bean implementation class NeoTest
 */
@Singleton
@LocalBean
public class NeoDbProvider {

	private static final String HOSTNAME = "localhost";
	// 'For most use cases it is recommended to use a single driver instance
	// throughout an application.'
	private static final Driver DRIVER = GraphDatabase.driver("bolt://" + HOSTNAME + "", AuthTokens.basic("neo4j", "Tokyo2000"));
//private static final Driver DRIVER = GraphDatabase.driver("bolt://" + HOSTNAME + "", AuthTokens.basic("neo4j", "s7asTaba"));

	/**
	 * Default constructor.
	 */
	public NeoDbProvider() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Checks that the db is running and lists indexes and constraints in use
	 */
	public static void checkDatabaseIsRunning() {
		try (Session session = getDriver().session()) {
			StatementResult result = session.run("CALL db.indexes()");
			System.out.printf("********* DB is running with indexes *********\nDescription\t State \t Type\n");
			while (result.hasNext()) {
				Record record = result.next();
				System.out.printf("%s\t %s\t %s\n", record.get(0), record.get(1), record.get(2));
			}
			StatementResult result1 = session.run("CALL db.constraints()");
			System.out.println("\n********* DB is running with constraints *********");
			while (result1.hasNext()) {
				Record record = result1.next();
				System.out.printf("%s\n", record.get(0));
			}
		} catch (ClientException e) {
			System.err.println("Exception in 'checkDatabaseIsRunning()':" + e + "\nProgram exits[1]...");
			System.exit(1);
		}
	}

	/**
	 * @return the driver
	 */
	public static Driver getDriver() {
		return DRIVER;
	}

	/**
	 * Close the DB driver
	 */
	public void closeNeo4jDriver() {
		DRIVER.close();
	}

	/**
	 * @param args
	 *            the command line arguments
	 */

}
