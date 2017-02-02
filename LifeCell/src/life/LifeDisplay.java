package life;

import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LifeDisplay extends JLabel 
						 implements Scrollable {
	
	private Flatland flatland;
	private int fWidth, fHeight;
	
	public LifeDisplay(Flatland fland) {
		flatland = fland;
		
		this.setSize(fWidth + 1, fHeight + 1);
	}
	
	@Override
	public Dimension getPreferredSize() {
		ResetSize();
		return new Dimension(fWidth + 1, fHeight + 1);
	}
	
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect,
			                              int orientation,
			                              int direction)
	{
		return 11;
	}
	
	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect,
										   int orientaion,
										   int direction)
	{
		return 55;
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
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		for ( Colony col : flatland.GetColoniesList() ) {
			for ( Cell cell : col.GetCells() ) {
				switch ( cell.GetAge() ) {
					case 1 :  g.setColor(Color.GREEN); 		break;
					case 2 :  g.setColor(Color.YELLOW);		break;
					case 3 :  g.setColor(Color.ORANGE);		break;
					case 4 :  g.setColor(Color.RED);		break;
					default:  g.setColor(Color.DARK_GRAY);  break;
				}
				g.fillRect(col.GetOrigin().x * 11 + cell.GetPosition().x * 11 + 1, 
						   col.GetOrigin().y * 11 + cell.GetPosition().y * 11 + 1, 10, 10);
				
				g.setColor(Color.DARK_GRAY);
				g.drawRect(col.GetOrigin().x * 11, 
						   col.GetOrigin().y * 11, 
						   col.width * 11 + 2, col.height * 11 + 2);
			}
		}
		
	}
	
	public void ResetSize() {
		fWidth = (flatland.GetBounds()[0]) * 11 + 2;
		fHeight = (flatland.GetBounds()[1]) * 11 + 2;
		this.setSize(new Dimension(fWidth + 1, fHeight + 1));
	}


	
}


