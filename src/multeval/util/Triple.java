package multeval.util;

public class Triple<S1, S2, S3> {
	public S1 first;
	public S2 second;
	public S3 third;
	
	public Triple(S1 first, S2 second, S3 third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	@Override
	public int hashCode() {
		return first.hashCode() ^ second.hashCode() ^ third.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Triple) {
			Triple<?, ?, ?> t = (Triple<?, ?, ?>) obj;
			return t.first.equals(first) && t.second.equals(second) && t.third.equals(third);
		} else {
			return false;
		}
	}
}
