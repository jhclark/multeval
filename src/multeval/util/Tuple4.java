package multeval.util;

public class Tuple4<S1, S2, S3, S4> {
	public S1 first;
	public S2 second;
	public S3 third;
	public S4 fourth;
	
	public Tuple4(S1 first, S2 second, S3 third, S4 fourth) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
	}
	
	@Override
	public int hashCode() {
		return first.hashCode() ^ second.hashCode() ^ third.hashCode() ^ fourth.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Tuple4) {
			Tuple4<?, ?, ?, ?> t = (Tuple4<?, ?, ?, ?>) obj;
			return t.first.equals(first) && t.second.equals(second) && t.third.equals(third) && t.fourth.equals(fourth);
		} else {
			return false;
		}
	}
}
