/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.util.BitSet;

/**
 * The set of symbols declared in a lexical scope.
 * <p>The symbol identifiers are determined by the functional scope.</p>
 * <p>We use 2 bits per symbol s; bit (s*2)+0 sets the actual symbol as lexical (let), bit (s*2)+1 as a const.
 * There are actually only 2 used states: 1 and 3</p>
 */
public class LexicalScope {
    /**
     * Number of bits in a long.
     */
    protected static final int LONGBITS = 64;
    /**
     * Bits per symbol.
     * let (b + 0) + const (b + 1).
     */
    protected static final int BITS_PER_SYMBOL = 2;
    /**
     * Bits per symbol.
     * Declared, const, defined.
     */
    protected static final int SYMBOL_SHIFT = 1;
    /**
     * Bitmask for symbols.
     * Declared, const, defined.
     */
    protected static final long SYMBOL_MASK = (1L << (BITS_PER_SYMBOL - 1)) - 1; // 3, as 1+2, 2 bits
    /**
     * Number of symbols.
     */
    protected int count = 0;
    /**
     * The mask of symbols in the scope.
     */
    protected long symbols = 0L;
    /**
     * Symbols after 64.
     */
    protected BitSet moreSymbols = null;


    /**
     * Create a scope.
     */
    public LexicalScope() {
    }

    /**
     * Frame copy ctor base.
     */
    protected LexicalScope(LexicalScope other) {
        BitSet ms;
        symbols = other.symbols;
        ms = other.moreSymbols;
        moreSymbols = ms != null ? (BitSet) ms.clone() : null;
    }

    /**
     * Ensures more symbols can be stored.
     *
     * @return the set of more symbols
     */
    private BitSet moreSymbols() {
        if (moreSymbols == null) {
            moreSymbols = new BitSet();
        }
        return moreSymbols;
    }

    /**
     * Whether a given bit (not symbol) is set.
     * @param bit the bit
     * @return true if set
     */
    private boolean isSet(final int bit) {
        if (bit < LONGBITS) {
            return (symbols & (1L << bit)) != 0L;
        }
        return moreSymbols != null && moreSymbols.get(bit - LONGBITS);
    }

    /**
     * Sets a given bit (not symbol).
     * @param bit the bit
     * @return true if it was actually set, false if it was set before
     */
    private boolean set(final int bit) {
        if (bit < LONGBITS) {
            if ((symbols & (1L << bit)) != 0L) {
                return false;
            }
            symbols |= (1L << bit);
        } else {
            final int s = bit - LONGBITS;
            final BitSet ms = moreSymbols();
            if (ms.get(s)) {
                return false;
            }
            ms.set(s, true);
        }
        return true;
    }

    /**
     * Checks whether a symbol has already been declared.
     *
     * @param symbol the symbol
     * @return true if declared, false otherwise
     */
    public boolean hasSymbol(final int symbol) {
        final int bit = symbol << SYMBOL_SHIFT;
        return isSet(bit);
    }

    /**
     * Checks whether a symbol is declared as a constant.
     *
     * @param symbol the symbol
     * @return true if declared as constant, false otherwise
     */
    public boolean isConstant(final int symbol) {
        final int bit = (symbol << SYMBOL_SHIFT) | 1;
        return isSet(bit);
    }

    /**
     * Adds a symbol in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addSymbol(final int symbol) {
        final int bit = (symbol << SYMBOL_SHIFT) ;
        if (set(bit)) {
            count += 1;
            return true;
        }
        return false;
    }

    /**
     * Adds a constant in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addConstant(final int symbol) {
        final int letb = (symbol << SYMBOL_SHIFT) ;
        if (!isSet(letb)) {
            throw new IllegalStateException("symbol not declared before const " + symbol);
        }
        final int bit = (symbol << SYMBOL_SHIFT) | 1;
        return set(bit);
    }

    /**
     * Clear all symbols.
     *
     * @param cleanSymbol a (optional, may be null) functor to call for each cleaned symbol
     */
    public final void clearSymbols(final java.util.function.IntConsumer cleanSymbol) {
        // undefine symbols getting out of scope
        if (cleanSymbol != null) {
            long clean = symbols;
            while (clean != 0L) {
                final int s = Long.numberOfTrailingZeros(clean);
                // call clean for symbol definition (3 as a mask for 2 bits,1+2)
                clean &= ~(SYMBOL_MASK << s);
                cleanSymbol.accept(s >> SYMBOL_SHIFT);
            }
        }
        symbols = 0L;
        if (moreSymbols != null) {
            if (cleanSymbol != null) {
                // step by bits per symbol
                for (int s = moreSymbols.nextSetBit(0);
                     s != -1;
                     s = moreSymbols.nextSetBit(s + BITS_PER_SYMBOL)) {
                    // skip const bit indicator
                    if ((s & 1) == 0) {
                        cleanSymbol.accept((s >> SYMBOL_SHIFT) + LONGBITS);
                    }
                }
            }
            moreSymbols.clear();
        }
    }

    /**
     * @return the number of symbols defined in this scope.
     */
    public int getSymbolCount() {
        return count;
    }
}
