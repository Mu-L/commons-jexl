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
//CSOFF: FileLength
package org.apache.commons.jexl3.internal;


import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect.PropertyResolver;
import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTDoWhileStatement;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEmptyMethod;
import org.apache.commons.jexl3.parser.ASTEnumerationNode;
import org.apache.commons.jexl3.parser.ASTEnumerationReference;
import org.apache.commons.jexl3.parser.ASTExpressionStatement;
import org.apache.commons.jexl3.parser.ASTExtendedLiteral;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTForeachVar;
import org.apache.commons.jexl3.parser.ASTForVar;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIdentifierAccessJxlt;
import org.apache.commons.jexl3.parser.ASTInlinePropertyAssignment;
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyNode;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapEnumerationNode;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMapProjectionNode;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNullpNode;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTProjectionNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReductionNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTRegexLiteral;
import org.apache.commons.jexl3.parser.ASTRemove;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSelectionNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTSizeMethod;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTThisNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Node;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import org.apache.commons.jexl3.JxltEngine;


/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter extends InterpreterBase {
    /** The operators evaluation delegate. */
    protected final Operators operators;
    /** Cache executors. */
    protected final boolean cache;
    /** Symbol values. */
    protected final Scope.Frame frame;
    /** The context to store/retrieve variables. */
    protected final JexlContext.NamespaceResolver ns;
    /** The map of 'prefix:function' to object resolving as namespaces. */
    protected final Map<String, Object> functions;
    /** The map of dynamically creates namespaces, NamespaceFunctor or duck-types of those. */
    protected Map<String, Object> functors;

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param aContext the context to evaluate expression
     * @param eFrame   the interpreter evaluation frame
     */
    protected Interpreter(Engine engine, JexlContext aContext, Scope.Frame eFrame) {
        super(engine, aContext);
        this.operators = new Operators(this);
        this.cache = jexl.cache != null;
        this.frame = eFrame;
        if (this.context instanceof JexlContext.NamespaceResolver) {
            ns = ((JexlContext.NamespaceResolver) context);
        } else {
            ns = Engine.EMPTY_NS;
        }
        this.functions = jexl.functions;
        this.functors = null;
    }

    /**
     * Copy constructor.
     * @param ii  the interpreter to copy
     * @param jexla the arithmetic instance to use (or null)
     */
    protected Interpreter(Interpreter ii, JexlArithmetic jexla) {
        super(ii, jexla);
        operators = ii.operators;
        cache = ii.cache;
        frame = ii.frame;
        ns = ii.ns;
        functions = ii.functions;
        functors = ii.functors;
    }

    /**
     * Interpret the given script/expression.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through
     * its logger as warning.
     * @param node the script or expression to interpret.
     * @return the result of the interpretation.
     * @throws JexlException if any error occurs during interpretation.
     */
    public Object interpret(JexlNode node) {
        JexlContext.ThreadLocal tcontext = null;
        JexlEngine tjexl = null;
        try {
            if (context instanceof JexlContext.ThreadLocal) {
                tcontext = jexl.putThreadLocal((JexlContext.ThreadLocal) context);
            }
            tjexl = jexl.putThreadEngine(jexl);
            cancelCheck(node);
            return node.jjtAccept(this, null);
        } catch (JexlException.Return xreturn) {
            return xreturn.getValue();
        } catch (JexlException.Cancel xcancel) {
            cancelled |= Thread.interrupted();
            if (isCancellable()) {
                throw xcancel.clean();
            }
        } catch (JexlException xjexl) {
            if (!isSilent()) {
                throw xjexl.clean();
            }
            if (logger.isWarnEnabled()) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
            }
        } finally {
            synchronized(this) {
                if (functors != null) {
                    if (AUTOCLOSEABLE != null) {
                        for (Object functor : functors.values()) {
                            closeIfSupported(functor);
                        }
                    }
                    functors.clear();
                    functors = null;
                }
            }
            jexl.putThreadEngine(tjexl);
            if (context instanceof JexlContext.ThreadLocal) {
                jexl.putThreadLocal(tcontext);
            }
        }
        return null;
    }

    /**
     * Resolves a namespace, eventually allocating an instance using context as constructor argument.
     * <p>
     * The lifetime of such instances span the current expression or script evaluation.</p>
     * @param prefix the prefix name (may be null for global namespace)
     * @param node   the AST node
     * @return the namespace instance
     */
    protected Object resolveNamespace(String prefix, JexlNode node) {
        Object namespace;
        // check whether this namespace is a functor
        synchronized (this) {
            if (functors != null) {
                namespace = functors.get(prefix);
                if (namespace != null) {
                    return namespace;
                }
            }
        }
        // check if namespace is a resolver
        namespace = ns.resolveNamespace(prefix);
        if (namespace == null) {
            namespace = functions.get(prefix);
            if (prefix != null && namespace == null) {
                throw new JexlException(node, "no such function namespace " + prefix, null);
            }
        }
        // shortcut if ns is known to be not-a-functor
        final boolean cacheable = cache;
        Object cached = cacheable ? node.jjtGetValue() : null;
        if (cached != JexlContext.NamespaceFunctor.class) {
            // allow namespace to instantiate a functor with context if possible, not an error otherwise
            Object functor = null;
            if (namespace instanceof JexlContext.NamespaceFunctor) {
                functor = ((JexlContext.NamespaceFunctor) namespace).createFunctor(context);
            } else if (namespace instanceof Class<?> || namespace instanceof String) {
                // attempt to reuse last ctor cached in volatile JexlNode.value
                if (cached instanceof JexlMethod) {
                    Object eval = ((JexlMethod) cached).tryInvoke(null, context);
                    if (JexlEngine.TRY_FAILED != eval) {
                        functor = eval;
                    }
                }
                if (functor == null) {
                    JexlMethod ctor = uberspect.getConstructor(namespace, context);
                    if (ctor != null) {
                        try {
                            functor = ctor.invoke(namespace, context);
                            if (cacheable && ctor.isCacheable()) {
                                node.jjtSetValue(ctor);
                            }
                        } catch (Exception xinst) {
                            throw new JexlException(node, "unable to instantiate namespace " + prefix, xinst);
                        }
                    }
                }
            }
            // got a functor, store it and return it
            if (functor != null) {
                synchronized (this) {
                    if (functors == null) {
                        functors = new HashMap<String, Object>();
                    }
                    functors.put(prefix, functor);
                }
                return functor;
            } else {
                // use the NamespaceFunctor class to tag this node as not-a-functor
                node.jjtSetValue(JexlContext.NamespaceFunctor.class);
            }
        }
        return namespace;
    }

    @Override
    protected Object visit(ASTAddNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.ADD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.add(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "+ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTSubNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.SUBTRACT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.subtract(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTMulNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.MULTIPLY, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.multiply(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "* error", xrt);
        }
    }

    @Override
    protected Object visit(ASTDivNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.DIVIDE, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.divide(left, right);
        } catch (ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "/ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTModNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.MOD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.mod(left, right);
        } catch (ArithmeticException xrt) {
            if (!arithmetic.isStrict()) {
                return 0.0d;
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "% error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.AND, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.and(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "& error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.OR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.or(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "| error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseXorNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.XOR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.xor(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "^ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTEQNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "== error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result) ? Boolean.FALSE : Boolean.TRUE
                   : arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "!= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTGENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.GTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, ">= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTGTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.GT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "> error", xrt);
        }
    }

    @Override
    protected Object visit(ASTLENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.LTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "<= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTLTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.LT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "< error", xrt);
        }
    }

    @Override
    protected Object visit(ASTSWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.startsWith(node, "^=", left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNSWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.startsWith(node, "^!", left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTEWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.endsWith(node, "$=", left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNEWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.endsWith(node, "$!", left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTERNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.contains(node, "=~", right, left) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNRNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.contains(node, "!~", right, left) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTRangeNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.createRange(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, ".. error", xrt);
        }
    }

    @Override
    protected Object visit(ASTUnaryMinusNode node, Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        Object val = valNode.jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NEGATE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            Object number = arithmetic.negate(val);
            // attempt to recoerce to literal class
            if (valNode instanceof ASTNumberLiteral && number instanceof Number) {
                number = arithmetic.narrowNumber((Number) number, ((ASTNumberLiteral) valNode).getLiteralClass());
            }
            return number;
        } catch (ArithmeticException xrt) {
            throw new JexlException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseComplNode node, Object data) {
        Object arg = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.COMPLEMENT, arg);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.complement(arg);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "~ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNotNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NOT, val);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.not(val);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "! error", xrt);
        }
    }

    @Override
    protected Object visit(ASTEnumerationReference node, Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        // pass first piece of data in and loop through children
        Object object = data;
        JexlNode objectNode = null;
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            // attempt to evaluate the property within the object)
            object = objectNode.jjtAccept(this, object);
            cancelCheck(node);
        }
        return object;
    }

    @Override
    protected Object visit(ASTEnumerationNode node, Object data) {
        final int numChildren = node.jjtGetNumChildren();
        if (numChildren == 1) {
            JexlNode valNode = node.jjtGetChild(0);
            Object iterableValue = valNode.jjtAccept(this, data);

            if (iterableValue != null) {
                Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                Iterator<?> itemsIterator = forEach instanceof Iterator
                                      ? (Iterator<?>) forEach
                                      : uberspect.getIndexedIterator(iterableValue);
                return itemsIterator;
            } else {
                return null;
            }
        } else {
            Object initialValue = node.jjtGetChild(0).jjtAccept(this, data);

            ASTJexlLambda generator = (ASTJexlLambda) node.jjtGetChild(1);
            return new GeneratorIterator(initialValue, generator);
        }
    }

    public class GeneratorIterator implements Iterator<Object> {

        protected final ASTJexlLambda node;
        protected final Closure generator;

        protected int i;

        protected Object value;

        protected GeneratorIterator(Object initialValue, ASTJexlLambda node) {
            this.node = node;
            generator = new Closure(Interpreter.this, node);

            i = 0;
            value = initialValue;
        }

        protected void nextValue() {

            i += 1;

            int argCount = node.getArgCount();

            Object[] argv = null;

            if (argCount == 0) {
                argv = EMPTY_PARAMS;
            } else if (argCount == 1) {
                argv = new Object[] {value};
            } else if (argCount == 2) {
                argv = new Object[] {i, value};
            }

            value = generator.execute(null, argv);
        }

        @Override
        public boolean hasNext() {
            return value != null;
        }

        @Override
        public Object next() {
            cancelCheck(node);

            if (value == null)
                throw new NoSuchElementException();

            Object result = value;

            nextValue();

            return result;
        }

        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

    }


    @Override
    protected Object visit(ASTExpressionStatement node, Object data) {
        cancelCheck(node);
        Object result = node.jjtGetChild(0).jjtAccept(this, data);
        return result;
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        int n = 0;
        final int numChildren = node.jjtGetNumChildren();
        try {
            Object result = null;
            // pairs of { conditions , 'then' statement }
            for(int ifElse = 0; ifElse < (numChildren - 1); ifElse += 2) {
                Object condition = node.jjtGetChild(ifElse).jjtAccept(this, null);
                if (arithmetic.toBoolean(condition)) {
                    // first objectNode is true statement
                    return node.jjtGetChild(ifElse + 1).jjtAccept(this, null);
                }
            }
            // if odd...
            if ((numChildren & 1) == 1) {
                // if there is an else, there are an odd number of children in the statement and it is the last child,
                // execute it.
                result = node.jjtGetChild(numChildren - 1).jjtAccept(this, null);
            }
            return result;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(n), "if error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBlock node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            cancelCheck(node);
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTReturnStatement node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        cancelCheck(node);
        throw new JexlException.Return(node, null, val);
    }

    @Override
    protected Object visit(ASTContinue node, Object data) {
        throw new JexlException.Continue(node);
    }

    @Override
    protected Object visit(ASTRemove node, Object data) {
        throw new JexlException.Remove(node);
    }

    @Override
    protected Object visit(ASTBreak node, Object data) {
        throw new JexlException.Break(node);
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        Object result = null;
        /* first objectNode is the loop variable */
        ASTForeachVar loopReference = (ASTForeachVar) node.jjtGetChild(0);

        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);

        /* second objectNode is the variable to iterate */
        Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);

        // make sure there is a value to iterate on and a statement to execute
        if (iterableValue != null && node.jjtGetNumChildren() >= 3) {
            /* third objectNode is the statement to execute */
            JexlNode statement = node.jjtGetChild(2);
            // get an iterator for the collection/array etc via the introspector.
            Object forEach = null;
            try {

                if (loopReference.jjtGetNumChildren() > 1) {

                    ASTIdentifier loopValueVariable = (ASTIdentifier) loopReference.jjtGetChild(1);

                    int symbol = loopVariable.getSymbol();
                    int valueSymbol = loopValueVariable.getSymbol();

                    forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
                    Iterator<?> itemsIterator = forEach instanceof Iterator
                                            ? (Iterator<?>) forEach
                                            : uberspect.getIndexedIterator(iterableValue);

                    int i = -1;

                    if (itemsIterator != null) {
                        while (itemsIterator.hasNext()) {
                            cancelCheck(node);
                            i += 1;
                            // set loopVariable to value of iterator
                            Object value = itemsIterator.next();

                            if (value instanceof Map.Entry<?,?>) {
                                Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                                if (symbol < 0) {
                                    context.set(loopVariable.getName(), entry.getKey());
                                } else {
                                    frame.set(symbol, entry.getKey());
                                }
                                if (valueSymbol < 0) {
                                    context.set(loopValueVariable.getName(), entry.getValue());
                                } else {
                                    frame.set(valueSymbol, entry.getValue());
                                }
                            } else {
                                if (symbol < 0) {
                                    context.set(loopVariable.getName(), i);
                                } else {
                                    frame.set(symbol, i);
                                }
                                if (valueSymbol < 0) {
                                    context.set(loopValueVariable.getName(), value);
                                } else {
                                    frame.set(valueSymbol, value);
                                }
                            }

                            try {
                                // execute statement
                                result = statement.jjtAccept(this, data);
                            } catch (JexlException.Break stmtBreak) {
                                break;
                            } catch (JexlException.Continue stmtContinue) {
                                //continue;
                            } catch (JexlException.Remove stmtRemove) {
                                itemsIterator.remove();
                                i -= 1;
                                // and continue
                            }
                        }
                    }

                } else {
                    int symbol = loopVariable.getSymbol();

                    forEach = operators.tryOverload(node, JexlOperator.FOR_EACH, iterableValue);
                    Iterator<?> itemsIterator = forEach instanceof Iterator
                                            ? (Iterator<?>) forEach
                                            : uberspect.getIterator(iterableValue);
                    if (itemsIterator != null) {
                        while (itemsIterator.hasNext()) {
                            cancelCheck(node);
                            // set loopVariable to value of iterator
                            Object value = itemsIterator.next();
                            if (symbol < 0) {
                                context.set(loopVariable.getName(), value);
                            } else {
                                frame.set(symbol, value);
                            }
                            try {
                                // execute statement
                                result = statement.jjtAccept(this, data);
                            } catch (JexlException.Break stmtBreak) {
                                break;
                            } catch (JexlException.Continue stmtContinue) {
                                //continue;
                            } catch (JexlException.Remove stmtRemove) {
                                itemsIterator.remove();
                                // and continue
                            }
                        }
                    }
                }

            } finally {
                //  closeable iterator handling
                closeIfSupported(forEach);
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTForeachVar node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        Object result = null;
        /* first objectNode is the expression */
        Node expressionNode = node.jjtGetChild(0);
        while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data))) {
            cancelCheck(node);
            if (node.jjtGetNumChildren() > 1) {
                try {
                    // execute statement
                    result = node.jjtGetChild(1).jjtAccept(this, data);
                } catch (JexlException.Break stmtBreak) {
                    break;
                } catch (JexlException.Continue stmtContinue) {
                    //continue;
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTDoWhileStatement node, Object data) {
        Object result = null;
        /* last objectNode is the expression */
        Node expressionNode = node.jjtGetChild(1);
        do {
            cancelCheck(node);

            try {
                // execute statement
                result = node.jjtGetChild(0).jjtAccept(this, data);
            } catch (JexlException.Break stmtBreak) {
                break;
            } catch (JexlException.Continue stmtContinue) {
                //continue;
            }

        } while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data)));

        return result;
    }

    @Override
    protected Object visit(ASTAndNode node, Object data) {
        /**
         * The pattern for exception mgmt is to let the child*.jjtAccept out of the try/catch loop so that if one fails,
         * the ex will traverse up to the interpreter. In cases where this is not convenient/possible, JexlException
         * must be caught explicitly and rethrown.
         */
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (!leftValue) {
                return Boolean.FALSE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (!rightValue) {
                return Boolean.FALSE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNullLiteral node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTThisNode node, Object data) {
        return context;
    }

    @Override
    protected Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTFalseNode node, Object data) {
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNumberLiteral node, Object data) {
        if (data != null && node.isInteger()) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTStringLiteral node, Object data) {
        if (data != null) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTRegexLiteral node, Object data) {
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.ArrayBuilder ab = arithmetic.arrayBuilder(childCount);
        boolean extended = false;
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTExtendedLiteral) {
                extended = true;
            } else if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                if (it != null) {
                   while (it.hasNext()) {
                       Object entry = it.next();
                       ab.add(entry);
                   }
                }
                closeIfSupported(it);
            } else {
                Object entry = child.jjtAccept(this, data);
                ab.add(entry);
            }
        }
        return ab.create(extended);
    }

    @Override
    protected Object visit(ASTExtendedLiteral node, Object data) {
        return node;
    }

    @Override
    protected Object visit(ASTSetLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.SetBuilder mb = arithmetic.setBuilder(childCount);
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                if (it != null) {
                   while (it.hasNext()) {
                       Object entry = it.next();
                       mb.add(entry);
                   }
                }
                closeIfSupported(it);
            } else {
                Object entry = child.jjtAccept(this, data);
                mb.add(entry);
            }
        }
        return mb.create();
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.MapBuilder mb = arithmetic.mapBuilder(childCount);
        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);
            JexlNode child = node.jjtGetChild(i);
            if (child instanceof ASTMapEntry) {
                Object[] entry = (Object[]) (child).jjtAccept(this, data);
                mb.put(entry[0], entry[1]);
            } else {
                Iterator<Object> it = (Iterator<Object>) (child).jjtAccept(this, data);
                int j = 0;
                if (it != null) {
                    while (it.hasNext()) {
                        Object value = it.next();
                        if (value instanceof Map.Entry<?,?>) {
                            Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                            mb.put(entry.getKey(), entry.getValue());
                        } else {
                            mb.put(i, value);
                        }
                        i++;
                    }
                }
            }
        }
        return mb.create();
    }

    @Override
    protected Object visit(ASTMapEntry node, Object data) {
        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    @Override
    protected Object visit(ASTMapEnumerationNode node, Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        Object iterableValue = valNode.jjtAccept(this, data);

        if (iterableValue != null) {
            Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
            Iterator<?> itemsIterator = forEach instanceof Iterator
                                   ? (Iterator<?>) forEach
                                   : uberspect.getIndexedIterator(iterableValue);
            return itemsIterator;
        } else {
            return null;
        }
    }

    @Override
    protected Object visit(ASTInlinePropertyAssignment node, Object data) {

        int childCount = node.jjtGetNumChildren();

        for (int i = 0; i < childCount; i++) {
            cancelCheck(node);

            JexlNode p = node.jjtGetChild(i);

            if (p instanceof ASTInlinePropertyEntry) {

               Object[] entry = (Object[]) p.jjtAccept(this, null);

               String name = String.valueOf(entry[0]);
               Object value = entry[1];

               setAttribute(data, name, value, p, JexlOperator.PROPERTY_SET);

            } else if (p instanceof ASTInlinePropertyArrayEntry) {

               Object[] entry = (Object[]) p.jjtAccept(this, null);

               Object key = entry[0];
               Object value = entry[1];

               setAttribute(data, key, value, p, JexlOperator.ARRAY_SET);

            } else {

               // ASTInlinePropertyNode
               p.jjtAccept(this, data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayEntry node, Object data) {

        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return new Object[] {key, value};
    }

    @Override
    protected Object visit(ASTInlinePropertyEntry node, Object data) {
        JexlNode name = node.jjtGetChild(0);

        Object key = name instanceof ASTIdentifier ? ((ASTIdentifier) name).getName() : name.jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);

        return new Object[] {key, value};
    }

    @Override
    protected Object visit(ASTInlinePropertyNode node, Object data) {

        JexlNode object = node.jjtGetChild(0);

        Object target = object.jjtAccept(this, data);

        if (target == null)
            return unsolvableProperty(node, "<null>{<?>}", null);

        // evaluate ASTInlinePropertyAssignment with calculated target
        node.jjtGetChild(1).jjtAccept(this, target);

        return target;
    }

    @Override
    protected Object visit(ASTTernaryNode node, Object data) {
        Object condition = node.jjtGetChild(0).jjtAccept(this, data);
        if (node.jjtGetNumChildren() == 3) {
            if (condition != null && arithmetic.toBoolean(condition)) {
                return node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                return node.jjtGetChild(2).jjtAccept(this, data);
            }
        }
        if (condition != null && arithmetic.toBoolean(condition)) {
            return condition;
        } else {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
    }

    @Override
    protected Object visit(ASTNullpNode node, Object data) {
        Object lhs = node.jjtGetChild(0).jjtAccept(this, data);
        return lhs != null? lhs : node.jjtGetChild(1).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTSizeFunction node, Object data) {
        try {
            Object val = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.size(node, val);
        } catch(JexlException xany) {
            return 0;
        }
    }

    @Override
    protected Object visit(ASTSizeMethod node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return operators.size(node, val);
    }

    @Override
    protected Object visit(ASTEmptyFunction node, Object data) {
        try {
            Object value = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.empty(node, value);
        } catch(JexlException xany) {
            return true;
        }
    }

    @Override
    protected Object visit(ASTEmptyMethod node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return operators.empty(node, val);
    }

    @Override
    protected Object visit(ASTJexlScript node, Object data) {
        if (node instanceof ASTJexlLambda && !((ASTJexlLambda) node).isTopLevel()) {
            return new Closure(this, (ASTJexlLambda) node);
        } else {
            final int numChildren = node.jjtGetNumChildren();
            Object result = null;
            for (int i = 0; i < numChildren; i++) {
                JexlNode child = node.jjtGetChild(i);
                result = child.jjtAccept(this, data);
                cancelCheck(child);
            }
            return result;
        }
    }

    @Override
    protected Object visit(ASTVar node, Object data) {
        return visit((ASTIdentifier) node, data);
    }

    @Override
    protected Object visit(ASTForVar node, Object data) {
        return visit((ASTIdentifier) node, data);
    }

    @Override
    protected Object visit(ASTReferenceExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTIdentifier node, Object data) {
        cancelCheck(node);
        String name = node.getName();
        if (data == null) {
            int symbol = node.getSymbol();
            if (symbol >= 0) {
                return frame.get(symbol);
            }
            Object value = context.get(name);

            if (value == null && node.jjtGetParent() instanceof ASTExpressionStatement) {

               JexlMethod vm = uberspect.getMethod(arithmetic, name, EMPTY_PARAMS);

               if (vm != null) {

                   try {
                      Object eval = vm.invoke(arithmetic, EMPTY_PARAMS);

                      if (cache && vm.isCacheable()) {
                          Funcall funcall = new ArithmeticFuncall(vm, false);
                          node.jjtSetValue(funcall);
                      }

                      return eval;

                   } catch (JexlException xthru) {
                       throw xthru;
                   } catch (Exception xany) {
                       throw invocationException(node, name, xany);
                   }
               }
            }

            if (value == null
                    && !(node.jjtGetParent() instanceof ASTReference)
                    && !context.has(name)
                    && !node.isTernaryProtected()) {
                return unsolvableVariable(node, name, true);
            }
            return value;
        } else {
            return getAttribute(data, name, node);
        }
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        // first objectNode is the identifier
        Object object = data;
        // can have multiple nodes - either an expression, integer literal or reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            JexlNode nindex = node.jjtGetChild(i);
            if (object == null) {
                return null;
            }
            Object index = nindex.jjtAccept(this, null);
            cancelCheck(node);
            object = getAttribute(object, index, nindex);
        }
        return object;
    }

    /**
     * Checks whether a reference child node holds a local variable reference.
     * @param node  the reference node
     * @param which the child we are checking
     * @return true if child is local variable, false otherwise
     */
    protected boolean isLocalVariable(ASTReference node, int which) {
        return (node.jjtGetNumChildren() > which
                && node.jjtGetChild(which) instanceof ASTIdentifier
                && ((ASTIdentifier) node.jjtGetChild(which)).getSymbol() >= 0);
    }

    /**
     * Evaluates an access identifier based on the 2 main implementations;
     * static (name or numbered identifier) or dynamic (jxlt).
     * @param node the identifier access node
     * @return the evaluated identifier
     */
    private Object evalIdentifier(ASTIdentifierAccess node) {
        if (node instanceof ASTIdentifierAccessJxlt) {
            final ASTIdentifierAccessJxlt accessJxlt = (ASTIdentifierAccessJxlt) node;
            final String src = node.getName();
            Throwable cause = null;
            TemplateEngine.TemplateExpression expr = (TemplateEngine.TemplateExpression) accessJxlt.getExpression();
            try {
                if (expr == null) {
                    TemplateEngine jxlt = jexl.jxlt();
                    expr = jxlt.parseExpression(node.jexlInfo(), src, frame != null ? frame.getScope() : null);
                    accessJxlt.setExpression(expr);
                }
                if (expr != null) {
                    Object name = expr.evaluate(frame, context);
                    if (name != null) {
                        Integer id = ASTIdentifierAccess.parseIdentifier(name.toString());
                        return id != null ? id : name;
                    }
                }
            } catch (JxltEngine.Exception xjxlt) {
                cause = xjxlt;
            }
            return node.isSafe() ? null : unsolvableProperty(node, src, true, cause);
        } else {
            return node.getIdentifier();
        }
    }

    @Override
    protected Object visit(ASTIdentifierAccess node, Object data) {
        if (data == null) {
            return null;
        }
        Object id = evalIdentifier(node);
        return getAttribute(data, id, node);
    }

    @Override
    protected Object visit(ASTReference node, Object data) {
        cancelCheck(node);
        final int numChildren = node.jjtGetNumChildren();
        final JexlNode parent = node.jjtGetParent();
        // pass first piece of data in and loop through children
        Object object = data;
        JexlNode objectNode = null;
        JexlNode ptyNode = null;
        StringBuilder ant = null;
        boolean antish = !(parent instanceof ASTReference);
        int v = 1;
        main:
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            if (objectNode instanceof ASTMethodNode) {
                if (object == null) {
                    // we may be performing a method call on an antish var
                    if (ant != null) {
                        JexlNode child = objectNode.jjtGetChild(0);
                        if (child instanceof ASTIdentifierAccess) {
                            ant.append('.');
                            ant.append(((ASTIdentifierAccess) child).getName());
                            object = context.get(ant.toString());
                            if (object != null) {
                                object = visit((ASTMethodNode) objectNode, object, context);
                            }
                            continue;
                        }
                    }
                    break;
                } else {
                    antish = false;
                }
            }
            // attempt to evaluate the property within the object (visit(ASTIdentifierAccess node))
            object = objectNode.jjtAccept(this, object);
            cancelCheck(node);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {  // if we still have a null object, check for an antish variable
                if (ant == null) {
                    JexlNode first = node.jjtGetChild(0);
                    if (first instanceof ASTIdentifier) {
                        ASTIdentifier afirst = (ASTIdentifier) first;
                        if (afirst.getSymbol() < 0) {
                            ant = new StringBuilder(afirst.getName());
                        } else {
                            break main;
                        }
                    } else {
                        ptyNode = objectNode;
                        break main;
                    }
                }
                for (; v <= c; ++v) {
                    JexlNode child = node.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        ASTIdentifierAccess achild = (ASTIdentifierAccess) child;
                        if (achild.isSafe() || achild.isExpression()) {
                           break main;
                        }
                        ant.append('.');
                        ant.append(((ASTIdentifierAccess) objectNode).getName());
                    } else {
                        break main;
                    }
                }
                object = context.get(ant.toString());
            } else {
                // the last one may be null
                ptyNode = c != numChildren - 1? objectNode : null;
                break; //
            }
        }
        if (object == null && !node.isTernaryProtected()) {
            if (antish && ant != null) {
                boolean undefined = !(context.has(ant.toString()) || isLocalVariable(node, 0));
                // variable unknown in context and not a local
                return node.isSafeLhs()
                        ? null
                        : unsolvableVariable(node, ant.toString(), undefined);
            }
            if (ptyNode != null) {
                // am I the left-hand side of a safe op ?
                return ptyNode.isSafeLhs()
                       ? null
                       : unsolvableProperty(node, stringifyProperty(ptyNode), false, null);
            }
        }
        return object;
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        return executeAssign(node, null, data);
    }

    @Override
    protected Object visit(ASTSetAddNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_ADD, data);
    }

    @Override
    protected Object visit(ASTSetSubNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_SUBTRACT, data);
    }

    @Override
    protected Object visit(ASTSetMultNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_MULTIPLY, data);
    }

    @Override
    protected Object visit(ASTSetDivNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_DIVIDE, data);
    }

    @Override
    protected Object visit(ASTSetModNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_MOD, data);
    }

    @Override
    protected Object visit(ASTSetAndNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_AND, data);
    }

    @Override
    protected Object visit(ASTSetOrNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_OR, data);
    }

    @Override
    protected Object visit(ASTSetXorNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_XOR, data);
    }

    /**
     * Executes an assignment with an optional side-effect operator.
     * @param node     the node
     * @param assignop the assignment operator or null if simply assignment
     * @param data     the data
     * @return the left hand side
     */
    protected Object executeAssign(JexlNode node, JexlOperator assignop, Object data) { // CSOFF: MethodLength
        cancelCheck(node);
        // left contains the reference to assign to
        final JexlNode left = node.jjtGetChild(0);
        // right is the value expression to assign
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        Object object = null;
        int symbol = -1;
        boolean antish = true;
        // 0: determine initial object & property:
        final int last = left.jjtGetNumChildren() - 1;
        if (left instanceof ASTIdentifier) {
            ASTIdentifier var = (ASTIdentifier) left;
            symbol = var.getSymbol();
            if (symbol >= 0) {
                // check we are not assigning a symbol itself
                if (last < 0) {
                    if (assignop != null) {
                        Object self = frame.get(symbol);
                        right = operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    frame.set(symbol, right);
                    // make the closure accessible to itself, ie hoist the currently set variable after frame creation
                    if (right instanceof Closure) {
                        ((Closure) right).setHoisted(symbol, right);
                    }
                    return right; // 1
                }
                object = frame.get(symbol);
                // top level is a symbol, can not be an antish var
                antish = false;
            } else {
                // check we are not assigning direct global
                if (last < 0) {
                    if (assignop != null) {
                        Object self = context.get(var.getName());
                        right = operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    try {
                        context.set(var.getName(), right);
                    } catch (UnsupportedOperationException xsupport) {
                        throw new JexlException(node, "context is readonly", xsupport);
                    }
                    return right; // 2
                }
                object = context.get(var.getName());
                // top level accesses object, can not be an antish var
                if (object != null) {
                    antish = false;
                }
            }
        } else if (!(left instanceof ASTReference)) {
            throw new JexlException(left, "illegal assignment form 0");
        }
        // 1: follow children till penultimate, resolve dot/array
        JexlNode objectNode = null;
        StringBuilder ant = null;
        int v = 1;
        // start at 1 if symbol
        for (int c = symbol >= 0 ? 1 : 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {
                if (ant == null) {
                    JexlNode first = left.jjtGetChild(0);
                    if (first instanceof ASTIdentifier && ((ASTIdentifier) first).getSymbol() < 0) {
                        ant = new StringBuilder(((ASTIdentifier) first).getName());
                    } else {
                        break;
                    }
                }
                for (; v <= c; ++v) {
                    JexlNode child = left.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        ant.append('.');
                        ant.append(((ASTIdentifierAccess) objectNode).getName());
                    } else {
                        break;
                    }
                }
                object = context.get(ant.toString());
            } else {
                throw new JexlException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        Object property = null;
        JexlNode propertyNode = left.jjtGetChild(last);
        if (propertyNode instanceof ASTIdentifierAccess) {
            property = evalIdentifier((ASTIdentifierAccess) propertyNode);
            // deal with antish variable
            if (ant != null && object == null) {
                if (last > 0) {
                    ant.append('.');
                }
                ant.append(String.valueOf(property));
                if (assignop != null) {
                    Object self = context.get(ant.toString());
                    right = operators.tryAssignOverload(node, assignop, self, right);
                    if (right == JexlOperator.ASSIGN) {
                        return self;
                    }
                }
                try {
                    context.set(ant.toString(), right);
                } catch (UnsupportedOperationException xsupport) {
                    throw new JexlException(node, "context is readonly", xsupport);
                }
                return right; // 3
            }
        } else if (propertyNode instanceof ASTArrayAccess) {
            // can have multiple nodes - either an expression, integer literal or reference
            int numChildren = propertyNode.jjtGetNumChildren() - 1;
            for (int i = 0; i < numChildren; i++) {
                JexlNode nindex = propertyNode.jjtGetChild(i);
                Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
            propertyNode = propertyNode.jjtGetChild(numChildren);
            property = propertyNode.jjtAccept(this, null);
        } else {
            throw new JexlException(objectNode, "illegal assignment form");
        }
        if (property == null) {
            // no property, we fail
            return unsolvableProperty(propertyNode, "<?>.<null>", true, null);
        }
        if (object == null) {
            // no object, we fail
            return unsolvableProperty(objectNode, "<null>.<?>", true, null);
        }
        // 3: one before last, assign
        if (assignop != null) {
            Object self = getAttribute(object, property, propertyNode);
            right = operators.tryAssignOverload(node, assignop, self, right);
            if (right == JexlOperator.ASSIGN) {
                return self;
            }
        }

        final JexlOperator operator = propertyNode != null && propertyNode.jjtGetParent() instanceof ASTArrayAccess
                                      ? JexlOperator.ARRAY_SET : JexlOperator.PROPERTY_SET;

        setAttribute(object, property, right, propertyNode, operator);
        return right; // 4
    }

    @Override
    protected Object[] visit(ASTArguments node, Object data) {
        int childCount = node.jjtGetNumChildren();
        if (childCount > 0) {
            List<Object> av = new ArrayList<Object> (childCount);
            for (int i = 0; i < childCount; i++) {
                JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTEnumerationNode || child instanceof ASTEnumerationReference) {
                    Iterator<?> it = (Iterator<?>) child.jjtAccept(this, data);
                    if (it != null) {
                       while (it.hasNext()) {
                           Object entry = it.next();
                           av.add(entry);
                       }
                    }
                    closeIfSupported(it);
                } else {
                    Object entry = child.jjtAccept(this, data);
                    av.add(entry);
                }
            }
            return av.toArray();
        } else {
            return EMPTY_PARAMS;
        }
    }

    @Override
    protected Object visit(final ASTMethodNode node, Object data) {
        return visit(node, null, data);
    }

    /**
     * Execute a method call, ie syntactically written as name.call(...).
     * @param node the actual method call node
     * @param object non null when name.call is an antish variable
     * @param data the context
     * @return the method call result
     */
    private Object visit(final ASTMethodNode node, Object object, Object data) {
        // left contains the reference to the method
        final JexlNode methodNode = node.jjtGetChild(0);
        Object method;
        // 1: determine object and method or functor
        if (methodNode instanceof ASTIdentifierAccess) {
            method = methodNode;
            if (object == null) {
                object = data;
                if (object == null) {
                    // no object, we fail
                    return unsolvableMethod(methodNode, "<null>.<?>(...)");
                }
            } else {
                // edge case of antish var used as functor
                method = object;
            }
        } else {
            method = methodNode.jjtAccept(this, data);
        }
        Object result = method;
        for (int a = 1; a < node.jjtGetNumChildren(); ++a) {
            if (result == null) {
                // no method, we fail
                return unsolvableMethod(methodNode, "<?>.<null>(...)");
            }
            ASTArguments argNode = (ASTArguments) node.jjtGetChild(a);
            result = call(node, object, result, argNode);
            object = result;
        }
        return result;
    }

    @Override
    protected Object visit(ASTFunctionNode node, Object data) {
        ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(0);
        String nsid = functionNode.getNamespace();
        Object namespace = (nsid != null)? resolveNamespace(nsid, node) : context;
        ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
        return call(node, namespace, functionNode, argNode);
    }

    /**
     * Calls a method (or function).
     * <p>
     * Method resolution is a follows:
     * 1 - attempt to find a method in the target passed as parameter;
     * 2 - if this fails, seeks a JexlScript or JexlMethod or a duck-callable* as a property of that target;
     * 3 - if this fails, narrow the arguments and try again 1
     * 4 - if this fails, seeks a context or arithmetic method with the proper name taking the target as first argument;
     * </p>
     * *duck-callable: an object where a "call" function exists
     *
     * @param node    the method node
     * @param target  the target of the method, what it should be invoked upon
     * @param functor the object carrying the method or function or the method identifier
     * @param argNode the node carrying the arguments
     * @return the result of the method invocation
     */
    protected Object call(final JexlNode node, Object target, Object functor, final ASTArguments argNode) {
        cancelCheck(node);
        // evaluate the arguments
        final Object[] argv = visit(argNode, null);
        // get the method name if identifier
        final int symbol;
        final String methodName;
        boolean cacheable = cache;
        boolean isavar = false;
        if (functor instanceof ASTIdentifier) {
            // function call, target is context or namespace (if there was one)
            ASTIdentifier methodIdentifier = (ASTIdentifier) functor;
            symbol = methodIdentifier.getSymbol();
            methodName = methodIdentifier.getName();
            functor = null;
            // is it a global or local variable ?
            if (target == context) {
                if (symbol >= 0) {
                    functor = frame.get(symbol);
                    isavar = functor != null;
                } else if (context.has(methodName)) {
                    functor = context.get(methodName);
                    isavar = functor != null;
                }
                // name is a variable, cant be cached
                cacheable &= !isavar;
            }
        } else if (functor instanceof ASTIdentifierAccess) {
            // a method call on target
            methodName = ((ASTIdentifierAccess) functor).getName();
            symbol = -1;
            functor = null;
            cacheable = true;
        } else if (functor != null) {
            // ...(x)(y)
            symbol = -1 - 1; // -2;
            methodName = null;
            cacheable = false;
        } else {
            return unsolvableMethod(node, "?");
        }

        // solving the call site
        CallDispatcher call = new CallDispatcher(node, cacheable);
        try {
            // do we have a  cached version method/function name ?
            Object eval = call.tryEval(target, methodName, argv);
            if (JexlEngine.TRY_FAILED != eval) {
                return eval;
            }
            boolean functorp = false;
            boolean narrow = false;
            // pseudo loop to try acquiring methods without and with argument narrowing
            while (true) {
                call.narrow = narrow;
                // direct function or method call
                if (functor == null || functorp) {
                    // try a method or function from context
                    if (call.isTargetMethod(target, methodName, argv)) {
                        return call.eval(methodName);
                    }
                    if (target == context) {
                        // solve 'null' namespace
                        Object namespace = resolveNamespace(null, node);
                        if (namespace != null
                            && namespace != context
                            && call.isTargetMethod(namespace, methodName, argv)) {
                            return call.eval(methodName);
                        }
                        // do not try context function since this was attempted
                        // 10 lines above...; solve as an arithmetic function
                        if (call.isArithmeticMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                        // could not find a method, try as a property of a non-context target (performed once)
                    } else {
                        // try prepending target to arguments and look for
                        // applicable method in context...
                        Object[] pargv = functionArguments(target, narrow, argv);
                        if (call.isContextMethod(methodName, pargv)) {
                            return call.eval(methodName);
                        }
                        // ...or arithmetic
                        if (call.isArithmeticMethod(methodName, pargv)) {
                            return call.eval(methodName);
                        }
                        // the method may also be a functor stored in a property of the target
                        if (!narrow) {
                            JexlPropertyGet get = uberspect.getPropertyGet(target, methodName);
                            if (get != null) {
                                functor = get.tryInvoke(target, methodName);
                                functorp = functor != null;
                            }
                        }
                    }
                }
                // this may happen without the above when we are chaining call like x(a)(b)
                // or when a var/symbol or antish var is used as a "function" name
                if (functor != null) {
                    // lambda, script or jexl method will do
                    if (functor instanceof JexlScript) {
                        JexlScript s = (JexlScript) functor;
                        boolean varArgs = s.isVarArgs();
                        if (!varArgs && isStrictEngine()) {
                            String[] params = s.getUnboundParameters();
                            int paramCount = params != null ? params.length : 0;
                            int argCount = argv != null ? argv.length : 0;
                            if (argCount > paramCount)
                                return unsolvableMethod(node, "(...)");
                        }

                        return s.execute(context, argv);
                    }
                    if (functor instanceof JexlMethod) {
                        return ((JexlMethod) functor).invoke(target, argv);
                    }
                    final String mCALL = "call";
                    // may be a generic callable, try a 'call' method
                    if (call.isTargetMethod(functor, mCALL, argv)) {
                        return call.eval(mCALL);
                    }
                    // functor is a var, may be method is a global one ?
                    if (isavar && target == context) {
                        if (call.isContextMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                        if (call.isArithmeticMethod(methodName, argv)) {
                            return call.eval(methodName);
                        }
                    }
                    // try prepending functor to arguments and look for
                    // context or arithmetic function called 'call'
                    Object[] pargv = functionArguments(functor, narrow, argv);
                    if (call.isContextMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                    if (call.isArithmeticMethod(mCALL, pargv)) {
                        return call.eval(mCALL);
                    }
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    continue;
                } else {
                    break;
                }
            }
            // we have either evaluated and returned or no method was found
            return unsolvableMethod(node, methodName);
        } catch (JexlException xthru) {
            throw xthru;
        } catch (Exception xany) {
            throw invocationException(node, methodName, xany);
        }
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        final Object target = node.jjtGetChild(0).jjtAccept(this, data);
        // get the ctor args
        int argc = node.jjtGetNumChildren() - 1;
        Object[] argv = argc > 0 ? new Object[argc] : EMPTY_PARAMS;
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i + 1).jjtAccept(this, data);
        }

        try {
            boolean cacheable = cache;
            // attempt to reuse last funcall cached in volatile JexlNode.value
            if (cacheable) {
                Object cached = node.jjtGetValue();
                if (cached instanceof Funcall) {
                    Object eval = ((Funcall) cached).tryInvoke(this, null, target, argv);
                    if (JexlEngine.TRY_FAILED != eval) {
                        return eval;
                    }
                }
            }
            boolean narrow = false;
            JexlMethod ctor = null;
            Funcall funcall = null;
            while (true) {
                // try as stated
                ctor = uberspect.getConstructor(target, argv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new Funcall(ctor, narrow);
                    }
                    break;
                }
                // try with prepending context as first argument
                Object[] nargv = callArguments(context, narrow, argv);
                ctor = uberspect.getConstructor(target, nargv);
                if (ctor != null) {
                    if (cacheable && ctor.isCacheable()) {
                        funcall = new ContextualCtor(ctor, narrow);
                    }
                    argv = nargv;
                    break;
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (!narrow && arithmetic.narrowArguments(argv)) {
                    narrow = true;
                    continue;
                }
                // we are done trying
                break;
            }
            // we have either evaluated and returned or might have found a ctor
            if (ctor != null) {
                Object eval = ctor.invoke(target, argv);
                // cache executor in volatile JexlNode.value
                if (funcall != null) {
                    node.jjtSetValue(funcall);
                }
                return eval;
            }
            String tstr = target != null ? target.toString() : "?";
            return unsolvableMethod(node, tstr);
        } catch (JexlException.Method xmethod) {
            throw xmethod;
        } catch (Exception xany) {
            String tstr = target != null ? target.toString() : "?";
            throw invocationException(node, tstr, xany);
        }
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @return the attribute value
     */
    public Object getAttribute(Object object, Object attribute) {
        return getAttribute(object, attribute, null);
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param node      the node that evaluated as the object
     * @return the attribute value
     */
    protected Object getAttribute(Object object, Object attribute, JexlNode node) {
        if (object == null) {
            throw new JexlException(node, "object is null");
        }
        cancelCheck(node);
        final JexlOperator operator = node != null && node.jjtGetParent() instanceof ASTArrayAccess
                ? JexlOperator.ARRAY_GET : JexlOperator.PROPERTY_GET;
        Object result = operators.tryOverload(node, operator, object, attribute);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        Exception xcause = null;
        try {
            // attempt to reuse last executor cached in volatile JexlNode.value
            if (node != null && cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlPropertyGet) {
                    JexlPropertyGet vg = (JexlPropertyGet) cached;
                    Object value = vg.tryInvoke(object, attribute);
                    if (!vg.tryFailed(value)) {
                        return value;
                    }
                }
            }
            // resolve that property
            List<PropertyResolver> resolvers = uberspect.getResolvers(operator, object);
            JexlPropertyGet vg = uberspect.getPropertyGet(resolvers, object, attribute);
            if (vg != null) {
                Object value = vg.invoke(object);
                // cache executor in volatile JexlNode.value
                if (node != null && cache && vg.isCacheable()) {
                    node.jjtSetValue(vg);
                }
                return value;
            }
        } catch (Exception xany) {
            xcause = xany;
        }
        // lets fail
        if (node != null) {
            boolean safe = (node instanceof ASTIdentifierAccess) && ((ASTIdentifierAccess) node).isSafe();
            if (safe) {
                return null;
            } else {
                String attrStr = attribute != null ? attribute.toString() : null;
                return unsolvableProperty(node, attrStr, true, xcause);
            }
        } else {
            // direct call
            String error = "unable to get object property"
                    + ", class: " + object.getClass().getName()
                    + ", property: " + attribute;
            throw new UnsupportedOperationException(error, xcause);
        }
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     */
    public void setAttribute(Object object, Object attribute, Object value) {
        setAttribute(object, attribute, value, null, JexlOperator.PROPERTY_SET);
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     * @param node      the node that evaluated as the object
     */
    protected void setAttribute(Object object, Object attribute, Object value, JexlNode node, JexlOperator operator) {
        cancelCheck(node);
        Object result = operators.tryOverload(node, operator, object, attribute, value);
        if (result != JexlEngine.TRY_FAILED) {
            return;
        }
        Exception xcause = null;
        try {
            // attempt to reuse last executor cached in volatile JexlNode.value
            if (node != null && cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlPropertySet) {
                    JexlPropertySet setter = (JexlPropertySet) cached;
                    Object eval = setter.tryInvoke(object, attribute, value);
                    if (!setter.tryFailed(eval)) {
                        return;
                    }
                }
            }
            List<PropertyResolver> resolvers = uberspect.getResolvers(operator, object);
            JexlPropertySet vs = uberspect.getPropertySet(resolvers, object, attribute, value);
            // if we can't find an exact match, narrow the value argument and try again
            if (vs == null) {
                // replace all numbers with the smallest type that will fit
                Object[] narrow = {value};
                if (arithmetic.narrowArguments(narrow)) {
                    vs = uberspect.getPropertySet(resolvers, object, attribute, narrow[0]);
                }
            }
            if (vs != null) {
                // cache executor in volatile JexlNode.value
                vs.invoke(object, value);
                if (node != null && cache && vs.isCacheable()) {
                    node.jjtSetValue(vs);
                }
                return;
            }
        } catch (Exception xany) {
            xcause = xany;
        }
        // lets fail
        if (node != null) {
            String attrStr = attribute != null ? attribute.toString() : null;
            unsolvableProperty(node, attrStr, true, xcause);
        } else {
            // direct call
            String error = "unable to set object property"
                    + ", class: " + object.getClass().getName()
                    + ", property: " + attribute
                    + ", argument: " + value.getClass().getSimpleName();
            throw new UnsupportedOperationException(error, xcause);
        }
    }

    @Override
    protected Object visit(ASTJxltLiteral node, Object data) {
        TemplateEngine.TemplateExpression tp = (TemplateEngine.TemplateExpression) node.jjtGetValue();
        if (tp == null) {
            TemplateEngine jxlt = jexl.jxlt();
            tp = jxlt.parseExpression(node.jexlInfo(), node.getLiteral(), frame != null ? frame.getScope() : null);
            node.jjtSetValue(tp);
        }
        if (tp != null) {
            return tp.evaluate(frame, context);
        }
        return null;
    }

    @Override
    protected Object visit(ASTAnnotation node, Object data) {
        throw new UnsupportedOperationException(ASTAnnotation.class.getName() + ": Not supported.");
    }

    @Override
    protected Object visit(ASTAnnotatedStatement node, Object data) {
        return processAnnotation(node, 0, data);
    }

    /**
     * Processes an annotated statement.
     * @param stmt the statement
     * @param index the index of the current annotation being processed
     * @param data the contextual data
     * @return  the result of the statement block evaluation
     */
    protected Object processAnnotation(final ASTAnnotatedStatement stmt, final int index, final Object data) {
        // are we evaluating the block ?
        final int last = stmt.jjtGetNumChildren() - 1;
        if (index == last) {
            JexlNode block = stmt.jjtGetChild(last);
            // if the context has changed, might need a new interpreter
            final JexlArithmetic jexla = arithmetic.options(context);
            if (jexla != arithmetic) {
                if (!arithmetic.getClass().equals(jexla.getClass())) {
                    logger.warn("expected arithmetic to be " + arithmetic.getClass().getSimpleName()
                            + ", got " + jexla.getClass().getSimpleName()
                    );
                }
                Interpreter ii = new Interpreter(Interpreter.this, jexla);
                Object r = block.jjtAccept(ii, data);
                if (ii.isCancelled()) {
                    Interpreter.this.cancel();
                }
                return r;
            } else {
                return block.jjtAccept(Interpreter.this, data);
            }
        }
        // tracking whether we processed the annotation
        final boolean[] processed = new boolean[]{false};
        final Callable<Object> jstmt = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                processed[0] = true;
                try {
                    return processAnnotation(stmt, index + 1, data);
                } catch (JexlException.Return xreturn) {
                    return xreturn;
                } catch (JexlException.Break xbreak) {
                    return xbreak;
                } catch (JexlException.Continue xcontinue) {
                    return xcontinue;
                } catch (JexlException.Remove xremove) {
                    return xremove;
                }
            }
        };
        // the annotation node and name
        final ASTAnnotation anode = (ASTAnnotation) stmt.jjtGetChild(index);
        final String aname = anode.getName();
        // evaluate the arguments
        Object[] argv = anode.jjtGetNumChildren() > 0
                        ? visit((ASTArguments) anode.jjtGetChild(0), null) : null;
        // wrap the future, will recurse through annotation processor
        Object result;
        try {
            result = processAnnotation(aname, argv, jstmt);
            // not processing an annotation is an error
            if (!processed[0]) {
                return annotationError(anode, aname, null);
            }
        } catch (JexlException xany) {
            throw xany;
        } catch (Exception xany) {
            return annotationError(anode, aname, xany);
        }
        // the caller may return a return, break or continue
        if (result instanceof JexlException) {
            throw (JexlException) result;
        }
        return result;
    }

    /**
     * Delegates the annotation processing to the JexlContext if it is an AnnotationProcessor.
     * @param annotation    the annotation name
     * @param args          the annotation arguments
     * @param stmt          the statement / block that was annotated
     * @return the result of statement.call()
     * @throws Exception if anything goes wrong
     */
    protected Object processAnnotation(String annotation, Object[] args, Callable<Object> stmt) throws Exception {
        return context instanceof JexlContext.AnnotationProcessor
                ? ((JexlContext.AnnotationProcessor) context).processAnnotation(annotation, args, stmt)
                : stmt.call();
    }

    protected Iterator<?> prepareIndexedIterator(JexlNode node, Object iterableValue) {

        if (iterableValue != null) {
            Object forEach = operators.tryOverload(node, JexlOperator.FOR_EACH_INDEXED, iterableValue);
            Iterator<?> itemsIterator = forEach instanceof Iterator
                                    ? (Iterator<?>) forEach
                                    : uberspect.getIndexedIterator(iterableValue);
            return itemsIterator;
        }

        return null;
    }

    protected abstract class IteratorBase implements Iterator<Object> {

        protected final Iterator<?> itemsIterator;
        protected final JexlNode node;

        protected int i;

        protected IteratorBase(Iterator<?> iterator, JexlNode projection) {
            itemsIterator = iterator;
            node = projection;

            i = 0;
        }

        protected Object[] prepareArgs(ASTJexlLambda lambda, Object data) {

            int argCount = lambda.getArgCount();
            boolean varArgs = lambda.isVarArgs();

            Object[] argv = null;

            if (argCount == 0) {
                argv = EMPTY_PARAMS;
            } else if (argCount == 1) {
                argv = new Object[] {data};
            } else if (!varArgs && data instanceof Object[]) {
                int len = ((Object[]) data).length;
                if (argCount > len) {
                    argv = new Object[len + 1];
                    argv[0] = i;
                    System.arraycopy(data, 0, argv, 1, len);
                } else if (argCount == len) {
                    argv = (Object[]) data;
                } else {
                    argv = new Object[] {i, data};
                }
            } else {
                argv = new Object[] {i, data};
            }

            return argv;
        }
    }

    public class ProjectionIterator extends IteratorBase {

        protected Map<Integer,Closure> scripts;

        protected ProjectionIterator(Iterator<?> iterator, JexlNode projection) {
            super(iterator, projection);

            scripts = new HashMap<Integer,Closure> ();
            i = -1;
        }

        protected Object evaluateProjection(int i, Object data) {
            JexlNode child = node.jjtGetChild(i);

            if (child instanceof ASTJexlLambda) {
                ASTJexlLambda lambda = (ASTJexlLambda) child;
                Closure c = scripts.get(i);
                if (c == null) {
                    c = new Closure(Interpreter.this, lambda);
                    scripts.put(i, c);
                }
                Object[] argv = prepareArgs(lambda, data);
                return c.execute(null, argv);
            } else {
                return child.jjtAccept(Interpreter.this, data);
            }
        }

        @Override
        public boolean hasNext() {
            return itemsIterator.hasNext();
        }

        @Override
        public Object next() {

            cancelCheck(node);

            Object data = itemsIterator.next();

            i += 1;

            // can have multiple nodes
            int numChildren = node.jjtGetNumChildren();

            if (numChildren == 1) {
                return evaluateProjection(0, data);
            } else {
                Object[] value = new Object[numChildren];
                for (int child = 0; child < numChildren; child++) {
                    value[child] = evaluateProjection(child, data);
                }
                return value;
            }
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    @Override
    protected Object visit(ASTProjectionNode node, Object data) {
        Iterator<?> itemsIterator = prepareIndexedIterator(node, data);
        return itemsIterator != null ? new ProjectionIterator(itemsIterator, node) : null;
    }

    public class MapProjectionIterator extends ProjectionIterator {

        protected MapProjectionIterator(Iterator<?> iterator, JexlNode projection) {
            super(iterator, projection);
        }

        @Override
        public Object next() {

            cancelCheck(node);

            Object data = itemsIterator.next();

            i += 1;

            Object key = evaluateProjection(0, data);
            Object value = evaluateProjection(1, data);

            return new AbstractMap.SimpleEntry<Object,Object> (key, value);
        }

    }

    @Override
    protected Object visit(ASTMapProjectionNode node, Object data) {
        Iterator<?> itemsIterator = prepareIndexedIterator(node, data);
        return itemsIterator != null ? new MapProjectionIterator(itemsIterator, node) : null;
    }

    public class SelectionIterator extends IteratorBase {

        protected final Closure closure;

        protected Object nextItem;
        protected boolean hasNextItem;

        protected SelectionIterator(Iterator<?> iterator, ASTJexlLambda filter) {
            super(iterator, filter);
            closure = new Closure(Interpreter.this, filter);
        }

        protected void findNextItem() {
            if (!itemsIterator.hasNext()) {
                hasNextItem = false;
                nextItem = null;
            } else {
                Object data = null;
                boolean selected = false;

                do {
                    data = itemsIterator.next();
                    Object[] argv = prepareArgs((ASTJexlLambda) node, data);
                    selected = arithmetic.toBoolean(closure.execute(null, argv));
                } while (!selected && itemsIterator.hasNext());

                if (selected) {
                    hasNextItem = true;
                    nextItem = data;
                }
            }
        }

        @Override
        public boolean hasNext() {

            if (!hasNextItem)
                findNextItem();

            return hasNextItem;
        }

        @Override
        public Object next() {
            cancelCheck(node);

            if (!hasNextItem)
                findNextItem();

            if (!hasNextItem)
                throw new NoSuchElementException();

            i += 1;
            hasNextItem = false;

            return nextItem;
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    public class StopCountIterator extends IteratorBase {

        protected final int limit;

        protected StopCountIterator(Iterator<?> iterator, JexlNode node, int stopCount) {
            super(iterator, node);
            limit = stopCount;
        }

        @Override
        public boolean hasNext() {
            return itemsIterator.hasNext() && i < limit;
        }

        @Override
        public Object next() {
            cancelCheck(node);

            if (!hasNext())
                throw new NoSuchElementException();

            i += 1;

            return itemsIterator.next();
        }

        @Override
        public void remove() {
            itemsIterator.remove();
        }
    }

    @Override
    protected Object visit(ASTSelectionNode node, Object data) {
        JexlNode child = node.jjtGetChild(0);

        if (child instanceof ASTJexlLambda) {
           ASTJexlLambda script = (ASTJexlLambda) child;
           Iterator<?> itemsIterator = prepareIndexedIterator(child, data);
           return itemsIterator != null ? new SelectionIterator(itemsIterator, script) : null;
        } else {
           int stopCount = arithmetic.toInteger(child.jjtAccept(this, null));
           Iterator<?> itemsIterator = prepareIndexedIterator(child, data);
           return itemsIterator != null ? new StopCountIterator(itemsIterator, node, stopCount) : null;
        }
    }

    @Override
    protected Object visit(ASTReductionNode node, Object data) {
        int numChildren = node.jjtGetNumChildren();

        ASTJexlLambda reduction = null;
        Object result = null;

        if (numChildren > 1) {
            result = node.jjtGetChild(0).jjtAccept(this, null);
            reduction = (ASTJexlLambda) node.jjtGetChild(1);
        } else {
            reduction = (ASTJexlLambda) node.jjtGetChild(0);
        }

        Iterator<?> itemsIterator = prepareIndexedIterator(node, data);

        if (itemsIterator != null) {

            Closure closure = new Closure(this, reduction);

            boolean varArgs = reduction.isVarArgs();
            int argCount = reduction.getArgCount();

            int i = 0;

            while (itemsIterator.hasNext()) {
                Object value = itemsIterator.next();

                Object[] argv = null;

                if (argCount == 0) {
                    argv = EMPTY_PARAMS;
                } else if (argCount == 1) {
                    argv = new Object[] {result};
                } else if (argCount == 2) {
                    argv = new Object[] {result, value};
                } else if (argCount == 3) {
                    argv = new Object[] {result, i, value};
                } else if (value instanceof Map.Entry<?,?>) {
                    Map.Entry<?,?> entry = (Map.Entry<?,?>) value;
                    argv = new Object[] {result, i, entry.getKey(), entry.getValue()};
                } else if (!varArgs && value instanceof Object[]) {

                    int len = ((Object[]) value).length;
                    if (argCount > len + 1) {
                       argv = new Object[len + 2];
                       argv[0] = result;
                       argv[2] = i;
                       System.arraycopy(value, 0, argv, 2, len);
                    } else if (argCount == len + 1) {
                       argv = new Object[len + 1];
                       argv[0] = result;
                       System.arraycopy(value, 0, argv, 1, len);
                    } else {
                       argv = new Object[] {result, i, value};
                    }

                } else {
                    argv = new Object[] {result, i, value};
                }

                result = closure.execute(null, argv);

                i += 1;
            }
        }

        return result;
    }

}
