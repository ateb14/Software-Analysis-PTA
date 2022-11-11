package benchmark.objects;

public class Father {
	// Class P extends class Q

	public A a;

	public B qb;

	public Father(A a) {
		this.a = a;
	}

	public void alias(A x) {
		this.a = x;
	}

	public A getA(){
		return a;
	}
}
