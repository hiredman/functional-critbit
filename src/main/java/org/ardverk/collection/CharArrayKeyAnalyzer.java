/*
 * Copyright 2005-2010 Roger Kapsi, Sam Berlin
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ardverk.collection;

import java.io.Serializable;

/**
 * An {@link KeyAnalyzer} for {@code char[]}s
 */
public class CharArrayKeyAnalyzer extends AbstractKeyAnalyzer<char[]> implements Serializable {
    
    private static final long serialVersionUID = -253675854844425270L;

    private static final int DEFAULT_LENGTH = Integer.MAX_VALUE / Character.SIZE;

    /**
     * A singleton instance of {@link CharArrayKeyAnalyzer}
     */
    public static final CharArrayKeyAnalyzer INSTANCE = new CharArrayKeyAnalyzer();
    
    /**
     * A bit mask where the first bit is 1 and the others are zero
     */
    private static final int MSB = 0x8000;
    
    private final int maxLengthInBits;
    
    public CharArrayKeyAnalyzer() {
        this(DEFAULT_LENGTH);
    }
    
    public CharArrayKeyAnalyzer(int maxLengthInBits) {
        if (maxLengthInBits < 0 || DEFAULT_LENGTH < maxLengthInBits) {
            throw new IllegalArgumentException(
                    "maxLengthInBits=" + maxLengthInBits);
        }
        
        this.maxLengthInBits = maxLengthInBits;
    }
    
    public int getMaxLengthInBits() {
        return maxLengthInBits;
    }
    
    @Override
    public int compare(char[] o1, char[] o2) {
        if (o1 == null) {
            return (o2 == null) ? 0 : -1;
        } else if (o2 == null) {
            return (o1 == null) ? 0 : 1;
        }
        
        if (o1.length != o2.length) {
            return o1.length - o2.length;
        }
        
        for (int i = 0; i < o1.length; i++) {
            int diff = (o1[i] & 0xFF) - (o2[i] & 0xFF);
            if (diff != 0) {
                return diff;
            }
        }

        return 0;
    }

    @Override
    public int lengthInBits(char[] key) {
        return key.length * Character.SIZE;
    }

    @Override
    public boolean isBitSet(char[] key, int bitIndex) {
        int lengthInBits = lengthInBits(key);
        int prefix = maxLengthInBits - lengthInBits;
        int keyBitIndex = bitIndex - prefix;
        
        if (keyBitIndex >= lengthInBits || keyBitIndex < 0) {
            return false;
        }
        
        int index = (int)(keyBitIndex / Character.SIZE);
        int bit = (int)(keyBitIndex % Character.SIZE);
        return (key[index] & mask(bit)) != 0;
    }

    @Override
    public int bitIndex(char[] key, char[] otherKey) {
        
        int length1 = lengthInBits(key);
        int length2 = lengthInBits(otherKey);
        int length = Math.max(length1, length2);
        int prefix = maxLengthInBits - length;
                
        if (prefix < 0) {
            return KeyAnalyzer.OUT_OF_BOUNDS_BIT_KEY;
        }
        
        boolean allNull = true;
        for (int i = 0; i < length; i++) {
            int bitIndex = prefix + i;
            boolean value = isBitSet(key, bitIndex);
                
            if (value) {
                allNull = false;
            }
            
            boolean otherValue = isBitSet(otherKey, bitIndex);
            
            if (value != otherValue) {
                return bitIndex;
            }
        }
        
        if (allNull) {
            return KeyAnalyzer.NULL_BIT_KEY;
        }
        
        return KeyAnalyzer.EQUAL_BIT_KEY;
    }
    
    @Override
    public boolean isPrefix(char[] key, char[] prefix) {
        if (key.length < prefix.length) {
            return false;
        }
        
        for (int i = 0; i < prefix.length; i++) {
            if (key[i] != prefix[i]) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Returns a bit mask where the given bit is set
     */
    private static int mask(int bit) {
        return MSB >>> bit;
    }
}