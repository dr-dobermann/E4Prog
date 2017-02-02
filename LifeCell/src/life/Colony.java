package life;

import java.util.*;

class Colony {
	Map<Point, Cell> cells;
	int generation;
	int id;
	int width, height;
	Point origin;
	int cellCounter;
	
	public Colony(int newID, Point origin) {
		id = newID;
		cells = new HashMap<Point, Cell>();
		generation = 0;
		this.origin = origin;
		width = 0;
		height = 0;
		cellCounter = 0;
	}
	
	public int LoadField(List<Point> points) {
		
		for ( Point p : points )
			AddNewCell(p);
		
		return cells.size();
	}
	
	public Point GetBounds() {
		return new Point(width, height);
	}
	
	public Collection<Cell> GetCells() {
		return cells.values();
	}
	
	public int GetID() {
		return id;
	}
	
	public int GetCurrGeneration() {
		return generation;
	}
	
	public Point GetOrigin() {
		return origin;
	}
	
	public static void ShiftColony(Colony col, int x, int y) {
		col.origin.x += x;
		col.origin.y += y;
	}
	
	/**
	 * Converts two-dimension array of integers into listed points pattern.
	 * Array presents map of cell as 1 and empty space as 0 
	 * @param arr -- two-dimension array
	 * @param rows -- rows in array
	 * @param cols -- columns in every row
	 * @return
	 */
	public static List<Point> ConvertArr2PointsList(int[][] arr, int rows, int cols) {
		List<Point> points = new LinkedList<Point>();
		
		for ( int r = 0; r < rows; r++ )
			for ( int c = 0; c < cols; c++ )
				if ( arr[r][c] == 1 )
					points.add(new Point(c, r));
		
		return points;
	}

	/**
	 * Converts colony of cells into an array of integers.
	 * Every 1 represents a cells and every 0 represents an empty space.
	 * @param col -- colony to analyze
	 * @return two-dimension integer array
	 */
	public static int[][] ConvertColony2Array(Colony col) {
		
		int[][] pattern = new int[col.height][col.width];
		
		for ( Cell cell : col.cells.values() )
			pattern[cell.GetPosition().y][cell.GetPosition().x] = 1;
		
		return pattern;		
	}
	
	/**
	 * Adds a new cell into position pos
	 * @param pos
	 */
	private void AddNewCell(Point pos) {
		
		if ( cells.containsKey(pos) )
				throw new RuntimeException("The position [" + pos.x + ":" + pos.y + 
						                   "] already occupied by cell [" + cells.get(pos).GetID() + 
						                   " in the colony [" + id + "]!!!");
		
		Cell cell = new Cell(this, cellCounter++, pos);
		cells.put(pos, cell);
		
		// expand the colony bounds
		if ( pos.x >= width )
			width = pos.x + 1;
		if ( pos.y >= height )
			height = pos.y + 1;
		
		// meet with a new neighbourhood
		if ( cells.size() < 2 ) // if there is only one cell yet, no need to check neighbourhood
			return;
		
		Point chkPoint;
		for ( int dir = 0; dir < 8; dir++ ) {
			chkPoint = Cell.GetOppositeDirPoint(cell, dir);
			if ( cells.containsKey(chkPoint) ) {
				cell.Bind(cells.get(chkPoint), dir);  							// bind a new cell to an existed one
				cells.get(chkPoint).Bind(cell, Cell.GetOppositeDir(dir));	 	// bind an existed cell to a new one
			}
		}
	}
	
	/**
	 * Returns a number of neighbours for a given position
	 * @param p -- Point to check
	 * @return 
	 */
	private int GetNeighboursCount(Point p) {
		
		int nCount = 0;
		
		// N
		if ( p.y > 0 && cells.containsKey(new Point(p.x, p.y - 1) ) )
			nCount++;
		// NE
		if ( p.y > 0 && p.x < width - 1 && cells.containsKey(new Point(p.x + 1, p.y - 1) ) )
			nCount++;
		// E
		if ( p.x < width - 1 && cells.containsKey(new Point(p.x + 1, p.y) ) )
			nCount++;
		// SE
		if ( p.y < height - 1 && p.x < width - 1 && cells.containsKey(new Point(p.x + 1, p.y + 1) ) )
			nCount++;
		// S
		if ( p.y < height - 1 && cells.containsKey(new Point(p.x, p.y + 1) ) )
			nCount++;
		// SW
		if ( p.y < height -1 && p.x > 0 && cells.containsKey(new Point(p.x - 1, p.y + 1) ) )
			nCount++;
		// W
		if ( p.x > 0 && cells.containsKey(new Point(p.x - 1, p.y) ) )
			nCount++;
		// NW
		if ( p.y > 0 && p.x > 0 && cells.containsKey(new Point(p.x - 1, p.y - 1) ) )
			nCount++;
		
		return nCount;
	}

