package edu.biu.scapi.primitives.dlog.groupParams;


import java.math.BigInteger;

public class ECFpGroupParams extends ECGroupParams{

	private BigInteger p; //modulus 
	
	/**
	 * Sets p, a, b parameters
	 * @param p
	 * @param a
	 * @param b
	 */
	public ECFpGroupParams(BigInteger q, BigInteger xG, BigInteger yG, BigInteger p, BigInteger a, BigInteger b) {
		this.q = q;
		this.xG = xG;
		this.yG = yG;
		this.a = a;
		this.b = b;
		this.p = p;
	}
	
	/**
	 * 
	 * @return p
	 */
	public BigInteger getP(){
		return p;
	}
}