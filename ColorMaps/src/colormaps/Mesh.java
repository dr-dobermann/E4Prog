package colormaps;

import java.util.*;

/**
 * Mesh represent a free grapf of Nodes linked by Links
 * @author Dober
 *
 */
public class Mesh {
	List<Node> nodes;
	
	// Setter and Getters
	//-------------------------------------------------------------------------
	public Node[] GetNodes() {
		return nodes.toArray(new Node[0]);
	}
	
	public Node GetNodeByName(String name) {
		for ( Node node : nodes )
			if ( node.GetName() == name )
				return node;
		
		return null;
	}
	
	
	
	
	// Constructors
	//-------------------------------------------------------------------------
	public Mesh() {
		nodes = new LinkedList<Node>();
	}
	
	
	
	
	// Functionality
	//-------------------------------------------------------------------------
	
	/**
	 * Adds new node to the mash
	 * Doesn't allow duplication
	 * @param name -- Name of a new node
	 */
	public void AddNode(String name) {
		
		Node node = GetNodeByName(name);
		
		if ( node == null ) {
			node = new Node(name);
			nodes.add(node);
		}	
	}
	
	/**
	 * Binds two nodes by link
	 * @param name1 -- Name of the first node
	 * @param name2 -- Name of the second node
	 */
	public void LinkTwoNodes(String name1, String name2) {
		Node 
			node1 = GetNodeByName(name1),
			node2 = GetNodeByName(name2);
		
		if ( node1 == null || node2 == null )
			return;
		
		node1.LinkTo(node2);
		
	}
}
