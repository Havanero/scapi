package edu.biu.scapi.primitives.dlog.bc;

import java.math.BigInteger;

import org.bouncycastle.math.ec.ECPoint;

import edu.biu.scapi.primitives.dlog.ECElement;
import edu.biu.scapi.primitives.dlog.groupParams.ECGroupParams;

/**
 * This class is an adapter for BC point.
 * @author Moriya
 *
 */
public abstract class ECPointBc implements ECElement{

	protected ECPoint point = null;
	
	public ECPoint getPoint(){
		return point;
	}

	abstract boolean checkValidity(BigInteger bigInteger, BigInteger bigInteger2,
			ECGroupParams groupParams);
	
}