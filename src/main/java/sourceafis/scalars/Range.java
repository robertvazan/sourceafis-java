package sourceafis.scalars;

public class Range {
	public final int start;
	public final int end;
	public Range(int start, int end) {
		this.start = start;
		this.end = end;
	}
	public Range(int length) {
		this(0, length);
	}
	public int length() {
		return end - start;
	}
	public int interpolate(int nth, int of) {
		return start + (nth * length() + of / 2) / of;
	}
}
