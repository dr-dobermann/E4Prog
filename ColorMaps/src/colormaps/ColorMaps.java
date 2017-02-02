package colormaps;

import java.util.*;

/**
 * Represents the colormap solver
 * @author Dober
 *
 */
public class ColorMaps {

	public enum Color {
		UNSPECIFIED,
		COLOR1, 
		COLOR2,
		COLOR3,
		COLOR4,
		COLOR5,
		COLOR6;
		
		static public Color GetNext(Color col) {
			switch ( col ) {
			case COLOR1 : return COLOR2; 
			case COLOR2 : return COLOR3; 
			case COLOR3 : return COLOR4; 
			case COLOR4 : return COLOR5; 
			case COLOR5 : return COLOR6; 
			}
			
			return UNSPECIFIED;			
		}
		
	};
	
	private Mesh mesh;
	private Map<String, Color> colormap;
	
	
	// Setters and getters
	//-------------------------------------------------------------------------
	public Mesh GetMesh() {
		return mesh;
	}
	
	public Map<String, Color> GetColorMap()
	{
		return colormap;
	}
	
	
	// Constructors
	//-------------------------------------------------------------------------
	public ColorMaps() {
		mesh = new Mesh();
		colormap = new HashMap<String, Color>();
	}
	
	
	
	
	// Functionality
	//-------------------------------------------------------------------------
	/**
	 * Loads mesh from given list of node names and links between them
	 * @param nodes -- List of Node names
	 * @param linkDescriptors -- List of LinkDescriptors
	 */
	public void LoadMesh(String[] nodes, List<String[]> linkDescriptors) {
		for ( String nodeName : nodes )
			mesh.AddNode(nodeName);
		
		for ( String[] ld : linkDescriptors )
			mesh.LinkTwoNodes(ld[0], ld[1]);
	}
	
	/**
	 * Solves the ColorMaps stored in mesh
	 */
	public void SolveColorMaps() {
		
		Vector<Color> aClr = new Vector<Color>(6);
		
		colormap.clear();
		for ( Node node : mesh.GetNodes() )
			colormap.put(node.GetName(), Color.UNSPECIFIED);
		
		for ( String state : colormap.keySet() ) {
			// create set of available colors
			aClr.clear();
			for ( Color clr : Color.values() )
				if ( clr != Color.UNSPECIFIED )
					aClr.add(clr);

			// remove all color which is already in use
			for ( Link link : mesh.GetNodeByName(state).GetLinks() ) {
				Color col = colormap.get(link.GetOppositeNode(mesh.GetNodeByName(state)).GetName());
				if ( col != Color.UNSPECIFIED )
					aClr.remove(col);
			}
			
			if ( aClr.size() == 0 )
				throw new RuntimeException("There are no available colors for state " + state);
			
			colormap.put(state, aClr.get(0));
		}
			
		
	}
}
