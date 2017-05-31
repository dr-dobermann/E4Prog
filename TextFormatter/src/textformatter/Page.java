/********************************************************
* Project : TextFormatter
* 
* Filename: Page.java
* Package : textformatter
* 
* Author: dr.Dobermann (c) 2017
********************************************************/
package textformatter;

import lombok.*;

class Page {
	
}

/*
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

*//**
 * Implements functionality of one page of the document
 * Consists one or zero header, few paragraphs and zero or few footnotes
 * 
 * @author dr.Dobermann
 *//*
class Page 
	implements TFExcDataLoad {
	
	private @Getter TextFormatter textFormatter;
	private @Getter int width = 72;
	private @Getter int height = 40;
	private @Getter int currWidth = width;
	private @Getter Para.PAlign align = Para.PAlign.PA_LEFT;
	
	private @Getter int pageNum = 1;
	
	private @Getter int[] spaces = new int[] {0, 0};
	private @Getter int interval = 1;
	private @Getter int[] margins = new int[] {0, 0};
	private @Getter int indent = 3;
	
	private List<Para> paragraphs = new ArrayList<Para>();
	
	private @Getter int headerHeight = 3;
	private @Getter Para.PAlign headerAlign = Para.PAlign.PA_CENTER;
	private @Getter int headerLine = 1;
	private @Getter Header header;
	
	private @Getter boolean isClosed = false;

	private @Getter int linesLeft = height - headerHeight;
	
	private List<Footnote> footnotes = new ArrayList<Footnote>();
	private List<Frame> frames = new ArrayList<>();
	
	
	public Page(TextFormatter tf) 
		throws TFException {

		if ( tf != null ) {
		
			pageNum = tf.GetLastPageNum();
			
			textFormatter = tf;

			width = tf.getWidth();
			height = tf.getHeight();
			
			indent = tf.getIndent();
			interval = tf.getInterval();
			spaces[0] = tf.getSpaces()[0];
			spaces[1] = tf.getSpaces()[1];
			margins[0] = tf.getMargins()[0];
			margins[1] = tf.getMargins()[1];
		}
		
		header = new Header( this );

		if ( headerHeight > 0 )
			header.ResetHeader();
				
		AddNewPara();
	}
	
	
	// Functionality
	//-------------------------------------------------------------------------
	
	*//**
	 * Adds a new paragraph on the page. Before the addition it closes and 
	 * formats the previous one.
	 * 
	 * @return True if a new paragraph were added or 
	 *         False if there is no free lines left on the page
	 *//*
	public void AddNewPara() 
		throws TFException {
		
		if ( isClosed )
			throw new
				TFException( getID(), "[AddNewPara] Page already closed!" );
		
		if ( paragraphs.size() > 0 ) { 
			
			GetLastPara().Close();
			
			if ( GetLastPara().GetLinesCount( this, true, true, true ) > linesLeft )
				linesLeft -= GetLastPara().FitToLines( linesLeft, footnotes.size() == 0, frames.size() == 0 );
			
			if ( linesLeft <= 0 ) {
				textFormatter.AddPage();
				return;
			}
		}
		
		paragraphs.add(new Para(this, currWidth, align, interval, indent, spaces, margins));
	}
	
	*//**
	 * Returns last paragraph on the page
	 * @return last paragraph. If there is no any, exception fires
	 * @throws TFException
	 *//*
	private Para GetLastPara()
		throws TFException {
		
		if ( paragraphs.size() == 0 )
			throw new
				TFException(getID(), "GetLastPara: There is no available paragraphs yet!");
		
		return paragraphs.get(paragraphs.size() - 1);
	}
	
	*//**
	 * Adds a new string into the last paragraph
	 * 
	 * @param str  -- Decorated string to add
	 * 
	 * @throws TFException
	 *//*
	public void AddString(DecoratedStr str) 
		throws TFException {
		
		if ( isClosed )
			throw new
				TFException(getID(), "[AddNewPara] Page already closed!");
		
		GetLastPara().AddString(str);
	}

	*//**
	 * Close page and sent the rest of it to the next one
	 * 
	 * @param next -- Next page to consume the rest of the current page
	 *//*
	public void Close( Page next )
		throws TFException {

		if ( isClosed )
			return;
		
		GetLastPara().Close();
		
		
		if ( linesLeft >= 0 ) {
			isClosed = true;
			return;
		}
		
		// while linesLeft is still below 0
		// 1. Process all paragraphs with footnotes. 
		//    Footnotes will be reformatted for the new page width, but 
		//    new lines count will not take into account while moving.
		//    Footnote's lines moves to the next page until the first one. 
		//    If there are still no enough lines on the page, 
		//    then footnote moves as a whole and takes the lines of paragraph 
		//    it refers to with it. 
		//    If there is already footnote moved from the previous page, 
		//    it will not move anymore
		//
		// 2. Process all paragraphs while page height is reached.
		//    If the part of paragraph is not enough for the compensation,
		//    full paragraph will move to the next page.
		//    Paragraph will be reformatted for the new page width after moving.
		
		while ( linesLeft < 0 ) {
			
			
		}

		isClosed = true;
		
	}
	
	*//**
	 * Sets new paragraph width. Closes the current paragraph and adds a new one
	 * 
	 * @param newWidth -- new paragraphs width for next new paragraphs
	 * 
	 * @throws TFException
	 *//*
	public void SetWidth( int newWidth )
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( newWidth > width )
			throw new
				TFException(getID(), 
						    "The new page width [%d] should not be greater than its initial widht [%d]!!!",
						    newWidth, width);
		
		currWidth = newWidth;
		
		AddNewPara();
	}
	
	*//**
	 * Sets or deletes the page header
	 * 
	 * @param height
	 * @param line
	 * @throws TFException
	 *//*
	public void SetHeader() 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException( getID(), 
					     "The page is closed already!!!" );

		if ( headerHeight != textFormatter.getHeaderHeight() ) {

			linesLeft += textFormatter.getHeaderHeight() - headerHeight;
							
			headerHeight = textFormatter.getHeaderHeight();
			headerLine = textFormatter.getHeaderLine();
			headerAlign = textFormatter.getHeaderAlign();

			header.ResetHeader();
		}
		
	}
	
	*//**
	 * Feeds lines in the last paragraph
	 * 
	 * @param lines	       -- lines to feed
	 * @param withInterval -- add interval after lines or not
	 * 
	 * @throws TFException
	 *//*
	public void FeedLines( int lines, boolean withInterval ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		GetLastPara().FeedLines(lines, withInterval);
		
		AddNewPara();
	}
	
	*//**
	 * Sets new align setting for the next paragraphs
	 * 
	 * @param newAlign -- new align settings
	 * 
	 * @throws TFException
	 *//*
	public void SetAlign( Para.PAlign newAlign ) 
		throws TFException {

		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");
		
		align = newAlign;
		
		AddNewPara();		
	}
	
	*//**
	 * Sets new paragraph settings
	 * 
	 * @param indent   -- new first line indent settings
	 * @param spaces   -- spaces before[0] and after[1] paragraph
	 * 
	 *//*
	public void SetParaSettings( int indent, int[] spaces ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( spaces[0] < 0 || spaces[1] < 0)
			throw new
			TFException( getID(),
					     "[SetParaSettings] Invalid spaces [%d] [%d]",
					     spaces[0], 
					     spaces[1] );
		
		
		if ( indent < 0 && margins[0] < -indent )
			throw new
				TFException( getID(),
						     "[SetParaSettings] Invalid negative indent [%d]." +
						     "Should be not less than left margin [%d]",
						     indent, 
						     margins[0] );
			 
		if ( indent > 0 && indent > currWidth )
			throw new
				TFException( getID(),
						     "[SetParaSettings] Indent [%d] is wider than current page width [%d].",
						     indent, 
						     currWidth );
		
		this.spaces[0] = spaces[0];
		this.spaces[1] = spaces[1];
		
		this.indent = indent;
		
		AddNewPara();	 
	}

	*//**
	 * Sets margins settings for new paragraphs
	 * 
	 * @param  margins   -- new margins settings [0] - left, [1] - right
	 * 
	 * @throws TFException
	 *//*
	public void SetMargins( int[] margins ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( margins[0] < 0 || margins[1] < 0 || margins[0] + margins[1] > width )
			throw new
			TFException( getID(),
					     "[SetMargins] Invalid margins [%d] [%d]",
					     margins[0], 
					     margins[1] );
		
		this.margins[0] = margins[0];
		this.margins[1] = margins[1];
		
		AddNewPara();	 	
	}
	
	*//**
	 * Sets new interval value for next paragraphs
	 * 
	 * @param newInt
	 * @throws TFException
	 *//*
	public void SetInterval( int newInt ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( newInt  < 0 || newInt > height )
			throw new
			TFException( getID(),
					     "[SetInterval] Invalid interval value [%d]!!!",
					     newInt );
		
		interval = newInt;
				
		AddNewPara();	 	
	}

	*//**
	 * Sets new page number
	 * 
	 * @param pNum -- new page number
	 * 
	 * @throws TFException
	 *//*
	public void SetPageNum( int pNum ) 
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		if ( pNum < 0 )
			throw new
			TFException( getID(),
					     "[SetInterval] Invalid page number [%d]!!!",
					     pNum );
		
		pageNum = pNum;
		
		header.ResetHeader();
	}
	
	*//**
	 * Adds a footnote to the last line of current paragraph
	 * 
	 * @param fnote -- an array of strings of footnote
	 * @param id    -- id of a new footnote
	 * @throws TFException
	 *//*
	public void AddFootnote( String[] fnote, int id )
		throws TFException {
		
		if ( isClosed )
			throw new
			TFException(getID(), 
					    "The page is closed already!!!");

		DecoratedStr[] dfn = new DecoratedStr[fnote.length];
		
		for ( int f = 0; f < fnote.length; f++ ) 
			dfn[f] = ParaLine.PrepareString(fnote[f]);
		
		GetLastPara().AddFootnote(dfn, id);
	}
		
	 
	 * @see textformatter.TFExcDataLoad#getID()
	 
	@Override
	public String getID() {
		
		return String.format("PAGE #%d3;", pageNum);
	}
	
}
//-----------------------------------------------------------------------------



*/