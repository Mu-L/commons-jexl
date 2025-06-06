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

package org.apache.commons.jexl3.introspection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A sandbox describes permissions on a class by explicitly allowing or forbidding
 * access to methods and properties through "allowlists" and "blocklists".
 *
 * <p>A <strong>allowlist</strong> explicitly allows methods/properties for a class;</p>
 *
 * <ul>
 *   <li>If a allowlist is empty and thus does not contain any names,
 *       all properties/methods are allowed for its class.</li>
 *   <li>If it is not empty, the only allowed properties/methods are the ones contained.</li>
 * </ul>
 *
 * <p>A <strong>blocklist</strong> explicitly forbids methods/properties for a class;</p>
 *
 * <ul>
 *   <li>If a blocklist is empty and thus does not contain any names,
 *       all properties/methods are forbidden for its class.</li>
 *   <li>If it is not empty, the only forbidden properties/methods are the ones contained.</li>
 * </ul>
 *
 * <p>Permissions are composed of three lists, read, write, execute, each being
 * "allow" or "block":</p>
 *
 * <ul>
 *   <li><strong>read</strong> controls readable properties </li>
 *   <li><strong>write</strong> controls writable properties</li>
 *   <li><strong>execute</strong> controls executable methods and constructor</li>
 * </ul>
 *
 * <p>When specified, permissions - allow or block lists - can be created inheritable
 * on interfaces or classes and thus applicable to their implementations or derived
 * classes; the sandbox must be created with the 'inheritable' flag for this behavior
 * to be triggered. Note that even in this configuration, it is still possible to
 * add non-inheritable permissions.
 * Adding inheritable lists to a non inheritable sandbox has no added effect;
 * permissions only apply to their specified class.</p>
 *
 * <p>Note that a JexlUberspect always uses a <em>copy</em> of the JexlSandbox
 * used to built it preventing permission changes after its instantiation.</p>
 *
 * @since 3.0
 */
public final class JexlSandbox {
    /**
     * The marker string for explicitly disallowed null properties.
     */
    public static final String NULL = "?";

    /**
     * The pass-thru name set.
     */
    static final Names ALLOW_NAMES = new Names() {
        @Override
        public boolean add(final String name) {
            return false;
        }

        @Override
        public String toString() {
            return "allowAll";
        }
    };

    /**
     * The block-all name set.
     */
    private static final Names BLOCK_NAMES = new Names() {
        @Override
        public boolean add(final String name) {
            return false;
        }

        @Override
        public String get(final String name) {
            return name == null ? NULL : null;
        }

        @Override
        public String toString() {
            return "blockAll";
        }
    };

    /**
     * The block-all permissions.
     */
    private static final Permissions BLOCK_ALL = new Permissions(false, BLOCK_NAMES, BLOCK_NAMES, BLOCK_NAMES);
    /**
     * The pass-thru permissions.
     */
    private static final Permissions ALLOW_ALL = new Permissions(false, ALLOW_NAMES, ALLOW_NAMES, ALLOW_NAMES);
    /**
     * The map from class names to permissions.
     */
    private final Map<String, Permissions> sandbox;
    /**
     * Whether permissions can be inherited (through implementation or extension).
     */
    private final boolean inherit;
    /**
     * Default behavior, block or allow.
     */
    private final boolean allow;

    /**
     * Creates a new default sandbox.
     * <p>In the absence of explicit permissions on a class, the
     * sandbox is an allow-box, allow-listing that class for all permissions (read, write and execute).
     */
    public JexlSandbox() {
        this(true, false, null);
    }

    /**
     * Creates a new default sandbox.
     * <p>A allow-box considers no permissions as &quot;everything is allowed&quot; when
     * a block-box considers no permissions as &quot;nothing is allowed&quot;.
     *
     * @param ab whether this sandbox is allow (true) or block (false)
     *           if no permission is explicitly defined for a class.
     * @since 3.1
     */
    public JexlSandbox(final boolean ab) {
        this(ab, false, null);
    }

    /**
     * Creates a sandbox.
     *
     * @param ab  whether this sandbox is allow (true) or block (false)
     * @param inh whether permissions on interfaces and classes are inherited (true) or not (false)
     * @since 3.2
     */
    public JexlSandbox(final boolean ab, final boolean inh) {
        this(ab, inh, null);
    }

    /**
     * Creates a sandbox based on an existing permissions map.
     *
     * @param ab  whether this sandbox is allow (true) or block (false)
     * @param inh whether permissions are inherited, default false
     * @param map the permissions map
     * @since 3.2
     */
    private JexlSandbox(final boolean ab, final boolean inh, final Map<String, Permissions> map) {
        allow = ab;
        inherit = inh;
        sandbox = map != null ? map : new HashMap<>();
    }

