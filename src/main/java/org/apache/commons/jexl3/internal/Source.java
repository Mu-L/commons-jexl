/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.util.Objects;

import org.apache.commons.jexl3.JexlFeatures;

/**
 * Maintains the set of allowed features associated with a script/expression source.
 * <p>This is meant for caching scripts using their 'source' as key but still distinguishing
 * scripts with different features and prevent false sharing.
 */
public final class Source implements Comparable<Source> {
    /** The hash code, pre-computed for fast op. */
    private final int hashCode;
    /** The set of features. */
    private final JexlFeatures features;
    /** The actual source script/expression. */
    private final String str;

    /**
     * Default constructor.
     * @param theFeatures the features
     * @param theStr the script source
     */
    Source(final JexlFeatures theFeatures, final String theStr) { // CSOFF: MagicNumber
        this.features = theFeatures;
        this.str = theStr;
        int hash = 3;
        hash = 37 * hash + features.hashCode();
        hash = 37 * hash + str.hashCode() ;
        this.hashCode = hash;
    }

    @Override
    public int compareTo(final Source s) {
        return str.compareTo(s.str);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Source other = (Source) obj;
        if (!Objects.equals(this.features, other.features)) {
            return false;
        }
        if (!Objects.equals(this.str, other.str)) {
            return false;
        }
        return true;
    }

    /**
     * @return the features associated with the source
     */
    public JexlFeatures getFeatures() {
        return features;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * @return the length of the script source
     */
    int length() {
        return str.length();
    }

    @Override
    public String toString() {
        return str;
    }

}
