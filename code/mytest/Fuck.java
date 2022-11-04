// import test.Var;
package test;

public class Fuck {

    public static void main(String[] args) {
        Var a = new Var();
        Var b = new Var();
        if (args.length > 1){
            a = b;
        }
        System.out.println(a.i);
        System.out.println(b.i);
    }
}
