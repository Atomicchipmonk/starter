package com.glados.villagevehicle.backend;

import java.lang.reflect.Array;
import java.util.Arrays;

import android.util.Log;

/*
 * Copyright 2005, Nick Galbreath -- nickg [at] modp [dot] com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *   Neither the name of the modp.com nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This is the standard "new" BSD license:
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * Portions may also be
 * Copyright (C) 2004, Makoto Matsumoto and Takuji Nishimura,
 * All rights reserved.
 * (and covered under the BSD license)
 * See http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/VERSIONS/C-LANG/mt19937-64.c
 */

/**
 * MerseTWIST_NUMBERSe Twister 64-bit.
 *
 * <p>Similar to the regular MerseTWIST_NUMBERSe Twister but is implemented use
 * 64-bit registers (Java <code>long</code>) and produces
 * different output.
 * </p>
 * <p>This is mostly a straight port of the
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/VERSIONS/C-LANG/mt19937-64.c">
 * C-code (mt19937-64.c)</a>,
 * but made more "java-like".
 * </p>
 * <p>
 * References:
 * </p>
 * <ul>
 * <li>
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html">
 * The MerseTWIST_NUMBERSe Twister Home Page </a>
 * </li>
 * <li>  Makato Matsumoto and Takuji Nishimura,
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/ARTICLES/mt.pdf">"MerseTWIST_NUMBERSe Twister:A 623-Dimensionally Equidistributed Uniform Pseudo-Random Number Generator"</a>,
 * <i>ACM Transactions on Modeling and Computer Simulation, </i> Vol. 8, No. 1,
 * January 1998, pp 3--30.</li>
 * </ul>
 *
 * @author Nick Galbreath -- nickg [at] modp [dot] com
 * @version 1 -- 06-Jul-2005
 */
public class MersenneTwister64{
	
	public static final String TAG = "MT64";
	
    public static final int TWIST_NUMBERS = 200;

    private static final int MM = 156;

    private static final long MATRIX_A = 0xB5026F5AA96619E9L;

    /**
     * Mask: Most significant 33 bits
     */
    private static final long UM = 0xFFFFFFFF80000000L;

    /**
     * Mask: Least significant 31 bits
     */
    private static final long LM = 0x7FFFFFFFL;

    private static final long[] mag01 = { 0L, MATRIX_A };

    private long[] mt = new long[TWIST_NUMBERS];

    private int mti = TWIST_NUMBERS + 1;

    /**
     * Internal to hold 64 bits, that might
     * used to generate two 32 bit values.
     */
    private long bits;

    /**
     *
     */
    private boolean bitState = true;

    /**
     *
     */
    public MersenneTwister64() {
        setSeed(System.currentTimeMillis());
    }

    public MersenneTwister64(final long seed) {
        setSeed(seed);
    }

    public MersenneTwister64(final long[] ary) {
        setSeed(ary);
    }

    /**
     * Initalize the pseudo random number generator with 32-bits.
     */
    public void setSeed(final long seed) {
        mt[0] = seed;
        for (mti = 1; mti < TWIST_NUMBERS; mti++) {
        	
            mt[mti] = (6364136223846793005L * (mt[mti - 1] ^ (mt[mti - 1] >>> 62)) + mti);
        }
    }

    public void setSeed(final long[] array) {
        setSeed(19650218L);
        int i = 1;
        int j = 0;
        int k = (TWIST_NUMBERS > array.length ? TWIST_NUMBERS : array.length);
        for (; k != 0; k--) {
            mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 62)) * 3935559000370003845L))
                + array[j] + j;
            i++;
            j++;
            if (i >= TWIST_NUMBERS) {
                mt[0] = mt[TWIST_NUMBERS - 1];
                i = 1;
            }
            if (j >= array.length)
                j = 0;
        }
        for (k = TWIST_NUMBERS - 1; k != 0; k--) {
            mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 62)) * 2862933555777941757L))
                - i;
            i++;
            if (i >= TWIST_NUMBERS) {
                mt[0] = mt[TWIST_NUMBERS - 1];
                i = 1;
            }
        }

        mt[0] = 1L << 63; /* MSB is 1; assuring non-zero initial array */
    }

    /**
     * Returns up to 32 random bits.
     *
     * <p>the implementation splits a 64-bit long into
     * two 32-bit chunks.
     * </p>
     */
    public int next(final int numbits) {
        //return ((int)next64()) >>> (32 - numbits);

        if (bitState) {
            bits = next64();
            bitState = false;
            return (int) (bits >>> (64 - numbits));
        } else {
            bitState = true;
            return ((int) bits) >>> (32 - numbits);
        }
    }

    /**
     * returns 64 random bits.
     *
     * <p>
     * MT64 is unique in that it natively generates
     * 64-bit of randomness per cycle. This method
     * exposes that.
     * </p>
     */
    public long next64() {
        int i;
        long x;
        if (mti >= TWIST_NUMBERS) { /* generate TWIST_NUMBERS words at one time */

            for (i = 0; i < TWIST_NUMBERS - MM; i++) {
                x = (mt[i] & UM) | (mt[i + 1] & LM);
                mt[i] = mt[i + MM] ^ (x >>> 1) ^ mag01[(int) (x & 1L)];
            }
            for (; i < TWIST_NUMBERS - 1; i++) {
                x = (mt[i] & UM) | (mt[i + 1] & LM);
                mt[i] = mt[i + (MM - TWIST_NUMBERS)] ^ (x >>> 1) ^ mag01[(int) (x & 1L)];
            }
            x = (mt[TWIST_NUMBERS - 1] & UM) | (mt[0] & LM);
            mt[TWIST_NUMBERS - 1] = mt[MM - 1] ^ (x >>> 1) ^ mag01[(int) (x & 1L)];

            mti = 0;
        }

        x = mt[mti++];

        x ^= (x >>> 29) & 0x5555555555555555L;
        x ^= (x << 17) & 0x71D67FFFEDA60000L;
        x ^= (x << 37) & 0xFFF7EEE000000000L;
        x ^= (x >>> 43);

       // Log.v(TAG, "State 1: " + Long.toHexString(mt[0]));
        
        return x;
    }
    
    public long[] getState(){
    	
    	long[] state = new long[mt.length+1];
    	for(int i = 0; i < mt.length; i++){
    		state[i] = mt[i];
    	}
    	state[mt.length] = mti;
    	return state;
    }
    
    public void setState(long[] state){
    	for(int i = 0; i < mt.length; i++){
    		mt[i] = state[i];
    	}
    	mti = (int) state[mt.length];
    }
}
