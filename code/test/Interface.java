package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.*;

public class Interface {
    public static void main(String [] args){
        BenchmarkN.alloc(1);
        I g = new G();
        BenchmarkN.alloc(2);
        H h = new H();
        BenchmarkN.alloc(3);
        A a1 = new A();
        BenchmarkN.alloc(4);
        A a2 = new A();

        A a3 = g.foo(a1);

        BenchmarkN.test(1, a3); // expect: 3

//        A a4 = h.foo(a2);


//        BenchmarkN.test(2, a4); // expect: 4
//        if(g instanceof G) {
//            G gg = (G)g;
//            BenchmarkN.test(3, gg.a); // expect 3
//        }
//        BenchmarkN.test(4, h.a); // expect 4
    }
}
