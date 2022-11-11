package test;

import benchmark.internal.BenchmarkN;

public class XHZTest {

    static class obj{};

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

        Son son = new Son(o2);
        Grandpa uncleOrFather;
        Grandpa father = new Father();
        if(1==1)
        {
            uncleOrFather = new Uncle(o3);
        }
        else
        {
            uncleOrFather = new Father();
        }
        benchmark.internal.BenchmarkN.test(1, father.identity(o1)); // expects 1
        benchmark.internal.BenchmarkN.test(2, son.identity(o1)); // expects 2
        benchmark.internal.BenchmarkN.test(3, uncleOrFather.identity(o4)); // expects 3 or 4

    }
}
