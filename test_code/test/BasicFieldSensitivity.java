package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

public class BasicFieldSensitivity {
    public static void main(String [] args){
        Benchmark.alloc(1);
        A a1 = new A();
        Benchmark.alloc(2);
        A a2 = new A();
        Benchmark.alloc(3);
        B b1 = new B();
        Benchmark.alloc(4);
        B b2 = new B();

        a1.f = b1;
        a2.f = b2;

        Benchmark.test(1, a1.f); // expect : 3
        Benchmark.test(2, a2.f); // expect : 4
    }
}
