package test;

import benchmark.internal.BenchmarkN;

public class XHZTest {

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

    public static void main(String[] args)
    {
        benchmark.internal.BenchmarkN.alloc(1);
        obj o1 = new obj();
        benchmark.internal.BenchmarkN.alloc(2);
        obj o2 = new obj();
        benchmark.internal.BenchmarkN.alloc(3);
        obj o3 = new obj();
        benchmark.internal.BenchmarkN.alloc(4);
        obj o4 = new obj();
        benchmark.internal.BenchmarkN.alloc(5);
        obj o5 = new obj();
        benchmark.internal.BenchmarkN.alloc(6);
        obj o6 = new obj();
        benchmark.internal.BenchmarkN.alloc(7);
        obj o7 = new obj();
        benchmark.internal.BenchmarkN.alloc(8);
        obj o8 = new obj();
        benchmark.internal.BenchmarkN.alloc(9);
        obj o9 = new obj();
        benchmark.internal.BenchmarkN.alloc(10);
        obj o10 = new obj();
        benchmark.internal.BenchmarkN.alloc(11);
        obj o11 = new obj();
        benchmark.internal.BenchmarkN.alloc(12);
        obj o12 = new obj();
        benchmark.internal.BenchmarkN.alloc(13);
        obj o13 = new obj();
        benchmark.internal.BenchmarkN.alloc(14);
        obj o14 = new obj();
        benchmark.internal.BenchmarkN.alloc(15);
        obj o15 = new obj();

        Son son = new Son(o2);
        Grandson gson = new Grandson(o10);
        Grandson gson11 = new Grandson(o11);
        Grandson gson12 = new Grandson(o12);
        Grandson gson13 = new Grandson(o13);
        Grandson gson14 = new Grandson(o14);
        Grandson gson15 = new Grandson(o15);
        Grandpa uncleOrFather, fatherOrSon;
        Grandpa father = new Father();
        if(o1.equals(o2))
        {
            uncleOrFather = new Uncle(o3);
            fatherOrSon = new Father();
        }
        else
        {
            uncleOrFather = new Father();
            fatherOrSon = new Son(o3);
        }
        benchmark.internal.BenchmarkN.test(1, father.identity(o1)); // expects 1
        benchmark.internal.BenchmarkN.test(2, son.identity(o1)); // expects 2
        benchmark.internal.BenchmarkN.test(3, gson.identity(o3)); // expects 10
        benchmark.internal.BenchmarkN.test(4, gson.identity(o4)); // expects 10
        benchmark.internal.BenchmarkN.test(5, gson.identity(o5)); // expects 10
        benchmark.internal.BenchmarkN.test(6, gson.identity(o6)); // expects 10
        benchmark.internal.BenchmarkN.test(7, gson.identity(o7)); // expects 10
        benchmark.internal.BenchmarkN.test(8, gson.identity(o8)); // expects 10
        benchmark.internal.BenchmarkN.test(9, gson.identity(o9)); // expects 10
        benchmark.internal.BenchmarkN.test(10, gson.identity(o1)); // expects 10
        benchmark.internal.BenchmarkN.test(11, gson11.identity(o1)); // expects 11
        benchmark.internal.BenchmarkN.test(12, gson12.identity(o1)); // expects 12
        benchmark.internal.BenchmarkN.test(13, gson13.identity(o1)); // expects 13
        benchmark.internal.BenchmarkN.test(14, gson14.identity(o1)); // expects 14
        benchmark.internal.BenchmarkN.test(15, gson15.identity(o1)); // expects 15
        benchmark.internal.BenchmarkN.test(101, uncleOrFather.identity(o4)); // expects 3 or 4
        benchmark.internal.BenchmarkN.test(102, fatherOrSon.identity(o4)); // expects 3 or 4

    }
}
