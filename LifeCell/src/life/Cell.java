package life;

import java.util.*;

class Cell {
	
	public static final int N  = 0,
							NE = 1,
							E  = 2,
							SE = 3,
							S  = 4,
							SW = 5,
							W  = 6,
							NW = 7;
	
	int generation;
	Cell[] neighbours;
	Colony colony;
	int id;
	Point pos;
	int age;
	
	public Cell(Colony col, int newID, Point pos) {
		id = newID;
		colony = col;
		generation = col.GetCurrGeneration();
		neighbours = new Cell[8];
		this.pos = pos;
		age = 1;
	}

	public int GetID() {
		return id;
	}
	
	public Point GetPosition() {
		return pos;
	}
	
	public void SetPosition(Point newPos) {
		pos = newPos;
	}
	
	public int GetGeneration() {
		return generation;
	}
	
	public void SetGeneration(int newGen) {
		if ( newGen > generation )
			generation = newGen;
		age++;
	}
	
	public int GetAge() {
		return age;
	}
	
	public void Bind(Cell cell, int dir){
		if ( neighbours[dir] != null && neighbours[dir] != cell ) {
			throw new RuntimeException("Position [" + dir + "] is already occupied in Cell[" + colony.GetID() + "." + id + "]!!!");
		}
		
		neighbours[dir] = cell;
	}
	
	public void UnBind(int dir) {
		neighbours[dir] = null;
	}
	
	/**
	 * Returns an opposite direction index
	 * 0 - N, 1 - NE 2 - E, 3 - SE, 4 - S, 5 - SW, 6 - W, 7 - NW
	 * @param dir
	 * @return opposite direction index for a given direction
	 */
	static public int GetOppositeDir(int dir) {
		
		int oppDir;
		
		switch ( dir ) {
			case N  : 	oppDir = S; 	break;
			case NE : 	oppDir = SW; 	break;
			case E  : 	oppDir = W; 	break;
			case SE : 	oppDir = NW; 	break;
			case S  : 	oppDir = N; 	break;
			case SW : 	oppDir = NE; 	break;
			case W  : 	oppDir = E; 	break;
			case NW : 	oppDir = SE; 	break;
			default:
				throw new RuntimeException("Invalid direction [" + dir + "]");
		}
		
		return oppDir;
	}

	/**
	 * Returns a point for an opposite direction for the current cell
	 * @param dir -- direction to look for
	 * @return Point opposed to direction from the cell
	 */
	static public Point GetOppositeDirPoint(Cell cell, int dir) {
		Point res = new Point(cell.pos.x, cell.pos.y);
		
		switch (dir ) {
			case N : 
				res.y--;
				break;
			case NE :
				res.y--;
				res.x++;
				break;
			case E :
				res.x++;
				break;
			case SE :
				res.y++;
				res.x++;
				break;
			case S :
				res.y++;
				break;
			case SW :
				res.y++;
				res.x--;
				break;
			case W :
				res.x--;
				break;
			case NW :
				res.y--;
				res.x--;
				break;
			default:
				throw new RuntimeException("Invalid direction [" + dir + "]");
		}
		return res;
	}
	
	/**
	 * Unbind all the neighbours for a dying cell
	 */
	public void Die() {
		for ( int dir = 0; dir < 8 ; dir ++ ) {
			if ( neighbours[dir] != null )
				neighbours[dir].UnBind(GetOppositeDir(dir));
		}
	}

	/**
	 * Returns number of cell's neighbours.
	 * @param sameGen -- if sameGen is true, only neighbours with same generation count. If it's false all neighbours' count returns
	 * @return
	 */
	public int GetNeighboursCount(boolean sameGen) {
		
		int nCount = 0;
		
		for ( Cell cell : neighbours ) 
			if ( cell != null )
				if ( sameGen ) {
					if ( cell.generation == generation )
						nCount++;
				}
				else
					nCount++;
				
		
		return nCount;
	}
	
	/**
	 * Returns all empty slots around the cell
	 * @return list of empty slots
	 */
	public List<Point> GetEmptyNeighboursSlots() {
		
		List<Point> emptySlots = new ArrayList<Point>();
		
		for ( int dir = 0; dir < 8; dir ++ ) 
			if ( neighbours[dir] == null ) {
				Point emptySlot = GetOppositeDirPoint(this, dir);
				emptySlots.add(emptySlot);
			}
		
		return emptySlots;
	}
		
}
