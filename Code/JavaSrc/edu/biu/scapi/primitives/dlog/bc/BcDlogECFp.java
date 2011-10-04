package edu.biu.scapi.primitives.dlog.bc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Properties;
import java.util.logging.Level;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import edu.biu.scapi.generals.Logging;
import edu.biu.scapi.primitives.dlog.DlogECFp;
import edu.biu.scapi.primitives.dlog.ECElement;
import edu.biu.scapi.primitives.dlog.GroupElement;
import edu.biu.scapi.primitives.dlog.groupParams.ECFpGroupParams;

/**
 * 
 * @author Moriya
 *
 */
public class BcDlogECFp extends BcAdapterDlogEC implements DlogECFp{
	
	
	/**
	 * Initialize the DlogGroup with one of NIST recommended elliptic curve
	 * @param curveName - name of NIST curve to initialized
	 * @throws IllegalAccessException
	 */
	public void init(String curveName) throws IllegalArgumentException{
		
		try {
			Properties ecProperties = getProperties(PROPERTIES_FILES_PATH); //get properties object containing the curve data
		
			//checks that the curveName is in the file
			if(!ecProperties.containsKey(curveName)) { 
				throw new IllegalArgumentException("no such NIST elliptic curve"); 
			} 
			
			//check that the given curve is in the field that matches the group
			if (!curveName.startsWith("P-")){
				throw new IllegalArgumentException("curveName is not a curve over Fp field and doesn't match the DlogGroup type"); 
			}
			
			doInit(ecProperties, curveName);  // set the data and initialize the curve
			isInitialized = true; 
			
		} catch (IOException e) {
			Logging.getLogger().log(Level.WARNING, "error while loading the NIST elliptic curves file");
		}
	}
	
	/**
	 * Extracts the parameters of the curve from the properties object and initialize the groupParams, 
	 * generator and the underlying curve
	 * @param ecProperties - properties object contains the curve file data
	 * @param curveName - the curve name as it called in the file
	 */
	protected void doInit(Properties ecProperties, String curveName){
		
		//get the nist parameters
		BigInteger p = new BigInteger(ecProperties.getProperty(curveName));
		BigInteger a = new BigInteger(ecProperties.getProperty(curveName+"a"));
		BigInteger b = new BigInteger(1,Hex.decode(ecProperties.getProperty(curveName+"b")));
		BigInteger x = new BigInteger(1,Hex.decode(ecProperties.getProperty(curveName+"x")));
		BigInteger y = new BigInteger(1,Hex.decode(ecProperties.getProperty(curveName+"y")));
		BigInteger q = new BigInteger(ecProperties.getProperty(curveName+"r"));
		
		//create the GroupParams
		groupParams = new ECFpGroupParams(q, x, y, p, a, b);
		
		//create the ECCurve
		curve = new ECCurve.Fp(p, a, b);
		
		//create the generator
		generator = new ECFpPointBc(x,y, this);	
	}
	
	/**
	 * Create a random member of that Dlog group
	 * @return the random element
	 */
	public GroupElement getRandomElement(){
		return new ECFpPointBc(this);
	}
	 
	/**
	 * Creates a point over Fp field. 
	 * @return the created point
	 */
	public ECElement getElement(BigInteger x, BigInteger y){
		return new ECFpPointBc(x, y, this);
	}
	
	/**
	 * Creates ECPoint.Fp with the given parameters
	 */
	protected GroupElement createPoint(ECPoint result) {
		return new ECFpPointBc(result);
	}
	
}