	/**
	 * Shifts all cell and expands the colony bounds if any cell has negative position
	 */
	private void NormalizeField() {
		
		boolean needShift = true;
		int shiftX, shiftY; 		// the global shift of the whole field
		int minX, minY;				// minimal X and Y position of cell to detect of empty space from left and top sides.
		
		while ( needShift ) {
			shiftX = 0;
			shiftY = 0;

			minX = width - 1;	 
			minY = height - 1;
			
			width = 0;
			height = 0;

			for ( Cell cell : cells.values() ) {
				if ( cell.GetPosition().x < 0 ) {
					shiftX = 1;
					origin.x--;  // if we expand the field to the left,
								 // we also shift the colony origin to the left as well.
								 // Colony moves to the left
				}
				if ( cell.GetPosition().y < 0 ) {
					shiftY = 1;
					origin.y--;  // in case colony field need to expand up, we shift 
								 // the colony origin to the top too. The whole colony moves to the top.
				}
				
				if ( cell.GetPosition().x < minX )
					minX = cell.GetPosition().x;
				if ( cell.GetPosition().y < minY )
					minY = cell.GetPosition().y;
				

				if ( width <= cell.GetPosition().x )
					width = cell.GetPosition().x + 1;
				if ( height <= cell.GetPosition().y )
					height = cell.GetPosition().y + 1;
			}
			
			if ( shiftX == 0 && shiftY == 0 ) {
				if ( minX > 0 ) {	// shift the whole field to the left if there are empty columns
					shiftX = -minX;
					width -= minX;
					origin.x += minX; // move colony to the right if we trim an empty space from the left
				}
				if ( minY > 0 ) {
					shiftY = -minY;	// shift the whole field to the top if there are empty rows
					height -= minY;
					origin.y += minY; // move colony to the bottom if we trim an empty rows from the top
				}
				if ( shiftX == 0 && shiftY == 0 ) {
					needShift = false;
					continue;
				}
			}
						
			Point np;
			Map<Point, Cell> newCells = new HashMap<Point, Cell>();
			for ( Cell cell : cells.values() ) {
				np = new Point(cell.GetPosition().x + shiftX, cell.GetPosition().y + shiftY);
				cell.SetPosition(np);
				newCells.put(np, cell);
			}
			
			cells = newCells;
		}
	}
	
	/**
	 * Changes current generation onto a new one
	 */
	public void NextGeneration() {
		// switch generation onto a next one
		generation++;
		
		int nCount; 
		
		Set<Point> emptySlots = new HashSet<Point>(); 		// pretenders for a creating a new born cell
		List<Cell> dyingCells = new LinkedList<Cell>();		// a list of cell to die in a next generation
		List<Point> newCells = new LinkedList<Point>();		// a list of a newly created cells 
		
		// check all cells lived in past generation and prepare a list of dying cells
		for ( Cell cell : cells.values() ) {
			emptySlots.addAll(cell.GetEmptyNeighboursSlots());
			
			nCount = cell.GetNeighboursCount(true);
			
			if ( nCount < 2 )				// it's too few neighbours over there
				dyingCells.add(cell);
			
			if ( nCount > 3 )				// it's too crowdy over there
				dyingCells.add(cell);
		}
		
		// check all emptySlots if it possible to create a new cell over there
		for ( Point es : emptySlots ) {
			if ( GetNeighboursCount(es) == 3 )
				newCells.add(es);
		}
		
		// delete all dying cells
		for ( Cell cell : dyingCells ) {
			cell.Die();
			cells.remove(cell.GetPosition());
		}
		
		// set new generation for the existed cells
		for ( Cell cell : cells.values() )
			cell.SetGeneration(generation);
		
		// add all new cells
		for ( Point p : newCells )
			AddNewCell(p);
		
		NormalizeField();
	}
	
	/**
	 * Merges the colony col to the current one
	 * @param col -- Colony to merge
	 */
	public void MergeColony(Colony col) {
		int x, y;
		for ( Cell cell : col.cells.values() ) {
			if ( origin.x + width == col.origin.x ) 	// if the new col is next to the right
				x = width + cell.GetPosition().x;
			else
				if ( origin.x >= col.origin.x )			// new col is placed left to the current one
					x = origin.x - col.origin.x + cell.GetPosition().x;
				else
					x = col.origin.x - origin.x + cell.GetPosition().x;
			
			if ( origin.y + height == col.origin.y )	// if the new col is next to the bottom
				y = height + cell.GetPosition().y;
			else
				if ( origin.y >= col.origin.y )        // if the new col is higher that the current one
					y = origin.y - col.origin.y + cell.GetPosition().y;
				else
					y = col.origin.y - origin.y + cell.GetPosition().y;
			
			AddNewCell(new Point(x, y));
		}
			
	}

}
