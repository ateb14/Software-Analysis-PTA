package test;

import benchmark.internal.Benchmark;

public class StaticField{
    public static void main(String [] args){
        Benchmark.alloc(1);
        BB b1 = new BB();
        Benchmark.alloc(2);
        BB b2 = new BB();
        Benchmark.alloc(3);
        BB b3 = new BB();
        Benchmark.alloc(4);
        AA a1 = new AA(b1);

        Benchmark.alloc(5);
        AA a2 = new AA();

        AA.allBB = b2;
        AA.allBB = b3;

        Benchmark.test(1, AA.allBB);
        Benchmark.test(2, a1.myBB);
        Benchmark.test(3, a2.myBB);
    }
}
class AA{
    public static BB allBB;
    public BB myBB;
    AA(BB b){
        myBB = b;
    }

    AA(){
    }
}
class BB{
    public BB(){

    }
}
