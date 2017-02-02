package life;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class LifeCells {

	
	public static void main(String[] args) {
		
		Flatland desert = new Flatland();
		
		int[][] pattern1 = {
				            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
				            {1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1,},
				            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,},
				           };
		
		int[][] pattern2 = {
		           			{1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0,},
		           			{1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0,},
		           			{1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1,},
		           			{1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0,},
		           			{1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0,},
		          		   };

		int[][] pattern3 = {
		           			{0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1,},
		           			{0, 0, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0,},
		           			{0, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 0,},
		           			{0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0,},
		           			{1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0,},
		                   };
		
		int[][] pattern4 = {
       						{1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1,},
       						{0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1,},
       						{0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0,},
       						{0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,},
       						{0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0,},
      		               };
		
		desert.AddNewColony(new Point(10,30), Colony.ConvertArr2PointsList(pattern1, 3, 17));
		
		desert.AddNewColony(new Point(20, 40), Colony.ConvertArr2PointsList(pattern2, 4, 11));

		desert.AddNewColony(new Point(40, 60), Colony.ConvertArr2PointsList(pattern2, 5, 11));

		desert.AddNewColony(new Point(70, 10), Colony.ConvertArr2PointsList(pattern3, 5, 15));
		
		desert.AddNewColony(new Point(50, 80), Colony.ConvertArr2PointsList(pattern4, 5, 11));

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				CreateAndShowGUI(desert);
			}
		});
		
		
/*		// console test of Life Cells
  		for ( int i = 0; i < 11; i++ ) {
			for ( Colony col : desert.GetColoniesList() ) {
				System.out.println("Colony #" + col.GetID() + ". Generation #" + col.GetCurrGeneration());
				System.out.println(Arrays.deepToString(Colony.ConvertColony2Array(col)));
			}
			desert.NextGeneration();
			
		}
*/	
	}
	
	private static void CreateAndShowGUI(Flatland fland) {
		LifeWindow wnd = new LifeWindow(fland);
		wnd.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		wnd.setVisible(true);
	}

}