    /**
     * Gets a class by name, crude mechanism for backwards (&lt;3.2 ) compatibility.
     *
     * @param cname the class name
     * @return the class
     */
    static Class<?> forName(final String cname) {
        try {
            return Class.forName(cname);
        } catch (final Exception xany) {
            return null;
        }
    }

    /**
     * Creates a new set of permissions based on allow lists for methods and properties for a given class.
     * <p>The sandbox inheritance property will apply to the permissions created by this method
     *
     * @param clazz the allowed class name
     * @return the permissions instance
     */
    public Permissions allow(final String clazz) {
        return permissions(clazz, true, true, true);
    }

    /**
     * Creates a new set of permissions based on block lists for methods and properties for a given class.
     * <p>The sandbox inheritance property will apply to the permissions created by this method
     *
     * @param clazz the blocked class name
     * @return the permissions instance
     */
    public Permissions block(final String clazz) {
        return permissions(clazz, false, false, false);
    }

    /**
     * Gets a copy of this sandbox
     * @return a copy of this sandbox
     */
    public JexlSandbox copy() {
        // modified concurrently at runtime so...
        final Map<String, Permissions> map = new ConcurrentHashMap<>();
        for (final Map.Entry<String, Permissions> entry : sandbox.entrySet()) {
            map.put(entry.getKey(), entry.getValue().copy());
        }
        return new JexlSandbox(allow, inherit, map);
    }

    /**
     * Gets the execute permission value for a given method of a class.
     *
     * @param clazz the class
     * @param name  the method name
     * @return null if not allowed, the name of the method to use otherwise
     */
    public String execute(final Class<?> clazz, final String name) {
        final String m = get(clazz).execute().get(name);
        return "".equals(name) && m != null ? clazz.getName() : m;
    }

    /**
     * Gets the execute permission value for a given method of a class.
     *
     * @param clazz the class name
     * @param name  the method name
     * @return null if not allowed, the name of the method to use otherwise
     * @deprecated 3.3
     */
    @Deprecated
    public String execute(final String clazz, final String name) {
        final String m = get(clazz).execute().get(name);
        return "".equals(name) && m != null ? clazz : m;
    }

    /**
     * Gets the set of permissions associated to a class.
     *
     * @param clazz the class name
     * @return the defined permissions or an all-allow permission instance if none were defined
     */
    public Permissions get(final String clazz) {
        return get(forName(clazz));
    }

    /**
     * Gets the permissions associated to a class.
     *
     * @param clazz the class
     * @return the permissions
     */
    @SuppressWarnings("null")
    public Permissions get(final Class<?> clazz) {
        // argument clazz cannot be null since permissions would be not null and block:
        // we only store the result for classes we actively seek permissions for.
        return compute(clazz, true);
    }

    private static Permissions inheritable(final Permissions p) {
        return p != null && p.isInheritable() ? p : null;
    }

    /**
     * Computes and optionally stores the permissions associated to a class.
     *
     * @param clazz the class
     * @param store whether the computed permissions should be stored in the sandbox
     * @return the permissions
     */
    private Permissions compute(final Class<?> clazz, final boolean store) {
        // belt and suspender; recursion should not lead here
        if (clazz == null) {
            return BLOCK_ALL;
        }
        final String className = clazz.getName();
        Permissions permissions = sandbox.get(className);
        if (permissions == null) {
            if (inherit) {
                // find first inherited interface that defines permissions
                final Class<?>[] interfaces = clazz.getInterfaces();
                for (int i = 0; permissions == null && i < interfaces.length; ++i) {
                    permissions = inheritable(compute(interfaces[i], false));
                }
                // nothing defined yet, find first superclass that defines permissions
                if (permissions == null) {
                    // let's recurse on super classes
                    final Class<?> superClazz = clazz.getSuperclass();
                    if (Object.class != superClazz) {
                        permissions = inheritable(compute(superClazz, false));
                    }
                }
            }
            // nothing was inheritable
            if (permissions == null) {
                permissions = allow ? ALLOW_ALL : BLOCK_ALL;
            }
            // store the info to avoid doing this costly look-up
            if (store) {
                sandbox.put(className, permissions);
            }
        }
        return permissions;
    }

    /**
     * Creates the set of permissions for a given class.
     * <p>The sandbox inheritance property will apply to the permissions created by this method
     *
     * @param clazz       the class for which these permissions apply
     * @param readFlag    whether the readable property list is allow - true - or block - false -
     * @param writeFlag   whether the writable property list is allow - true - or block - false -
     * @param executeFlag whether the executable method list is allow - true - or block - false -
     * @return the set of permissions
     */
    public Permissions permissions(final String clazz,
                                   final boolean readFlag,
                                   final boolean writeFlag,
                                   final boolean executeFlag) {
        return permissions(clazz, inherit, readFlag, writeFlag, executeFlag);
    }

