/********************************************************
* Project : TextFormatter
* 
* Filename: Para.java
* Package : textformatter
* 
* Author: dr.Dobermann (c) 2017
********************************************************/
package textformatter;

import java.util.*;
import java.util.regex.*;
import lombok.*;

/**
 * Provides single paragraph functionality
 * Every single line might have position-linked decorations (such as bold,
 * italic and so on).
 *  
 * @author dr.Dobermann
 *
 */
class Para implements
	TFExcDataLoad {
	
	enum PAlign { 
					PA_AS_IS,
					PA_LEFT,
					PA_RIGHT,
					PA_CENTER,
					PA_FILL
				};
	
	protected @Getter PAlign align;
	
	private @Getter int width;
	protected @Getter Page page;
	
	// if paragraph is closed any addition to it
	// will fire TFException
	protected @Getter boolean closed = false;
	
	private @Getter int indent;
	private @Getter int[] spaces;
	private @Getter int[] margins;
	private @Getter int interval; // interval between formatted lines
	                              // 1 means no empty lines added between
	                              // lines 
	
	// the flag shows if the formatted lines are comply with the buffer
	protected @Getter boolean isInvalid = true;
	// buffer of unformatted ParaLines 
	protected List<ParaLine> buff = new ArrayList<ParaLine>();
	// list of Frames
	protected List<Frame> frame = new ArrayList<Frame>();
	
	protected List<Footnote> footnotes = new ArrayList<Footnote>();
	
	public Para( Page page, 
			     int width,
			     PAlign align,
			     int interval,
			     int indent,
			     int[] spaces,
			     int[] margins) {
		
		this.page = page;
		this.width = width;
		this.align = align;
		this.interval = interval;
		this.indent = indent;
		this.spaces = spaces;
		this.margins = margins;
	}

	/**
	 * Closing the paragraph
	 */
	public void Close() 
		throws TFException {
		
		if ( isInvalid )
			Format();
		
		closed = true;
	}
	
	/**
	 * Adds string to a unformatted buffer and adds decorations to it
	 * 
	 * @param str 		-- string to add
	 * @param decors	-- DecoPair placed on the string
	 */
	public void AddString( DecoratedStr str ) 
		throws TFException {
		
		if ( closed )
			throw new 
				TFException(getID(), "[AddString] Paragraph already closed!!!");
		
		isInvalid = true;
		
		ParaLine pl = new ParaLine(this, str.str.length() + 2);
		pl.AddString(str);
		buff.add(pl);
		
		// add double spaces into the end of the every sentence
		if ( str.str.length() > 0 && align != Para.PAlign.PA_AS_IS )
			pl.AddString("  ", new Decor[0]);
	}
	
	/**
	 * Adds a new Paraline to the paragraph buffer
	 * 
	 * @param pl ParaLine to add
	 * 
	 * @throws TFException
	 */
	protected void AddString(ParaLine pl) 
		throws TFException {
		
		if ( closed )
			throw new 
				TFException(getID(), "[AddString] Paragraph already closed!!!");
		
		isInvalid = true;

		buff.add(pl);
	}
	
	/**
	 * Adds a footnote to the current string in the buffer
	 * 
	 * @param footnote 	   -- Footnote string array with decorations 
	 * @param fNoteID	   -- footnote ID
	 * 
	 * @throws TFException
	 */
	public void AddFootnote( DecoratedStr[] footnote, int fNoteID ) 
		throws TFException {

		if ( closed )
			throw new 
				TFException(getID(), "[AddFootnote] Paragraph already closed!!!");
		
		isInvalid = true;
		
		if ( footnote.length > 0 ) {
			
			Footnote fnote = new Footnote(page, this, new int[] {-1, buff.size() - 1}, fNoteID, page != null ? page.getWidth() : width);
			for ( DecoratedStr fs : footnote )
				fnote.AddString(fs);
			
			footnotes.add(fnote);
		}

	}

	/**
	 * Adds new footnote to the paragraph
	 * 
	 * @param fn    -- footnote to add
	 * @param line  -- line linked to the new footnote
	 * 
	 * @throws TFException
	 */
	protected void AddFootnote( Footnote fn, int line ) 
		throws TFException {

		if ( closed )
			throw new 
				TFException(getID(), "[AddFootnote] Paragraph already closed!!!");

		fn.SetPara(this);
		fn.SetLine(line, true);
		
		footnotes.add(fn);
	}
	
	/**
	 * Formats the paragraph according the settings. 
	 * If lines limit exceeded while paragraph formatting, the rest of the
	 * buffer lines will send to the new created paragraph and it returns as a 
	 * formatting result
	 * 
	 * @param linesLimit sets a limit of formatted lines for the paragraph
	 * 
	 * @throws TFException
	 */
	protected Para Format( int linesLimit ) 
		throws TFException {
		
		
		if ( !isInvalid )
			return null;
		
		lines.clear();

		if ( footnotes.size() > 0 )
			for ( Footnote fn : footnotes )
				fn.Format( -1 );
		
		ParaLine line, newLine;
		int maxLen, lineNo;
		
		for ( ParaLine pl : buff ) {
			
			lineNo = buff.indexOf(pl);

			if ( linesLimit > 0 && lines.size() == linesLimit ) {
				return MoveBufferToPara( lineNo, null );
			}
			
			if ( pl.GetLength() == 0 ) {
				lines.add(pl);
				AddInterval();
				continue;
			}
			
			line = pl.Copy();
			
			while ( line.GetLength() > 0 ) {

				// add new line if lines list is empty or 
				//              if last line in lines is full or
				//              if the align is as_is
				if ( lines.size() == 0 ||
					 lines.get(lines.size() - 1).GetLength() == width ||
					 align == Para.PAlign.PA_AS_IS
					)
					lines.add(new ParaLine(this, width));

				newLine = lines.get(lines.size() - 1);

				maxLen = width - newLine.GetLength() -  
						 margins[1] - margins[0] -
						 (lines.size() == 1 ? indent : 0);
				
				if ( maxLen >= line.GetLength() ) {
					newLine.JoinLine(line);
					if ( Para.PAlign.PA_AS_IS != align && lineNo == buff.size() - 1 )
						AlignLine( newLine, 
								   line.GetLength() == 0 && lineNo == buff.size() - 1 );
					continue;
				}
				else { // add part of line from 0 up to last space in it
					int len = line.getBuff().substring(0, maxLen).lastIndexOf(' ');
					newLine.JoinLine(line.CutHead( len < 0 ? 0 : len ));

					LinkFootnotes(lineNo);
					
					if ( Para.PAlign.PA_AS_IS != align )
						AlignLine(newLine,
								 line.GetLength() == 0 && lineNo == buff.size() - 1); // is it the last line of paragraph?
					
					// if it's not fully processed last buffer line, then add new line into formatted lines list
					if ( !(line.GetLength() == 0  && lineNo == buff.size() - 1) )
						lines.add(new ParaLine(this, width));
				}
			}
		}
				
		isInvalid = false;
		
		return null;
	}

	
	/**
	 * Adds extra empty lines in the end of the paragraph
	 * and closes it
	 * 
	 * @param lNum
	 * @param withInterval
	 */
	public void FeedLines( int lNum, boolean withInterval ) 
		throws TFException {
		
		if ( closed )
			throw new 
				TFException( getID(), 
						     "[AddString] Paragraph already closed!!!" );
		
		Format();
		
		for ( int l = 0; l < lNum; l++ ) {
			lines.add( new ParaLine( this, width ) );
			if ( withInterval && interval > 1 )
				for ( int i = 0; i < interval; i++ )
					lines.add(new ParaLine(this, width));
		}

		closed = true;
		
	}
	
	/**
	 * Looking for footnotes, linked to the line
	 * 
	 * @param line -- line number in buffered or formatted paragraph lines
	 * 
	 * @param inFormatted -- if true, the search starts in formatted lines,
	 * 						 if false only buffer is checked
	 * 
	 * @return an array of footnotes, linked to the line
	 */
	Footnote[] GetFNotesOnLine( int line, boolean inFormatted )
	{
		if ( footnotes.size() == 0 )
			return new Footnote[0];
		
		List<Footnote> fnl = new ArrayList<Footnote>();
		
		for ( Footnote f : footnotes )
			if ( (inFormatted && f.getLineNo()[0] == line) ||
				 (!inFormatted && f.getLineNo()[1] == line) )
				fnl.add(f);
		
		return fnl.toArray(new Footnote[0]);
	}
	
	public int GetBuffLinesCount() {
		
		return buff.size();
	}
	/**
	 * Returns one buffer line
	 * 
	 * @param idx -- line index
	 * 
	 * @return buffer line on given index
	 * 
	 * @throws TFException
	 */
	public ParaLine GetBuffLine( int idx ) 
		throws TFException {
		
		if ( idx < 0 || idx >= buff.size() )
			throw new
				TFException(getID(), "[GetBuffLine] Buffer line index [%d] is out of bounds!!! ", idx);
		
		return buff.get(idx);
	}
	
	/**
	 * Returns an array of formatted lines
	 * 
	 * @param spaced -- Determines if empty string should be added as 
	 *                  it set up in spaces and interval settings. 
	 *                  Empty lines will be added when it's true.
	 *                   
	 * @return an array of ParaLines. Spaces and intervals added as empty ParaLines
	 * 
	 * @throws TFException
	 */
	public ParaLine[] GetLines( boolean spaced, boolean withFNotes )
		throws TFException 	{
		
		if ( isInvalid )
			Format();
			
		List<ParaLine> ll = new ArrayList<ParaLine>();
		
		if ( spaced )
			for ( int s = 0; s < spaces[0]; s++ )
				ll.add(new ParaLine(this, 1));
		
		for ( ParaLine line : lines ) {
			ll.add(line);
			if ( spaced && 
				 lines.indexOf(line) != lines.size() - 1 ) // do not add interval after last line
				for ( int i = 0; i < interval - 1; i++)
					ll.add(new ParaLine(this, 1));
		}
		
		if ( spaced )
			for ( int s = 0; s < spaces[1]; s++ )
				ll.add(new ParaLine(this, 1));
		
		if ( withFNotes && footnotes.size() > 0 ) {
			ll.add(new ParaLine(this, width));
			ll.get(ll.size() - 1).Pad(Para.PAlign.PA_LEFT, '-', width);
			
			for ( Footnote f : footnotes )
				ll.addAll(Arrays.asList(f.GetLines(spaced, false)));
		}
		
		return ll.toArray(new ParaLine[0]);
	}
	
	/**
	 * Returns number of lines in formatted or buffered zones.
	 * 
	 * Does not check if Para is formatted or not
	 * 
	 * @param fromFormatted -- if it's true, count of formatted lines returns, 
	 *                         if it's false, count of buffered lines returns.
	 * @param withFootnotes -- if it's true the count of footnotes lines also takes
	 *                         into account
	 *                         
	 * @return number of paragraph lines
	 */
	public int GetLinesCount( boolean fromFormatted, boolean withFootnotes ) {
		
		int fnLines = 0;
		if ( !isInvalid && footnotes.size() > 0 )
			for ( Footnote fNote : footnotes )
				fnLines += fNote.GetLinesCount(fromFormatted, false);

		if ( !fromFormatted )
			return buff.size() + fnLines;
		
		
		return lines.size() + fnLines;
	}

	/**
	 * Returns the number of footnotes linked to the paragraph
	 * 
	 * @return number of linked footnotes
	 */
	public int GetFootnotesCount() {
		
		return footnotes.size(); 
	}

	/**
	 * Return the ParaLines of footnote 
	 * 
	 * @param idx -- footnote index to get
	 * 
	 * @return an array of ParaLines
	 * 
	 * @throws TFException
	 */
	public ParaLine[] GetFootnote( int idx ) 
		throws TFException {
		
		if ( idx < 0 || idx > footnotes.size() - 1 )
			throw new
				TFException( getID(), 
						     "[GetFootnote] Invalid footnote index %d!!!", 
						     idx );
		
		if ( isInvalid )
			Format();
		
		return footnotes.get(idx).GetLines(false, false);
	}
	
	@Override
	public String toString() {
		
		ParaLine[] pll;
		try {
			pll = GetLines(true, true);
		} 
		catch ( TFException e ) {
			throw new
				RuntimeException(e.getMessage());
		}
		
		StringBuilder sb = new StringBuilder();
		
		for ( ParaLine line : pll )
			if ( line.GetLength() == 0 )
				sb.append('\n');
			else {
				sb.append(line.GetStr());
				sb.append('\n');
			}
		
		return sb.toString();
			
	}
		
	/*
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		return "PARA: ";
	}
	
	
	
}
//-----------------------------------------------------------------------------

/**
 * Implements functionality of a footnote
 * 
 * @author dr.Dobermann
 *
 */
class Footnote extends Para 
               implements TFExcDataLoad {
	
	private final static String fNoteFmt = "%3d) ";
	private final static int fNoteFmtLen = String.format(fNoteFmt, 1).length(); // max 100 footnotes on the page
	
	public final static String fNoteMark = "%d";
	
	private @Getter int noteID;
	private @Getter Para para;		// related paragraph
	private @Getter int[] lineNo;	// line number in paragraph where link is
									//   0 -- the position in formatted paragraph
									//   1 -- the position in buffer
	
	/**
	 * @param page
	 * @param width
	 */
	public Footnote( Page page, Para para, int[] lineNo, int id, int width ) 
		throws TFException {
		
		super(page, 
			  width, 
			  Para.PAlign.PA_LEFT, 
			  1,
			  -fNoteFmtLen,
			  new int[] {0, 0}, 
			  new int[] {fNoteFmtLen + 2, 0});
		
		noteID = id;		
		this.para = para;
		this.lineNo = lineNo;
		
		ParaLine pl = new ParaLine(this, super.getWidth()); 
		
		pl.AddString(String.format(fNoteFmt, noteID), new Decor[0]);
		buff.add(pl);
	}
	
	/**
	 * Sets a new line number for the footnote mark.
	 * if inBuffer is true, then line number is linked to buffer line,
	 * if not, then to the formatted lines
	 * 
	 * @param newLine   -- new line number
	 * @param inBuffer  -- if true, new line sets for buffer, if
	 * 					   false, then to the formatted lines
	 */
	public void SetLine( int newLine, boolean inBuffer ) {
	
		if ( inBuffer )
			lineNo[1] = newLine;
		else
			lineNo[0] = newLine;
	}
	
	public void SetPara( Para newPara ) {
		para = newPara;
	}
	
	@Override
	public void AddFootnote( DecoratedStr[] footnote, int fNoteID ) 
			throws TFException {} // footnote couldn't have footnote

	public void FeedLines( int lNum, boolean withInterval ) {}
	
	@Override
	public String getID() {
		return String.format("FOOTNOTE[%d]: ", noteID);
	}
	
}
//-----------------------------------------------------------------------------

/**
 * Implements a header of a page
 * 
 * @author dr.Dobermann
 *
 */
class Header extends Para
			 implements TFExcDataLoad {

	public Header(Page page) 
		throws TFException {

		super(page, 
			  page.getWidth(), 
			  page.getHeaderAlign(), 
			  1,
			  0,
			  new int[] {0, 0}, 
			  new int[] {0, 0});

		ResetHeader();
	}
	
	// Functionality
	//-------------------------------------------------------------------------
	/**
	 * Renews header representation
	 * 
	 * @throws TFException
	 */
	public void ResetHeader() 
		throws TFException {

		buff.clear();
		lines.clear();
		super.isInvalid = true;
		
		if ( page.getHeaderHeight() == 0 )
			return;
		
		super.align = page.getHeaderAlign();
		
		final String sNum = String.format( "- %d -", page.getPageNum() );

		ParaLine pl;
		
		buff.clear();
		for ( int l = 0; l < page.getHeaderHeight(); l++ ) {
			if ( l == page.getHeaderLine() ) { 
				pl = new ParaLine( this, getWidth() );
				pl.AddString( sNum, new Decor[0] );
				super.AlignLine( pl, false );
			}
			else
				// add a double dashed string as the last line 
				// of the header if space for it
				if ( l == page.getHeaderHeight() - 1 && 
				     page.getHeaderHeight() > 1 && 
				     page.getHeaderLine() != page.getHeaderHeight() - 1 ) {
					
					pl = new ParaLine( this, getWidth() );
					pl.Pad( Para.PAlign.PA_LEFT, '=', getWidth() );
				}
				else
					pl = new ParaLine( this, getWidth() );
	
			buff.add( pl );					
		}
		
		Format();
	}
	
	public void AddString(DecoratedStr str) {}
	
	public void AddFootnote(DecoratedStr[] fnote, int fNoteID) {}

	public void FeedLines( int lNum, boolean withInterval ) {}
	
	@Override
	public String getID() {
		return String.format("HEADER4PAGE[%d]: ", super.page.getPageNum());
	}
}
//-----------------------------------------------------------------------------

class FormattedLine {
	
	ParaLine s;  // ParaLine consisted the line symbols
	int line,    // line number in the paragraph buffer
	    pos;     // position of buffer line started the ParaLine
	
	FormattedLine( ParaLine s, int line, int pos ) {
		this.s = s;
		this.line = line;
		this.pos = pos;
	}
}


/**
 * Represents a Frame functionality
 * Frame is the view of the formatted paragraph lines on the page
 * Frames are linked to the page and to the paragraph. 
 * If the page link is null it's an orphaned frame and it should be
 * adopted by the next page
 * 
 * @author dr.Dobermann
 */
 
class Frame 
	implements TFExcDataLoad {
		
	private @Getter Para para;
	private @Getter Page page;
	
	private @Getter int sLine,
	                    sPos,
	                    eLine,
	                    ePos;
	
	private @Getter boolean isInvalid = true;
	
	private List<FormattedLine> lines = new ArrayList<FormattedLine>();
	
	private @Getter int width,
	                    indent,
	                    interval,
	                    margins[],
	                    spaces[],
	                    feedLines;
	private @Getter Para.PAlign align;
	
	public Frame( Para para, Page page,
			      int sLine, int sPos, 
			      int eLine, int ePos ) {
		
		this.para = para;
		this.page = page;
		this.sLine = sLine;
		this.sPos = sPos;
		this.eLine = eLine;
		this.ePos = ePos;
		
		width = para.getWidth();
		indent = para.getIndent();
		interval = para.getInterval();
		margins = para.getMargins();
		spaces = para.getSpaces();
		align = para.getAlign();
		
		feedLines = 0;
	}
	
	protected void Format() 
			throws TFException {
			
			
			if ( !isInvalid )
				return;
			
			lines.clear();

			ParaLine line, newLine;
			int maxLen, lineNo, pos;
			
			for ( lineNo = sLine; lineNo <= eLine; lineNo++ ) {

				line = para.GetBuffLine( lineNo );
				pos = 0;
				
				if ( lineNo == sLine && sPos > 0 ) {
					line.CutHead( sPos + 1 );
					pos = sPos;
				}
				
				if ( lineNo == eLine && ePos < line.GetLength() - 1 )
					line.DropTail( ePos + 1 );

				if ( line.GetLength() == 0 ) {
					lines.add( new FormattedLine( line, lineNo, pos ) );
					continue;
				}
				
				while ( line.GetLength() > 0 ) {

					// add new line if lines list is empty or 
					//              if last line in lines is full or
					//              if the align is as_is
					if ( lines.size() == 0 ||
						 lines.get(lines.size() - 1).s.GetLength() == width ||
						 align == Para.PAlign.PA_AS_IS
						)
						lines.add( new FormattedLine( new ParaLine( para, width), lineNo, pos ) );

					newLine = lines.get(lines.size() - 1).s;

					maxLen = width - newLine.GetLength() -  
							 margins[1] - margins[0] -
							 (lines.size() == 1 ? indent : 0);
					
					if ( maxLen >= line.GetLength() ) {
						
						// add rest of the line to a formatted lines string
						// and get next line from paragraph buffer
						newLine.JoinLine( line );
						
						// if it's the last line of the buffer then align it
						if ( Para.PAlign.PA_AS_IS != align && 
							 lineNo == eLine && eLine == para.GetBuffLinesCount() - 1 )
							
							AlignLine( newLine, 
									   line.GetLength() == 0 && 
									   lineNo == eLine && eLine == para.GetBuffLinesCount() - 1 );
						continue;
					}
					else { 
						
						// add part of line from 0 up to last space in it
						int len = line.getBuff().substring( 0, maxLen ).lastIndexOf(' ');
						newLine.JoinLine( line.CutHead( len < 0 ? 0 : len ));
						pos += len;
						
						LinkFootnotes( lineNo );
						
						if ( Para.PAlign.PA_AS_IS != align )
							AlignLine( newLine,
									   line.GetLength() == 0 && 
									   lineNo == eLine && eLine == para.GetBuffLinesCount() - 1 ); // is it the last line of paragraph?
						
						// if there are still exist symbols in the buffer then add an empty line to the formatted lines list
						if ( !( line.GetLength() == 0  && 
								lineNo == eLine && eLine == para.GetBuffLinesCount() - 1 ) )
							lines.add( new FormattedLine( new ParaLine( para, width ), lineNo, pos ) );
					}
				}
			}
					
			isInvalid = false;			
	}	

	/**
	 * Links all footnotes which were transferred from buffer line to the 
	 * last formatted line 
	 * 
	 * @param buffLineNo -- number of buffer line processing
	 */
	private void LinkFootnotes( int buffLineNo ) {

		Footnote[] fl = para.GetFNotesOnLine(buffLineNo, false);
		ParaLine line = lines.get(lines.size() - 1).s;
		
		if ( fl.length > 0 ) {
			
			int pos = 0;
			
			Decor fNoteDecor = line.GetFirstDecorFrom(Decor.DeCmd.DCS_FNOTE, 0);
			
			while ( fNoteDecor != null ) {
				
				pos = fNoteDecor.getPos() + 1;
				
				for ( Footnote fn : fl )
					if ( fn.getNoteID() == Integer.parseInt(fNoteDecor.getData().toString()) )
						fn.SetLine(lines.size() - 1, false);
				
				fNoteDecor = line.GetFirstDecorFrom(Decor.DeCmd.DCS_FNOTE, pos);
			}
		}
		
	}	

	/**
	 * Aligns the line according to align settings and adds necessary margins
	 * 
	 * @param pl         -- line to align
	 * @param lastLine   -- if it's the last line, it shouldn't be aligned in fill mode
	 * 
	 * @throws TFException
	 */
	protected void AlignLine( ParaLine pl, boolean lastLine ) 
		throws TFException {
		
		if ( align == Para.PAlign.PA_AS_IS )
			throw new
				TFException( getID(), "It's forbidden to align lines with PA_AS_IS setting on!!!");
		
		// remove leading or trailing spaces for every line except for the first one
		pl.Trim(ParaLine.TrimSide.TS_BOTH);
	
		// adds left margins
		if ( margins[0] > 0 )
			pl.Pad( Para.PAlign.PA_LEFT, ' ', margins[0] + (lines.size() == 1 ? indent : 0) );
		
		int fillSpace = width - pl.GetLength() - margins[1];
				
		switch ( align ) {
		
			case PA_AS_IS :
				break;
				
			case PA_LEFT :
				pl.Pad(Para.PAlign.PA_RIGHT, ' ', fillSpace);
				break;
			
			case PA_RIGHT :
				pl.Pad(Para.PAlign.PA_LEFT, ' ', fillSpace);
				break;
			
			case PA_CENTER :
				pl.Pad(Para.PAlign.PA_CENTER, ' ', fillSpace);
				break;
			
			case PA_FILL :
				
				if ( !lastLine && fillSpace > 0 ) { // do not align the last or full line
				
					List<Integer[]> words = new ArrayList<Integer[]>(); 
					boolean firstWord = true;
					// look for words
					Matcher m = Pattern.compile("[\\Wp{Punct}\\p{Blank}]*[\\w'-]+[\\p{Punct}\\p{Blank}]*").matcher(pl.getBuff());
				
					while ( m.find() )
					{
						if ( firstWord ) // we don't add spaces before the first word
							firstWord = false;
						else
							words.add(new Integer[] {m.start(), 0});
					}
					
					int maxSpaces = 1;
					int pos, idx;
					int tries = 0;
					while ( fillSpace > 0 ) {
						
						idx = (int)(Math.random() * (words.size() - 1));
						pos = words.get(idx)[0];
						
						if ( words.get(idx)[1] < maxSpaces ) {
							words.get(idx)[0] = pos + 1;
							words.get(idx)[1] = maxSpaces;
							// insert space before the word
							pl.InsertChar( pos, ' ');
							fillSpace--;
							tries = 0;
							
							// shift all words after it for one space
							for ( int i = idx + 1; i < words.size(); i++ )
								words.get(i)[0] = words.get(i)[0] + 1;
						}
						else 
							if ( tries++ > 2 ) {// if it not succeeded to find appropriate position three times 
								maxSpaces++;    // increase number of allowed spaces before the word.
								tries = 0;
							}
					}
				}
				break;
		}
		
		// adds right margins
		if ( margins[1] > 0 )
			pl.Pad(Para.PAlign.PA_RIGHT, ' ', margins[1]);
	
	}

	/* (non-Javadoc)
	 * @see textformatter.TFExcDataLoad#getID()
	 */
	@Override
	public String getID() {
		
		return "FRAME: ";
	}
	
}
//-----------------------------------------------------------------------------
