/**
 * PerfectUniversalFactory has a member of type FactoriesUtility to which it delegates the actual creation of the object. 
 * This ensures proper code re-use.The 
 * factories have two getObject methods to retrieve an algorithm compatible with the type; 
 * one specifies the provider and the other one relies on a default provider.
 */
package edu.biu.scapi.tools.Factories;


import edu.biu.scapi.exceptions.FactoriesException;
import edu.biu.scapi.primitives.perfectUniversalHash.PerfectUniversalHash;

/** 
 * @author LabTest
 */
public final class PerfectUniversalFactory {
	private FactoriesUtility factoriesUtility;
	private static PerfectUniversalFactory instance = new PerfectUniversalFactory();

	
	/**
	 * Private constructor since this class is of the singleton pattern. 
	 * It creates an instance of FactoriesUtility and passes a predefined file names to the constructor
	 * of FactoriesUtility.
	 * 
	 */
	private PerfectUniversalFactory() {

		//create an instance of FactoriesUtility with the predefined file names.
		factoriesUtility = new FactoriesUtility("PerfectUniversalDefault.properties", "PerfectUniversal.properties");
		
	}
	
	/** 
	 * @param provider the required provider name
	 * @param algName the required algorithm name
	 * @return an object of type perfectUniversalHash class that was determined by the algName + provider
	 * @throws FactoriesException 
	 */
	public Object getObject(String algName, String provider) throws FactoriesException {
		
		return (PerfectUniversalHash) factoriesUtility.getObject(algName, provider);
	}

	/** 
	 * 
	 * @param algName the required algorithm name
	 * @return an object of type perfectUniversalHash class that was determined by the algName + the default provider for that algorithm.
	 * @throws FactoriesException 
	 */public Object getObject(String algName) throws FactoriesException {
		
		return (PerfectUniversalHash) factoriesUtility.getObject(algName);
	}

	/** 
	 * @return the singleton instance.
	 */
	public static PerfectUniversalFactory getInstance() {
		return instance;
	}
}