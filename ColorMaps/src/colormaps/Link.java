package colormaps;


/**
 * Link represents a link between two different Nodes.
 * Circullar links aren't allowed
 * 
 * @author Dober
 *
 */
public class Link {
	private Node[] ends;
	private String name;
	
	// Getters and Setters
	//-------------------------------------------------------------------------
	public String GetName() {
		return name;
	}

	public Node[] GetNodes() {
		return ends;
	}

	
	
	
	// Constructors
	//-------------------------------------------------------------------------
	public Link(Node end1, Node end2, String name) {
		
		// Circullar links are not allowed
		if ( end1 == end2 )
			throw new RuntimeException("Circular link detected on Node " + end1.GetName());
		
		ends = new Node[2];
		ends[0] = end1;
		ends[1] = end2;
		if ( !name.isEmpty() )
			this.name = name;
		else
			this.name = end1.GetName() + "-" + end2.GetName();
	}
	
	
	
	
	// Functionality
	// ------------------------------------------------------------------------
	
	/**
	 * Checks if nodes are ends of the link
	 * @param end1 -- Node 1
	 * @param end2 -- Node 2
	 * @return true if nodes are linked by the link
	 */
	public boolean AreNodesLinked(Node end1, Node end2) {
		return ( end1 == ends[0] && end2 == ends[1] ) || ( end2 == ends[0] && end1 == ends[1] );
	}
	
	@Override
	public boolean equals(Object o) {
		return AreNodesLinked( ((Link)o).ends[0], ((Link)o).ends[1] ); 
	}
	
	/**
	 * Checks if the link contains the node
	 * @param node -- Node to check
	 * @return true if any end is the node
	 */
	public boolean IsContainsNode(Node node) {
		return node == ends[0] || node == ends[1];
	}
	
	/**
	 * Returns opposite to given Node end of the link
	 * @param node -- Node to check
	 * @return Node, which is opposed to the given one
	 */
	public Node GetOppositeNode(Node node) {
		if ( !IsContainsNode(node) )
			throw new RuntimeException("Node " + node.GetName() + " doesn't linked to link " + GetName());
		
		return node == ends[0] ? ends[1] : ends[0];
	}
	
	/**
	 * Returns link opposite node by name
	 * @param nName -- node name to look opposite to
	 * @return Node opposed to given Name
	 */
	public Node GetOppositeNode(String nName) {
		if ( nName != ends[0].GetName() && nName != ends[1].GetName() )
			throw new RuntimeException("Node " + nName + " doesn't linked to link " + GetName());
		
		return nName == ends[0].GetName() ? ends[1] : ends[0];
	}
	
}
