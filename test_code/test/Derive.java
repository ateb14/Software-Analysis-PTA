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
        Son son1 = new Son(a1);
        Benchmark.alloc(4);
        Son son2 = new Son(a1);

        son2.alias(a2);

        Benchmark.alloc(5);
        B b1 = new B();

        Benchmark.test(1, son1.getA()); // expect: 1

        Benchmark.test(2, son2.getA()); // expect: (1) 2


        Father father3 = new Son(a1);
        father3.qb = b1;
        Benchmark.test(3, father3.qb); // expect 5
    }
}
