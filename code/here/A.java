import Obj.*;

public class A{
    int a;
    B obj_b = new B(114514);
    B obj_b2;
    public A(int a_){
        a = a_;
    }

    public void inc(){
        a += 1;
    }

    public void dec(){
        a -= 1;
    }

    public void modify_b(B new_b){
       this.obj_b = new_b;
       B bobj = new B(1919810);
       obj_b2 = bobj;
    }
}
