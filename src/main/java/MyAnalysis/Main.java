package MyAnalysis;

public class Main {
    public static void main(String [] args){
        if(args.length < 2){
            return;
        }
        pascal.taie.Main.main(new String[]{"-cp",args[0],"-m",args[1],"-java", "7", "-a" ,"my_pta"});
    }
}
