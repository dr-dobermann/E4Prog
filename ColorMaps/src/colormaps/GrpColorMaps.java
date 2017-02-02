package colormaps;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.*;
import java.util.*;

/**
 * Draws the colormaps and highlits an active state with its links
 * @author Dober
 *
 */
public class GrpColorMaps extends JLabel
						  implements Scrollable, 
						             MouseMotionListener {
	final int circleSize = 40,
			  space      = 60;
	ColorMaps cmap;
	Map<String, ColorMaps.Color> scmap;

	Vector<String> states;
	int activeStateID = -1;
	
	
	
	
	
	// Constructors
	//-------------------------------------------------------------------------
	public GrpColorMaps(ColorMaps cmap) {
		this.cmap = cmap;
		scmap = cmap.GetColorMap();

		states = new Vector<String>(scmap.size());
		states.addAll(scmap.keySet());

		setOpaque(true);
		addMouseMotionListener(this);
	}
	
	
	
	
	
	// Functionality
	//-------------------------------------------------------------------------
	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		
		Graphics2D g2 = (Graphics2D)g;
		
		setSize(8 * circleSize + 7 * space + 1, 6 * circleSize + 5 * space + 1);
		
		
		BasicStroke simpleStroke = new BasicStroke(1.0f);
		BasicStroke accentedStroke = new BasicStroke(3.0f);
		
		// Draw links between the Nodes
		int end1, end2;
		for ( String state : states )
			for ( Link link : cmap.GetMesh().GetNodeByName(state).GetLinks() ) {
				end1 = states.indexOf(state);
				end2 = states.indexOf(link.GetOppositeNode(state).GetName());
				if ( end1 == activeStateID ) {
					g2.setStroke(accentedStroke);
					g.setColor(GetColor(scmap.get(state), false));
				}
				else {
					g2.setStroke(simpleStroke);
					g.setColor(Color.GRAY);			
				}
				g.drawLine( end1 % 8 * (circleSize + space) + circleSize / 2, 
						    end1 / 8 * (circleSize + space) + circleSize / 2,
						    end2 % 8 * (circleSize + space) + circleSize / 2,
						    end2 / 8 * (circleSize + space) + circleSize / 2);
			}
		// Draw nodes
		for ( String state : states ) {
			g.setColor(Color.WHITE);
			g.fillOval(states.indexOf(state) % 8 * (circleSize + space),
					   states.indexOf(state) / 8 * (circleSize + space),
					   circleSize,
					   circleSize);
			end1 = states.indexOf(state);
			if ( end1 == activeStateID ) {
				g.setColor(GetColor(scmap.get(state), false));
				g2.setStroke(accentedStroke);
			}
			else {
				g.setColor(GetColor(scmap.get(state), true));
				g2.setStroke(simpleStroke);
			}
			g.drawOval(states.indexOf(state) % 8 * (circleSize + space),
					   states.indexOf(state) / 8 * (circleSize + space),
					   circleSize,
					   circleSize);
			g.drawString(state, 
						 states.indexOf(state) % 8 * (circleSize + space) + (circleSize < 20 ? 4 :(circleSize - 15) / 2),
						 states.indexOf(state) / 8 * (circleSize + space) + circleSize / 2 + 4);
		}
		
	}
	
	/**
	 * Returns one of 6 predefined colors in bright or shaded mode
	 * @param clr 		-- ColorMaps.Color code
	 * @param shaded 	-- return shaded color if true, bright one if false
	 * @return java.awt.Color
	 */
	private Color GetColor(ColorMaps.Color clr, boolean shaded) {
		
		final int[][] colors = {
								{255,   0,   0,},  // RED
								{  0,  64,   0,},  // GREEN
								{  0,   0, 255,},  // BLUE
								{128,   0, 255,},  // MAGENTA
								{255, 128,  64,},  // ORANGE
								{128,  64,  64,},  // BROWN
							   };
		Color col;
		int alpha = shaded ? 200 : 255;
		
		switch ( clr ) {
			case COLOR1 : col = new Color(colors[0][0], colors[0][1], colors[0][2], alpha); break;
			case COLOR2 : col = new Color(colors[1][0], colors[1][1], colors[1][2], alpha); break;
			case COLOR3 : col = new Color(colors[2][0], colors[2][1], colors[2][2], alpha); break;
			case COLOR4 : col = new Color(colors[3][0], colors[3][1], colors[3][2], alpha); break;
			case COLOR5 : col = new Color(colors[4][0], colors[4][1], colors[4][2], alpha); break;
			case COLOR6 : col = new Color(colors[5][0], colors[5][1], colors[5][2], alpha); break;
			default: 
				throw new RuntimeException("Invalid color code " + clr.toString());
		}
		
		return col;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(8 * circleSize + 7 * space + 1, 6 * circleSize + 5 * space + 1);
	}
	
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
		return 30;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
		return 10;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		
		int x = e.getX(),
			y = e.getY(),
			overStateID = -1;
		
		for ( int i = 0; i < 8; i++)
			for ( int j = 0; j < 6; j++ )
				if ( x >= i * (circleSize + space) && x <= i * (circleSize + space) + circleSize &&
				     y >= j * (circleSize + space) && y <= j * (circleSize + space) + circleSize )
					overStateID = j * 8 + i;
		
		if ( overStateID != activeStateID ) {
			activeStateID = overStateID;
			repaint();
		}
	
	}
}

