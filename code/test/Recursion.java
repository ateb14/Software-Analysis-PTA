package test;

import benchmark.internal.Benchmark;
import benchmark.objects.A;

public class Recursion{

    public static A cycle_1(A a1, A a2, A a3, int cnt){
        if(cnt <= 0){
            return a1;
        }
        return cycle_2(a1, a2, a3, cnt-1);
    }

    public static A cycle_2(A a1, A a2, A a3, int cnt){
        if(cnt <= 0){
            return a2;
        }
        return cycle_3(a1, a2, a3, cnt-1);
    }

    public static A cycle_3(A a1, A a2, A a3, int cnt){
        if(cnt <= 0){
            return a3;
        }
        return cycle_1(a1, a2, a3, cnt-1);
    }

    public static A cycle_with_val1(int cnt){
        if(cnt <= 0 ){
            Benchmark.alloc(4);
            return new A();
        }
        return cycle_with_val2(cnt - 1);
    }

    public static A cycle_with_val2(int cnt){
        if(cnt <= 0 ){
            Benchmark.alloc(5);
            return new A();
        }
        return cycle_with_val1(cnt - 1);
    }

    public static A recursive_func(A a1, A a2, int cnt){
        if(cnt > 100){
            return a2;
        }
        if(cnt <= 0){
            return a1;
        }

        return recursive_func(a1,a2,cnt - 1);
    }

    public static void main(String [] args){
        Benchmark.alloc(1);
        A a1 = new A();
        Benchmark.alloc(2);
        A a2 = new A();
        Benchmark.alloc(3);
        A a3 = new A();

        A aa = recursive_func(a1,a2,10);
        A aaa = cycle_1(a1, a2, a3,10);

        A a4 = cycle_with_val1(10);

        Benchmark.test(1, aa);
        Benchmark.test(2, aaa);
        Benchmark.test(3, a4);

    }
}
