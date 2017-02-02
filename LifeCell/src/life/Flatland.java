package life;

import java.util.*;

public class Flatland {
	
	int fWidth, fHeight;

	List<Colony> colonies;
	
	/**
	 * Returns bounds of the flatland
	 * @return integer array of bounds. 0 - X, 1 - Y
	 */
	public int[] GetBounds() {
		
		int[] bounds = {fWidth, fHeight,};
		
		return bounds;
	}
	
	public Flatland() {
		colonies = new ArrayList<Colony>();
		fWidth = 0;
		fHeight = 0;
	}
	
	public void AddNewColony(Point origin, List<Point> pattern) {
		
		Colony col = new Colony(colonies.size(), origin);
		
		if ( pattern != null )
			col.LoadField(pattern);
		
		if ( fWidth <= col.GetOrigin().x + col.GetBounds().x )
			fWidth = col.GetOrigin().x + col.GetBounds().x;
			
		if ( fHeight <= col.GetOrigin().y + col.GetBounds().y )
			fHeight = col.GetOrigin().y + col.GetBounds().y;
		
		colonies.add(col);
	}
	
	public List<Colony> GetColoniesList() {
		return colonies;
	}
	
	public int ColoniesCount() {
		return colonies.size();
	}
	
	public void NextGeneration() {
		
		for ( Colony col : colonies )
			col.NextGeneration();
		
		// compensate the common field size according to colonies movement
		int shiftX, shiftY;
		while ( true ) {
			
			shiftX = 0;
			shiftY = 0;
			for ( Colony col : colonies ) {
				if ( col.GetOrigin().x < 0 ) {
					shiftX = -col.GetOrigin().x;
					break;
				}
				if ( col.GetOrigin().y < 0 ) {
					shiftY = -col.GetOrigin().y;
					break;
				}
			}
			if ( shiftX == 0 && shiftY == 0 )
				break;
			
			for ( Colony col : colonies )
				Colony.ShiftColony(col, shiftX, shiftY);
		}
		// check if colonies bumping
		// if so, the colonies should be merged
		boolean merged = true;
		while ( merged ) {
			for ( Colony col : colonies )
			{
				merged = false;
				for ( Colony chkCol : colonies )
				{
					if ( col != chkCol && ( 
						 // check collision on the right side of col
						 (col.GetOrigin().x + col.GetBounds().x == chkCol.GetOrigin().x &&													
						  ((col.GetOrigin().y <= chkCol.GetOrigin().y && col.GetOrigin().y + col.GetBounds().y >= chkCol.GetOrigin().y) ||
						   (chkCol.GetOrigin().y <= col.GetOrigin().y && chkCol.GetOrigin().y + chkCol.GetBounds().y >= col.GetOrigin().y))) ||
						 // check collision on the bottom side of col
						 (col.GetOrigin().y + col.GetBounds().y == chkCol.GetOrigin().y &&													
						  ((col.GetOrigin().x <= chkCol.GetOrigin().x && col.GetOrigin().x + col.GetBounds().x >= chkCol.GetOrigin().x) ||
						   (chkCol.GetOrigin().x <= col.GetOrigin().x && chkCol.GetOrigin().x + chkCol.GetBounds().x >= col.GetOrigin().x)))
				       )) {
						col.MergeColony(chkCol);
						colonies.remove(chkCol);
						merged = true;
						break;
					}
				}
				if ( merged )
					break;
			}
		}

		
		// recalculate flatland bounds
		fWidth = 0;
		fHeight = 0;
		for ( Colony col : colonies ) {
			if ( fWidth <= col.GetOrigin().x + col.GetBounds().x )
				fWidth = col.GetOrigin().x + col.GetBounds().x;
				
			if ( fHeight <= col.GetOrigin().y + col.GetBounds().y )
				fHeight = col.GetOrigin().y + col.GetBounds().y;
		}
	
	}

}
