package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;

public class ContextSensitivity{

    public static A give(A a){
        return a;
    }

    public static void main(String [] args){
        Benchmark.alloc(1);
        A a1 = new A();
        Benchmark.alloc(2);
        A a2 = new A();
        Benchmark.alloc(3);
        A a3 = new A();
        Benchmark.alloc(4);
        A a4 = new A();
        Benchmark.alloc(5);
        A a5 = new A();

        A x1 = give(a1);
        A x2 = give(a2);
        A x3 = give(a3);
        A x4 = give(a4);
        A x5 = give(a5);
        A x6 = give(a1);

        Benchmark.test(1, x1);
        Benchmark.test(2, x2);
        Benchmark.test(3, x3);
        Benchmark.test(4, x4);
        Benchmark.test(5, x5);
        Benchmark.test(6, x6);
    }
}
