package life;

public class Point {
	public int x;
	public int y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public boolean equals(Object o) {
		return x == ((Point)o).x && y == ((Point)o).y;
	}
	
	@Override
	public int hashCode() {
		int hash = x + 1013904223;
		return hash * 1664525 + y + 1013904223;
	}
	
	
}