    /**
     * Creates the set of permissions for a given class.
     *
     * @param clazz  the class for which these permissions apply
     * @param inhf   whether these permissions are inheritable
     * @param readf  whether the readable property list is allow - true - or block - false -
     * @param writef whether the writable property list is allow - true - or block - false -
     * @param execf  whether the executable method list is allow - true - or block - false -
     * @return the set of permissions
     */
    public Permissions permissions(final String clazz,
                                   final boolean inhf,
                                   final boolean readf,
                                   final boolean writef,
                                   final boolean execf) {
        final Permissions box = new Permissions(inhf, readf, writef, execf);
        sandbox.put(clazz, box);
        return box;
    }

    /**
     * Gets the read permission value for a given property of a class.
     *
     * @param clazz the class
     * @param name  the property name
     * @return null (or NULL if name is null) if not allowed, the name of the property to use otherwise
     */
    public String read(final Class<?> clazz, final String name) {
        return get(clazz).read().get(name);
    }

    /**
     * Gets the write permission value for a given property of a class.
     *
     * @param clazz the class
     * @param name  the property name
     * @return null (or NULL if name is null) if not allowed, the name of the property to use otherwise
     */
    public String write(final Class<?> clazz, final String name) {
        return get(clazz).write().get(name);
    }

    /**
     * Gets the write permission value for a given property of a class.
     *
     * @param clazz the class name
     * @param name  the property name
     * @return null if not allowed, the name of the property to use otherwise
     * @deprecated 3.3
     */
    @Deprecated
    public String write(final String clazz, final String name) {
        return get(clazz).write().get(name);
    }

    /**
     * An allow set of names.
     */
    static class AllowSet extends Names {
        /**
         * The map of controlled names and aliases.
         */
        private Map<String, String> names;

        @Override
        public boolean add(final String name) {
            if (names == null) {
                names = new HashMap<>();
            }
            return names.put(name, name) == null;
        }

        @Override
        public boolean alias(final String name, final String alias) {
            if (names == null) {
                names = new HashMap<>();
            }
            return names.put(alias, name) == null;
        }

        @Override
        protected Names copy() {
            final AllowSet copy = new AllowSet();
            copy.names = names == null ? null : new HashMap<>(names);
            return copy;
        }

        @Override
        public String get(final String name) {
            if (names == null) {
                return name;
            }
            final String actual = names.get(name);
            // if null is not explicitly allowed, explicit null aka NULL
            if (name == null && actual == null && !names.containsKey(null)) {
                return JexlSandbox.NULL;
            }
            return actual;
        }

        @Override
        public String toString() {
            return "allow{" + (names == null ? "all" : Objects.toString(names.entrySet())) + "}";
        }
    }

    /**
     * A block set of names.
     */
    static class BlockSet extends Names {
        /**
         * The set of controlled names.
         */
        private Set<String> names;

        @Override
        public boolean add(final String name) {
            if (names == null) {
                names = new HashSet<>();
            }
            return names.add(name);
        }

        @Override
        protected Names copy() {
            final BlockSet copy = new BlockSet();
            copy.names = names == null ? null : new HashSet<>(names);
            return copy;
        }

        @Override
        public String get(final String name) {
            // if name is null and contained in set, explicit null aka NULL
            if (names != null && !names.contains(name)) {
                return name;
            }
            if (name != null) {
                return null;
            }
            return NULL;
        }

        @Override
        public String toString() {
            return "block{" + (names == null ? "all" : Objects.toString(names)) + "}";
        }
    }

    /**
     * A base set of names.
     */
    public abstract static class Names {

        /** Default constructor */
        public Names() {} // Keep Javadoc happy

        /**
         * Adds a name to this set.
         *
         * @param name the name to add
         * @return true if the name was really added, false if not
         */
        public abstract boolean add(String name);

        /**
         * Adds an alias to a name to this set.
         * <p>This only has an effect on allow lists.</p>
         *
         * @param name  the name to alias
         * @param alias the alias
         * @return true if the alias was added, false if it was already present
         */
        public boolean alias(final String name, final String alias) {
            return false;
        }

        /**
         * Gets a copy of these Names
         * @return a copy of these Names
         */
        protected Names copy() {
            return this;
        }

        /**
         * Gets whether a given name is allowed or not.
         *
         * @param name the method/property name to check
         * @return null (or NULL if name is null) if not allowed, the actual name to use otherwise
         */
        public String get(final String name) {
            return name;
        }
    }

