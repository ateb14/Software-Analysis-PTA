package test;

import benchmark.internal.Benchmark;
import benchmark.objects.*;

public class Derive {
    public static void main(String []args){
        Benchmark.alloc(1);
        A a1 = new A();
        Benchmark.alloc(2);
        A a2 = new A();
        Benchmark.alloc(3);
        P p1 = new P(a1);
        Benchmark.alloc(4);
        P p2 = new P(a1);

        p2.alias(a2);

        Benchmark.alloc(5);
        B b1 = new B();

        Benchmark.test(1, p1.getA()); // expect: 1

        Benchmark.test(2, p2.getA()); // expect: (1) 2


        Q p3 = new P(a1);
        p3.qb = b1;
        Benchmark.test(3, p3.qb); // expect 5
    }
}
