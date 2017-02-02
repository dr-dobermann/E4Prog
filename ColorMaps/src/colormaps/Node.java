package colormaps;

import java.util.*;

/**
 * Node represents a node with all its links to other nodes
 * @author Dober
 *
 */
public class Node {
	private String name;
	private List<Link> links;
	
	// Setters and getters
	//-------------------------------------------------------------------------
	public String GetName() {
		return name;
	}
	
	public Link[] GetLinks() {
		return links.toArray(new Link[0]);
		
	}
	
	public int GetLinkCount() {
		return links.size();
	}
	
	
	
	// Constructors
	//-------------------------------------------------------------------------
	public Node(String name) {
		this.name = name;
		links = new LinkedList<Link>();
	}
	
	
	
	
	// Functionality
	//-------------------------------------------------------------------------
	
	/**
	 * Links node to another one
	 * Checks for duplicate links
	 * @param node -- another node to link to
	 */
	public void LinkTo(Node node) {
		
		Link link = new Link(this, node, "");
		if ( CheckForDuplicates(link))
			return;
		
		links.add(link);
		node.RegisterLink(link);
		
	}
	
	/**
	 * Register link to the node
	 * Checks for duplication and circular links
	 * @param link -- link to register
	 */
	public void RegisterLink(Link link) {
		
		// check if link consists the node itself
		if ( !link.IsContainsNode(this) ) 
			return;
		
		// check for duplicates
		if ( CheckForDuplicates(link) )
			return;
		
		links.add(link);
	}

	/**
	 * Returns a list of conneted Nodes
	 * @return List of connected nodes
	 */
	public Node[] GetConnectedNodes() {
		List<Node> connections = new ArrayList<Node>();
		
		for ( Link link : links )
			connections.add(link.GetOppositeNode(this));
		
		return (Node[])connections.toArray();
	}
	
	/**
	 * Check links list for duplications with link
	 * @param link -- Link to check
	 * @return true if link already exists in links list
	 */
	private boolean CheckForDuplicates(Link link) {
		
		for ( Link lnk : links ) 
			if ( lnk.equals(link) )
				return true;
		
		return false;
	}
	
}