    /**
     * Contains the allow or block lists for properties and methods for a given class.
     */
    public static final class Permissions {
        /**
         * Whether these permissions are inheritable, ie can be used by derived classes.
         */
        private final boolean inheritable;
        /**
         * The controlled readable properties.
         */
        private final Names read;
        /**
         * The controlled  writable properties.
         */
        private final Names write;
        /**
         * The controlled methods.
         */
        private final Names execute;

        /**
         * Creates a new permissions instance.
         *
         * @param inherit     whether these permissions are inheritable
         * @param readFlag    whether the read property list is allow or block
         * @param writeFlag   whether the write property list is allow or block
         * @param executeFlag whether the method list is allow of block
         */
        Permissions(final boolean inherit, final boolean readFlag, final boolean writeFlag, final boolean executeFlag) {
            this(inherit,
                readFlag ? new AllowSet() : new BlockSet(),
                writeFlag ? new AllowSet() : new BlockSet(),
                executeFlag ? new AllowSet() : new BlockSet());
        }

        /**
         * Creates a new permissions instance.
         *
         * @param inherit  whether these permissions are inheritable
         * @param nread    the read set
         * @param nwrite   the write set
         * @param nexecute the method set
         */
        Permissions(final boolean inherit, final Names nread, final Names nwrite, final Names nexecute) {
            this.read = nread != null ? nread : ALLOW_NAMES;
            this.write = nwrite != null ? nwrite : ALLOW_NAMES;
            this.execute = nexecute != null ? nexecute : ALLOW_NAMES;
            this.inheritable = inherit;
        }

        /**
         * @return a copy of these permissions
         */
        Permissions copy() {
            return new Permissions(inheritable, read.copy(), write.copy(), execute.copy());
        }

        /**
         * Gets the set of method names in these permissions.
         *
         * @return the set of method names
         */
        public Names execute() {
            return execute;
        }

        /**
         * Adds a list of executable methods names to these permissions.
         * <p>The constructor is denoted as the empty-string, all other methods by their names.</p>
         *
         * @param methodNames the method names
         * @return this instance of permissions
         */
        public Permissions execute(final String... methodNames) {
            for (final String methodName : methodNames) {
                execute.add(methodName);
            }
            return this;
        }

        /**
         * Do these permissions apply to derived classes?
         * @return whether these permissions apply to derived classes.
         */
        public boolean isInheritable() {
            return inheritable;
        }

        /**
         * Gets the set of readable property names in these permissions.
         *
         * @return the set of property names
         */
        public Names read() {
            return read;
        }

        /**
         * Adds a list of readable property names to these permissions.
         *
         * @param propertyNames the property names
         * @return this instance of permissions
         */
        public Permissions read(final String... propertyNames) {
            for (final String propertyName : propertyNames) {
                read.add(propertyName);
            }
            return this;
        }

        /**
         * Gets the set of writable property names in these permissions.
         *
         * @return the set of property names
         */
        public Names write() {
            return write;
        }

        /**
         * Adds a list of writable property names to these permissions.
         *
         * @param propertyNames the property names
         * @return this instance of permissions
         */
        public Permissions write(final String... propertyNames) {
            for (final String propertyName : propertyNames) {
                write.add(propertyName);
            }
            return this;
        }
    }

    /**
     * @deprecated since 3.2, use {@link BlockSet}
     */
    @Deprecated
    public static final class BlackSet extends BlockSet {
        /** Default constructor */
        public BlackSet() { } // Keep Javadoc happy
    }

    /**
     * @deprecated since 3.2, use {@link AllowSet}
     */
    @Deprecated
    public static final class WhiteSet extends AllowSet {
        /** Default constructor */
        public WhiteSet() { } // Keep Javadoc happy
    }

    /**
     * Use block() instead.
     *
     * @param clazz the blocked class name
     * @return the permissions instance
     * @deprecated 3.3
     */
    @Deprecated
    public Permissions black(final String clazz) {
        return block(clazz);
    }
    /**
     * Gets the read permission value for a given property of a class.
     *
     * @param clazz the class name
     * @param name  the property name
     * @return null if not allowed, the name of the property to use otherwise
     * @deprecated 3.3
     */
    @Deprecated
    public String read(final String clazz, final String name) {
        return get(clazz).read().get(name);
    }

    /**
     * Use allow() instead.
     *
     * @param clazz the allowed class name
     * @return the permissions instance
     * @deprecated 3.3
     */
    @Deprecated
    public Permissions white(final String clazz) {
        return allow(clazz);
    }
}
