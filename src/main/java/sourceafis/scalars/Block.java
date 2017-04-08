package sourceafis.scalars;

public class Block {
	public final int x;
	public final int y;
	public final int width;
	public final int height;
	public Block(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	public int left() {
		return x;
	}
	public int bottom() {
		return y;
	}
	public int right() {
		return x + width;
	}
	public int top() {
		return y + height;
	}
	public static Block between(Cell start, Cell end) {
		return new Block(start.x, start.y, end.x - start.x, end.y - start.y);
	}
	public Cell center() {
		return new Cell((right() + left()) / 2, (bottom() + top()) / 2);
	}
}
