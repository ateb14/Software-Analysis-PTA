package test;

import benchmark.internal.Benchmark;
import benchmark.internal.BenchmarkN;
import benchmark.objects.A;
import benchmark.objects.B;

public class OurTests{
    public static void main(String [] args){
        BenchmarkN.alloc(50);
        XHZTest xhztest = new XHZTest();
        xhztest.main();

        Benchmark.alloc(51);
        WYYTest wyytest = new WYYTest();
        wyytest.main();

    }
}

class WYYTest{
    public static A cycle_1(A a1, int cnt){
        if(cnt <= 0){
            return a1;
        }
        return cycle_2(a1, cnt-1);
    }

    public static A cycle_2(A a1, int cnt){
        if(cnt <= 0){
            return a1;
        }
        return cycle_3(a1,cnt-1);
    }

    public static A cycle_3(A a1, int cnt){
        if(cnt <= 0){
            return a1;
        }
        return cycle_1(a1, cnt-1);
    }

    public A give(A a){
        return a;
    }

    public void main(){
        Benchmark.alloc(30);
        B cycle_b = new B();
        Benchmark.alloc(31);
        A cycle_a = give(new A(cycle_b));

        Benchmark.alloc(32);
        A a1 = new A();
        Benchmark.alloc(33);
        A a2 = new A();
        Benchmark.alloc(34);
        A a3 = new A();
        Benchmark.alloc(35);
        A a4 = new A();
        Benchmark.alloc(36);
        A a5 = new A();
        Benchmark.alloc(37);
        A a6 = new A();
        Benchmark.alloc(38);
        A a7 = new A();
        Benchmark.alloc(39);
        A a8 = new A();
        Benchmark.alloc(40);
        A a9 = new A();
        Benchmark.alloc(41);
        A a10 = new A();

        Benchmark.alloc(42);
        B tied_b = new B();
        a1.f = tied_b;
        a2.f = a1.f;
        A tied_a1 = a2;
        A tied_a2 = tied_a1;
        a4.f = tied_a2.f;

        // passing arguments and returning values on a cyclic method calling path
        Benchmark.test(30, cycle_2(cycle_a, 10).f); // expect 30
        // context sensitivity
        BenchmarkN.test(31, give(a1)); // expect 32
        BenchmarkN.test(32, give(a2)); // expect 33
        BenchmarkN.test(33, give(a3)); // expect 34
        BenchmarkN.test(34, give(a4)); // expect 35
        BenchmarkN.test(35, give(a5)); // expect 36
        BenchmarkN.test(36, give(a6)); // expect 37
        BenchmarkN.test(37, give(a7)); // expect 38
        BenchmarkN.test(38, give(a8)); // expect 39
        BenchmarkN.test(39, give(a9)); // expect 40
        BenchmarkN.test(40, give(a10)); // expect 41
        // objects and fields
        BenchmarkN.test(41, give(a4).f); // expect 42
        BenchmarkN.test(42, give(give(a4)).f); // expect 42
        BenchmarkN.test(43, give(give(give(a4))).f); // expect 42
    }
}

class XHZTest {

    static class obj{}

    interface Grandpa
    {
        obj identity(obj o);
    }

    static class Father implements Grandpa
    {
        @Override
        public obj identity(obj o) {
            return o;
        }
    }

    static class Son extends Father
    {
        obj o;
        public Son(obj o)
        {
            this.o = o;
        }
        @Override
        public obj identity(obj o) {
            return this.o;
        }
    } // 这个儿子长得像叔叔，喜当爹了（x


    static class Uncle implements Grandpa
    {
        obj o;
        public Uncle(obj o)
        {
            this.o = o;
        }
        @Override
        public obj identity(obj o) {
            return this.o;
        }
    }

    static class Grandson extends Son
    {
        public Grandson(obj o)
        {
            super(o);
        }
    } // Empty

    public void main()
    {
        BenchmarkN.alloc(1);
        obj o1 = new obj();
        BenchmarkN.alloc(2);
        obj o2 = new obj();
        BenchmarkN.alloc(3);
        obj o3 = new obj();
        BenchmarkN.alloc(4);
        obj o4 = new obj();
        BenchmarkN.alloc(5);
        obj o5 = new obj();
        BenchmarkN.alloc(6);
        obj o6 = new obj();
        BenchmarkN.alloc(7);
        obj o7 = new obj();
        BenchmarkN.alloc(8);
        obj o8 = new obj();
        BenchmarkN.alloc(9);
        obj o9 = new obj();
        BenchmarkN.alloc(10);
        obj o10 = new obj();
        BenchmarkN.alloc(11);
        obj o11 = new obj();
        BenchmarkN.alloc(12);
        obj o12 = new obj();
        BenchmarkN.alloc(13);
        obj o13 = new obj();
        BenchmarkN.alloc(14);
        obj o14 = new obj();
        BenchmarkN.alloc(15);
        obj o15 = new obj();

        BenchmarkN.alloc(16);
        Son son = new Son(o2);
        BenchmarkN.alloc(17);
        Grandson gson = new Grandson(o10);
        BenchmarkN.alloc(18);
        Grandson gson11 = new Grandson(o11);
        BenchmarkN.alloc(19);
        Grandson gson12 = new Grandson(o12);
        BenchmarkN.alloc(20);
        Grandson gson13 = new Grandson(o13);
        BenchmarkN.alloc(21);
        Grandson gson14 = new Grandson(o14);
        BenchmarkN.alloc(22);
        Grandson gson15 = new Grandson(o15);
        Grandpa uncleOrFather, fatherOrSon;
        Grandpa father = new Father();
        if(o1.equals(o2))
        {
            BenchmarkN.alloc(23);
            uncleOrFather = new Uncle(o3);
            BenchmarkN.alloc(24);
            fatherOrSon = new Father();
        }
        else
        {
            BenchmarkN.alloc(25);
            uncleOrFather = new Father();
            BenchmarkN.alloc(26);
            fatherOrSon = new Son(o3);
        }

        BenchmarkN.test(1, father.identity(o1)); // expects 1
        BenchmarkN.test(2, son.identity(o1)); // expects 2
        BenchmarkN.test(3, gson.identity(o3)); // expects 10
        BenchmarkN.test(4, gson.identity(o4)); // expects 10
        BenchmarkN.test(5, gson.identity(o5)); // expects 10
        BenchmarkN.test(6, gson.identity(o6)); // expects 10
        BenchmarkN.test(7, gson.identity(o7)); // expects 10
        BenchmarkN.test(8, gson.identity(o8)); // expects 10
        BenchmarkN.test(9, gson.identity(o9)); // expects 10
        BenchmarkN.test(10, gson.identity(o1)); // expects 10
        BenchmarkN.test(11, gson11.identity(o1)); // expects 11
        BenchmarkN.test(12, gson12.identity(o1)); // expects 12
        BenchmarkN.test(13, gson13.identity(o1)); // expects 13
        BenchmarkN.test(14, gson14.identity(o1)); // expects 14
        BenchmarkN.test(15, gson15.identity(o1)); // expects 15
        BenchmarkN.test(101, uncleOrFather.identity(o4)); // expects 3 or 4
        BenchmarkN.test(102, fatherOrSon.identity(o4)); // expects 3 or 4

    }
}

