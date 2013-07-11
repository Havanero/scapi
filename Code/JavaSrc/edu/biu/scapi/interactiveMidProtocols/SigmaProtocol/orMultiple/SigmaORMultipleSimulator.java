/**
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
* Copyright (c) 2012 - SCAPI (http://crypto.biu.ac.il/scapi)
* This file is part of the SCAPI project.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
* to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
* FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* We request that any publication and/or code referring to and/or based on SCAPI contain an appropriate citation to SCAPI, including a reference to
* http://crypto.biu.ac.il/SCAPI.
* 
* SCAPI uses Crypto++, Miracl, NTL and Bouncy Castle. Please see these projects for any further licensing issues.
* %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
* 
*/
package edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.orMultiple;

import java.security.SecureRandom;
import java.util.ArrayList;

import edu.biu.scapi.exceptions.CheatAttemptException;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.SigmaSimulator;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaMultipleMsg;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaProtocolInput;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaProtocolMsg;
import edu.biu.scapi.interactiveMidProtocols.SigmaProtocol.utility.SigmaSimulatorOutput;

/**
 * Concrete implementation of Sigma Simulator.
 * This implementation simulates the case that the prover convince a verifier that at least k out of n 
 * statements is true, where each statement can be proven by an associated Sigma protocol.
 * 
 * @author Cryptography and Computer Security Research Group Department of Computer Science Bar-Ilan University (Moriya Farbstein)
 *
 */
public class SigmaORMultipleSimulator implements SigmaSimulator{

	/*	
	  This class computes the following calculations:
		  	SAMPLE random points e1,�,en-k in GF[2t].
			COMPUTE the polynomial Q and values en-k+1,�,en like in the protocol.
			RUN the simulator on each statement/challenge pair (xi,ei) for all i=1,�,n to obtain (ai,ei,zi).
			OUTPUT (a1,e1,z1),�, (an,en,zn).
	*/
	
	private ArrayList<SigmaSimulator> simulators;	//underlying simulators.
	private int t;									// Soundness parameter.
	private SecureRandom random;
	int len;										// number of underlying simulators.
	
	//Initiaize the field GF2E with a random irreducible polynomial with degree t.
	private native void initField(int t, int seed);
	
	//Creates random field elements to be the challenges.
	private native byte[][] createRandomFieldElements(int numElements, long[] fieldElements);
	
	//Interpolate the points to get a polynoial.
	private native long interpolate(byte[] e, long[] fieldElements, int[] indexesNotInI);
	
	//Calculate the challenges for the statements with the widnesses.
	private native byte[][] getRestChallenges(long polynomial, int start, int end, int[] indexesInI);
	
	//return the byteArray of the polynomial coefficients.
	private native byte[][] getPolynomialBytes(long polynomial);
	
	//delete the allocated memory of the polynomial and the field elements.
	private native void deletePointers(long polynomial, long[] fieldElements);
	
	/**
	 * Constructor that gets the underlying simulators.
	 * @param simulators array of SigmaSimulator that contains underlying simulators.
	 * @param t soundness parameter. t MUST be equal to both t values of the underlying simulators object.
	 * @param random
	 */
	public SigmaORMultipleSimulator(ArrayList<SigmaSimulator> simulators, int t, SecureRandom random){
		len = simulators.size();
		
		//If the given t is different from one of the underlying object's t values, throw exception.
		for (int i = 0; i < len; i++){
			if (t != simulators.get(i).getSoundness()){
				throw new IllegalArgumentException("the given t does not equal to one of the t values in the underlying simulators objects.");
			}
		}
		this.simulators = simulators;
		this.t = t; 
		initField(t, random.nextInt());
	}

	/**
	 * Returns the soundness parameter for this Sigma protocol.
	 * @return t soundness parameter
	 */
	public int getSoundness(){
		return t;
	}
	
