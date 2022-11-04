

public class here {

    public static void main(String[] args) {
        String a = new String();
        String b = new String();
        if(args.length > 1 ){
            a = b;
        }
        int x = 10;
        int y = inc(x) + dec(x) + inc(3 * x);
        System.out.println(y);
    }

    public static int inc(int a){
        return a + 1;
    }

    public static int dec(int a){
        return a - 1;
    }

}
