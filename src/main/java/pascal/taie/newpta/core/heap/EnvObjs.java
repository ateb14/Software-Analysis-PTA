/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.newpta.core.heap;

import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.types.Type;

import static pascal.taie.java.classes.StringReps.STRING;
import static pascal.taie.java.classes.StringReps.THREAD;
import static pascal.taie.java.classes.StringReps.THREAD_GROUP;

/**
 * Manage objects that are implicitly created by Java runtime environment.
 */
public class EnvObjs {

    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    EnvObjs(TypeManager typeManager) {
        mainThread = new EnvObj("<main-thread>",
                typeManager.getClassType(THREAD), null);
        systemThreadGroup = new EnvObj("<system-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        mainThreadGroup = new EnvObj("<main-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        Type string = typeManager.getClassType(STRING);
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new EnvObj("<main-arguments>",
                stringArray, World.getMainMethod());
        mainArgsElem = new EnvObj("<main-arguments-element>",
                string, World.getMainMethod());
    }

    public Obj getMainThread() {
        return mainThread;
    }

    public Obj getSystemThreadGroup() {
        return systemThreadGroup;
    }

    public Obj getMainThreadGroup() {
        return mainThreadGroup;
    }

    public Obj getMainArgs() {
        return mainArgs;
    }

    public Obj getMainArgsElem() {
        return mainArgsElem;
    }
}
