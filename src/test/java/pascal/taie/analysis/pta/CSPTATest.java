/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.TestUtils;

public class CSPTATest {
    
    private static final String DIR = "cspta";

    // Tests for context insensitivity
    @Test
    public void testNew() {
        TestUtils.testPTA(DIR, "New", "-pp");
    }

    @Test
    public void testAssign() {
        TestUtils.testPTA(DIR, "Assign", "-pp");
    }

    @Test
    public void testStoreLoad() {
        TestUtils.testPTA(DIR, "StoreLoad", "-pp");
    }

    @Test
    public void testCall() {
        TestUtils.testPTA(DIR, "Call", "-pp");
    }

    @Test
    public void testAssign2() {
        TestUtils.testPTA(DIR, "Assign2", "-pp");
    }

    @Test
    public void testInstanceField() {
        TestUtils.testPTA(DIR, "InstanceField", "-pp");
    }

    @Test
    public void testInstanceField2() {
        TestUtils.testPTA(DIR, "InstanceField2", "-pp");
    }

    @Test
    public void testCallParamRet() {
        TestUtils.testPTA(DIR, "CallParamRet", "-pp");
    }

    @Test
    public void testCallField() {
        TestUtils.testPTA(DIR, "CallField", "-pp");
    }
    
    // Tests for context sensitivity variants
    @Test
    public void testOneCall() {
        TestUtils.testPTA(DIR, "OneCall", "-pp", "pta=cs:1-call");
    }

    @Test
    public void testOneObject() {
        TestUtils.testPTA(DIR, "OneObject", "-pp", "pta=cs:1-obj");
    }

    @Test
    public void testOneType() {
        TestUtils.testPTA(DIR, "OneType", "-pp", "pta=cs:1-type");
    }

    @Test
    public void testTwoCall() {
        TestUtils.testPTA(DIR, "TwoCall", "-pp", "pta=cs:2-call");
    }

    @Test
    public void testTwoObject() {
        TestUtils.testPTA(DIR, "TwoObject", "-pp", "pta=cs:2-obj");
    }

    @Test
    public void testTwoType() {
        TestUtils.testPTA(DIR, "TwoType", "-pp", "pta=cs:2-type");
    }

    // Tests for Java feature supporting
    @Test
    public void testStaticField() {
        TestUtils.testPTA(DIR, "StaticField", "-pp");
    }

    @Test
    public void testArray() {
        TestUtils.testPTA(DIR, "Array", "-pp");
    }

    @Test
    public void testCast() {
        TestUtils.testPTA(DIR, "Cast", "-pp");
    }

    @Test
    public void testNull() {
        TestUtils.testPTA(DIR, "Null", "-pp");
    }

    @Test
    public void testPrimitive() {
        TestUtils.testPTA(DIR, "Primitive", "-pp");
    }

    @Test
    public void testStrings() {
        TestUtils.testPTA(DIR, "Strings");
    }

    @Test
    public void testMultiArray() {
        TestUtils.testPTA(DIR, "MultiArray", "-pp");
    }

    @Test
    public void testClinit() {
        TestUtils.testPTA(DIR, "Clinit");
    }

    @Test
    public void testClassObj() {
        TestUtils.testPTA(DIR, "ClassObj");
    }

    // Tests for handling of non-normal objects
    @Test
    public void testTypeSens() {
        TestUtils.testPTA(DIR, "TypeSens", "pta=cs:2-type");
    }

    @Test
    public void testSpecialHeapContext() {
        TestUtils.testPTA(DIR, "SpecialHeapContext", "pta=cs:2-object");
    }

    @Test
    public void testNativeModel() {
        TestUtils.testPTA(DIR, "NativeModel", "-pp");
    }
}