	/**
	 * Computes the simulator computation.
	 * @param input MUST be an instance of SigmaORMultipleInput.
	 * @param challenge
	 * @return the output of the computation - (a, e, z).
	 * @throws CheatAttemptException if the received challenge's length is not equal to the soundness parameter.
	 * @throws IllegalArgumentException if the given input is not an instance of SigmaORMultipleInput.
	 */
	public SigmaSimulatorOutput simulate(SigmaProtocolInput input, byte[] challenge) throws CheatAttemptException{
		if (!checkChallengeLength(challenge)){
			throw new CheatAttemptException("the length of the given challenge is differ from the soundness parameter");
		}
		
		if (!(input instanceof SigmaORMultipleInput)){
			throw new IllegalArgumentException("the given input must be an instance of SigmaORMultipleInput");
		}
		SigmaORMultipleInput orInput = (SigmaORMultipleInput) input;
		
		int nMinusK = len - orInput.getK();
		long[] fieldElements = new long[nMinusK];
		//For every j = 1 to n-k, SAMPLE a random element ej <- GF[2^t]. We sample the random elments at once.
		byte[][] ejs = createRandomFieldElements(nMinusK, fieldElements);

		byte[][] challenges = new byte[len][];
		//byte[][] challenges = new byte[len-orInput.k][];
		//Set the created challenges to the challenges array in the first n-k indexes.
		for (int i=0; i<nMinusK; i++){
			challenges[i] = alignToT(ejs[i]);
		}
		
		//Create two arrays. This arrays used for calculate the interpolated polynomial.
		int[] indexesNotInI= new int[nMinusK];
		int[] indexesInI= new int[orInput.getK()];
		int indexNotInI = 0;
		int indexInI = 0;
		//Fill the arrays with the indexes.
		for (int i = 0; i < len; i++){
			//If i in I, call the underlying computeFirstMsg.
			if (i<len-orInput.getK()){
				indexesNotInI[indexNotInI++] = i+1;
			} else {
				indexesInI[indexInI++] = i+1;
			}
		}
		//Interpolate the points (0,e) and {(j,ej)} for every j=1 to n-k to obtain a degree n-k polynomial Q.
		long polynomial = interpolate(challenge, fieldElements, indexesNotInI);
				
		//Get the rest of the challenges by computing for every i = n-k+1 to n, ei = Q(i).
		byte[][] jsInI = getRestChallenges(polynomial, nMinusK, len, indexesInI);
		for(int i=nMinusK, j=0; i<len; i++, j++){
			challenges[i] = alignToT(jsInI[j]);
		}
		
		ArrayList<SigmaProtocolMsg> aOutputs = new ArrayList<SigmaProtocolMsg>();
		ArrayList<byte[]> eOutputs = new ArrayList<byte[]>();
		ArrayList<SigmaProtocolMsg> zOutputs = new ArrayList<SigmaProtocolMsg>();
		
		//Run the simulator on each statement,challenge pair (xi,ei) for all i=1,�,n to obtain (ai,ei,zi).
		for (int i = 0; i < len; i++){
		//for (int i = 0; i < nMinusK; i++){
			try {
				SigmaSimulatorOutput output = simulators.get(i).simulate(orInput.getInputs().get(i), challenges[i]);
				aOutputs.add(output.getA());
				eOutputs.add(output.getE());
				zOutputs.add(output.getZ());
			} catch (CheatAttemptException e) {
				// This exception will not be thrown because the length of the challenges is valid.
			}
		}
		
		//prepare the input for the sigmaSimulatorOutput.
		byte[][] polynomBytes = getPolynomialBytes(polynomial);
		SigmaMultipleMsg first = new SigmaMultipleMsg(aOutputs);
		SigmaORMultipleSecondMsg second = new SigmaORMultipleSecondMsg(polynomBytes, zOutputs, challenges);
		
		//Delete the allocated memory.
		deletePointers(polynomial, fieldElements);
		
		return new SigmaORMultipleSimulatorOutput(first, challenge, second);
	}
	
	/**
	 * Align the given array to t length. Adds zeros in the beginning.
	 * @param array to align
	 * @return the aligned array.
	 */
	private byte[] alignToT(byte[] array) {
		byte[] alignArr = new byte[t/8];
		int len = array.length;
		if (len < t/8){
			int diff = t/8 - len;
			int index = 0;
			for (int i=0; i<len; i++){
				alignArr[index++] = array[i];
			}
			for (int i=0; i<diff; i++){
				alignArr[index++] = 0;
			}
		} else{
			alignArr = array;
		}
		return alignArr;
		
	}
	
	/**
	 * Computes the simulator computation.
	 * @param input MUST be an instance of SigmaORMultipleInput.
	 * @return the output of the computation - (a, e, z).
	 * @throws IllegalArgumentException if the given input is not an instance of SigmaORMultipleInput.
	 */
	public SigmaSimulatorOutput simulate(SigmaProtocolInput input){
		//Create a new byte array of size t/8, to get the required byte size.
		byte[] e = new byte[t/8];
		//Fill the byte array with random values.
		random.nextBytes(e);
		//Call the other simulate function with the given input and the samples e.
		try {
			return simulate(input, e);
		} catch (CheatAttemptException e1) {
			//will not occur since the challenge length is valid.
		}
		return null;
	}
	
	/**
	 * Checks if the given challenge length is equal to the soundness parameter.
	 * @return true if the challenge length is t; false, otherwise. 
	 */
	private boolean checkChallengeLength(byte[] challenge){
		//If the challenge's length is equal to t, return true. else, return false.
		return (challenge.length == (t/8) ? true : false);
	}
}