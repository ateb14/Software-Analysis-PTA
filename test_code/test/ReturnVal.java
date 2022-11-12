package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;
import benchmark.objects.B;

public class ReturnVal{

    private static A a1;

    public static void assign(A x, A y) {
        y.f = x.f;
    }

    public static B allocateA(){
        Benchmark.alloc(9);
        new B();
        Benchmark.alloc(10);
        B cen = new B();
        return cen;
    }

    public static B give(B giver){
        return giver;
    }

    public static void empty_ret(){
        Benchmark.alloc(11);
        new A();
    }

    public static void main(String[] args){
        Benchmark.alloc(1);
        B b1 = new B();
        Benchmark.alloc(2);
        B b2 = new B();

        B rb = allocateA();

        b1 = give(b2);

        empty_ret();
//        Benchmark.alloc(4);
//        A a2 = new A(b2);

        Benchmark.test(1, rb); // expect: 10
        Benchmark.test(2, b1); // expect: 1 2
    }
}
