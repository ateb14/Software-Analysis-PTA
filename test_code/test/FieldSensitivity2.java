//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class FieldSensitivity2 {
    public FieldSensitivity2() {
    }

    private void assign(A var1, A var2) {
        var2.f = var1.f;
    }

    private void test() {
        BenchmarkN.alloc(1);
        B var1 = new B();
        BenchmarkN.alloc(2);
        A var2 = new A(var1);
        BenchmarkN.alloc(3);
        A var3 = new A();
        BenchmarkN.alloc(4);
        new B();
        this.assign(var2, var3);
        B var5 = var3.f;
        BenchmarkN.test(1, var5); // expect 1
    }

    public void test2() {
        BenchmarkN.alloc(5);
        B var1 = new B();
        BenchmarkN.alloc(6);
        B var2 = new B();
        BenchmarkN.alloc(7);
        A var3 = new A();
        BenchmarkN.alloc(8);
        A var4 = new A();
        var3.f = var1;
        var4.f = var2;
        BenchmarkN.test(2, var3.f); // expect 5
        BenchmarkN.test(3, var4.f); // expect 6
    }

    public static void main(String[] var0) {
        BenchmarkN.alloc(9);
        FieldSensitivity2 var1 = new FieldSensitivity2();
        var1.test();
        var1.test2();
    }
}
