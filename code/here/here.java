

public class here {

    public static void main(String[] args) {
//        String a = new String("abc");
//        String b = new String("def");
//        if(args.length > 1 ){
//            a = b;
//        }
//        int x = 10;
//        int y = inc(x) + dec(x) + inc(3 * x);

//        A obj_a1 = new A(10);
        A obj_a2 = new A(20);
//        if(args.length < 1){
//            obj_a2 = obj_a1;
//        }
//        obj_a2.dec();

        obj_a2.modify_b(new Obj.B(88));

//        obj_a1.a = obj_a2.a;
//        int [] arr = new int[5];
//        arr[3] = 222;
//        arr[2] = arr[3];

//        System.out.println(y);
//        System.out.println(a);
//        System.out.println(obj_a2.a);
//        System.out.println(obj_a2.obj_b.value);
    }


    public static int inc(int a){
        return a + 1;
    }

    public static int dec(int a){
        return a - 1;
    }

}
