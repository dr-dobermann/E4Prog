/********************************************************
* Project : TextFormatter
* 
* Filename: ParaLine.java
* Package : textformatter
* 
* Author: dr.Dobermann (c) 2017
********************************************************/
package textformatter;


import java.util.*;
import java.util.regex.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import lombok.*;

/**
 * Consists of information about decoration command
 * Every decoration command linked to a line position.
 * It could start decoration or end decoration
 * 
 * Decoration could also has a data related to it. Usually 
 * starting decoration has it. The content of the data depends on 
 * type of decoration. 
 * For example, footnote decoration has a footnote index in data
 * as a string.
 * 
 * @author dr.Dobermann
 */

@Log
class Decor 
	implements Comparator<Decor> {
	
	// Decoration command. DCS means starting command, DCE means ending command 
	public enum DeCmd {
						DCS_BOLD,        DCE_BOLD, 
						DCS_ITALIC,      DCE_ITALIC,
						DCS_UNDERLINE,   DCE_UNDERLINE,
						DCS_UPIDX,       DCE_UPIDX,
						DCS_DNIDX,       DCE_DNIDX,
						DCS_FNOTE,       DCE_FNOTE,
						DCS_CMD,         DCE_CMD;
		
		public DeCmd GetOpposite( DeCmd dec ) {
			switch ( dec ) {
				case DCS_BOLD      : return DCE_BOLD;
				case DCS_ITALIC    : return DCE_ITALIC;
				case DCS_UNDERLINE : return DCE_UNDERLINE;
				case DCS_UPIDX     : return DCE_UPIDX;
				case DCS_DNIDX     : return DCE_DNIDX;
				case DCS_FNOTE     : return DCE_FNOTE;
				case DCS_CMD       : return DCE_CMD;
				
				case DCE_BOLD      : return DCS_BOLD;
				case DCE_ITALIC    : return DCS_ITALIC;
				case DCE_UNDERLINE : return DCS_UNDERLINE;
				case DCE_UPIDX     : return DCS_UPIDX;
				case DCE_DNIDX     : return DCS_DNIDX;
				case DCE_FNOTE     : return DCS_FNOTE;
				case DCE_CMD       : return DCS_CMD;
			}
			return dec;
		}
		
		public boolean IsEnding( DeCmd dec ) {
			
			return dec.name().matches("DCE.*");
		}
	};
	
	private @Getter ParaLine line;
	private @Getter DeCmd cmd;
	private @Getter int pos;
	private @Getter Object data;
	
	public Decor( ParaLine owner, DeCmd cmd, int pos, Object data )  {
		this.line = owner;
		this.cmd = cmd;
		this.data = data;
		
		if ( pos > line.GetLength() ) {
			log.severe( String.format( "[Decor] The given decoration position [%d] is out of bounds of the paraLine width [%d]!!!", 
								pos, line.GetLength() ) );
			return;
		}
		this.pos = pos;
	}
	
	public Decor( DeCmd cmd, int pos, Object data ) {
		this.line = null;
		this.cmd = cmd;
		this.data = data;
		this.pos = pos;
	}
	
	/**
	 * Shifts decoration position on given shift
	 * @param shift -- number of positions to shift. Might be negative
	 */
	public void ShiftPos(int shift) {
		
		int newPos = pos + shift;
		if ( newPos < 0 || ( line != null && newPos > line.GetLength()) ) {
			log.severe( "[ShiftPos] Could not shift beyond the line bounds!!!" );
			return;
		}
		
		pos = newPos;
	}

	/**
	 * Sets new decoration's owner and new position
	 * @param newPL   -- new decor's owner
	 * @param newPos  -- new position inside a new owner
	 * @throws TFException
	 */
	public void SetLine(ParaLine newPL, int newPos) {

		if ( newPL == line )
			return;
		
		line = newPL;
		
		if ( newPos < 0 || newPos > line.GetLength() ) {
			log.severe( String.format( "[SetLine] New position [%d] is out of bounds of the line!!!", newPos ) );
			return;
		}

		pos = newPos;
	}

	/**
	 * Returns a tail of the DecoPair array which element's pos is bigger than fromPos.
	 * Positions of the tail elements aligned by fromPos
	 * 
	 * @param dpl		-- initial Decorations list
	 * @param fromPos   -- position to align from.
	 *  
	 * @return new list
	 *  
	 * @throws TFException
	 */
	public static List<Decor> GetTail ( List<Decor> dpl, int fromPos ) {

		return dpl.stream()
				.filter( d -> d.pos > fromPos )
				.map( d -> new Decor( d.cmd, d.pos - fromPos, d.data ) )
				.collect( Collectors.toList() );
	}
	
	/**
	 * Gets first part of DecoPair array and return it
	 * @param dpl     -- initial Decorations list
	 * @param maxIdx  -- maximum index to slice
	 * @return new list
	 */
	public static List<Decor> GetHead ( List<Decor> dpl, int toPos ) {
		
		return dpl.stream()
			.filter( d -> d.pos <= toPos )
			.collect( Collectors.toList() );
	}	
	 
	@Override
	public int compare(Decor o1, Decor o2) {
		if ( o1.pos < o2.pos )
			return -1;
		else 
			if ( o1.pos > o2.pos )
				return 1;
		
		return 0;
	}
    
	
}
//-----------------------------------------------------------------------------

	
/**
 * Describes one line of a paragraph 
 * Consist the line itself and all related decorations
 * 
 * @author dr.Dobermann
 *
 */
@Log
class ParaLine {
	
	public enum TrimSide {
		TS_LEFT,
		TS_RIGHT,
		TS_BOTH
	};
	
	private @Getter StringBuilder buff;
	private @Getter int width;
	
	private @Getter List<Decor> decors = null;
	
	public ParaLine( int width ) {
		
		this.width = width; 
		
		buff = new StringBuilder( this.width );
	}
	
	/**
	 * Shift all decorations of the line started from 'after' position
	 * @param after -- position after which the shift started
	 * @param shift -- shift size
	 */
	public void ShiftDecors(int after, int shift) {
		
		if ( after < 0 || after > buff.length() ) {
			log.severe( String.format( "[ShiftDecors] Shift point [%d] is out of line bounds!!!", after ) );
			return;
		}
		for ( Decor dec : decors )
			if ( dec.getPos() >= after )
				dec.ShiftPos(shift);
	}
	
	/**
	 * Drops out tailing symbols into new ParaLine
	 * These symbols are removed from the original ParaLine
	 * 
	 * @param length -- length of the tail to drop off
	 * 
	 * @return New ParaLine consists the tail of the original ParaLine
	 */
	public ParaLine DropTail( int length ) {
		
		if ( length > buff.length() ) {
			log.severe( "[DropTail] The tail length is bigger than the actual line length!!!" );
			return null;
		}
		
		ParaLine pl = new ParaLine( width );
		
		// send the tail to a new ParaLine
		pl.buff.append(buff.substring(buff.length() - length, buff.length()));
		
		// sends the related decoration commands as well
		for ( Decor dec : decors )
			if ( dec.getPos() >= buff.length() - length )
				dec.SetLine(pl, dec.getPos() - length + 1);
		
		// delete the tail
		buff.delete(buff.length() - length, buff.length());
		
		// delete the related decoration commands as well
		for ( int d = 0; d < decors.size(); d++ )
			if ( decors.get(d).getLine() != this )
				decors.remove(d);
		
		return pl;
	}
	
	/**
	 * Cuts the begin of the ParaLine and returns it as a new ParaLine
	 * The rest of the initial one is kept in old ParaLine
	 * The relevant decorations are also going to the new ParaLine
	 *   
	 * @param length -- length of the head to cut
	 * 
	 * @return new ParaLine with length symbols
	 */
	public ParaLine CutHead( int length ) {
		
		ParaLine pl = new ParaLine( width );
		if ( length == 0 )
			return pl;
		
		if ( length > buff.length() ) {
			log.severe( "[CutHead] The head length is bigger than the actual line length!!!" );
			return null;
		}
		
		pl.buff.append(buff.substring(0, length));
		
		decors.stream()
			.filter( d -> d.getPos() < length )
			.forEach( d -> {
				pl.InsertDecor( d.getCmd(), d.getPos(), d.getData() );
				decors.remove( d );
			});
		
		ShiftDecors(0, -length);
		buff.delete(0, length);
		
		return pl;
	}

	/**
	 * Joins a new ParaLine.
	 * If its length is longer than the free rest of the existed one,
	 * only part of it will be added.
	 * The rest of new ParaLine would be returned
	 * 
	 * @param pl -- new ParaLine to add
	 * 
	 * @return the rest of new ParaLine. it might have a zero length
	 */
	public ParaLine JoinLine( ParaLine pl ) {  // TODO: Check if it could be rewritten in more efficient way
		                                       // by using massive operations instead of single ones
		
		if ( pl.GetLength() == 0 )
			return new ParaLine(0);
		
		int offset = buff.length(),
			len = pl.GetLength() > width - buff.length() ? width - buff.length() : pl.GetLength();
		
		for ( int p = 0; p < len; p++ ) {
			buff.append( pl.buff.charAt(p) );
			for ( Decor dec : pl.GetDecorAt(p) )
				InsertDecor(dec.getCmd(), dec.getPos() + offset, dec.getData());
		}
		
		if ( len < pl.GetLength() )
			pl.CutHead(len);
		else
			{
				pl.buff.delete(0, len);
				pl.decors.clear();
			}
		
		return pl;
	}
	
	/**
	 * Creates a copy of ParaLine
	 * 
	 * @return Copy of existed ParaLine
	 * 
	 * @throws TFException
	 */
	public ParaLine Copy() {
		
		ParaLine pl = new ParaLine(width);
		
		pl.buff.append(buff);
		
		for ( Decor dec : decors )
			pl.InsertDecor( dec.getCmd(), dec.getPos(), dec.getData() );
		
		return pl;		
	}
	
	/**
	 * Inserts one character ch into pos position
	 * @param pos   -- position inside or at the end of buffer
	 * @param ch	-- character to insert
	 * @throws TFException
	 */
	public void InsertChar(int pos, char ch) {
		
		if ( pos < 0 ||
			 pos > buff.length() ||
			 pos >= width ) {
			log.severe( String.format( "[InsertChar] Invalid position [%d] for char inserting!", pos ) );
			return;
		}
		if ( pos == buff.length() )
			buff.append(ch);
		else {
			buff.insert(pos, ch);
			ShiftDecors(pos, 1);
		}
	}
	
	/**
	 * Pads ParaLine with len number of char ch from align side
	 * @param align  -- side to add. Could be Para.PAlign.PA_LEFT, Para.PAlign.PA_RIGHT
	 *                  and Para.PAlign.PA_CENTER
	 * @param ch     -- char to pad
	 * @param len    -- number of chars
	 */
	public void Pad(Para.PAlign align, char ch, int len) {
		
		if ( align == Para.PAlign.PA_CENTER) {
			Pad(Para.PAlign.PA_LEFT, ch, len/2);
			Pad(Para.PAlign.PA_RIGHT, ch, len - len/2);
		}
		
		for ( int i = 0; i < len; i++ )
			switch ( align ) {
				case PA_LEFT :
					InsertChar(0, ch);
					break;
					
				case PA_RIGHT :
					InsertChar(buff.length(), ch);
					break;
					
				case PA_CENTER :
				case PA_AS_IS :
				case PA_FILL :
					break;
			}
	}	
	
	/**
	 * Trims extra spaces from begin, end or both sides of the ParaLine
	 * @param side  -- Select side for trimming @see textformatter.ParaLine.TrimSide  
	 */
	public void Trim( ParaLine.TrimSide side ) {
		
		if ( ParaLine.TrimSide.TS_BOTH == side ) {
			Trim(ParaLine.TrimSide.TS_LEFT);
			Trim(ParaLine.TrimSide.TS_RIGHT);
			
			return;
		}
		
		int pos;
		
		for ( int p = 0; p < buff.length(); p ++ ) {
			pos = ParaLine.TrimSide.TS_RIGHT == side ? buff.length() - p - 1 : p;
			
			if ( buff.charAt(pos) == ' ' ) {
				
				ShiftDecors(pos, -1);
				buff.deleteCharAt(pos);
			
				if ( ParaLine.TrimSide.TS_RIGHT == side )
					p--;
			}
			else
				break;
		}
		
		
	}
	
	/**
	 * Append new string str at the end of the ParaLine and adds Decorations for its begin
	 * @param str     -- string to append
	 * @param decors  -- array of decorations to insert in the begin of the appended string
	 * 
	 * @throws TFException
	 */
	public void AddString( String str, Decor[] decors) {
		
		if ( str.length() > width - buff.length() ) {
			log.severe( String.format( "[AddString] String [%s] is too long for this ParaLine. Only %d symbols left", 
					                   str, width - buff.length() ) );
			return;
		}
		
		int pos = buff.length();
		
		buff.append(str);
		
		for ( Decor d : decors ) 
			InsertDecor(d.getCmd(), d.getPos() + pos, d.getData());
	}
	
	/**
	 * Adds a new decorated string to the ParaLine
	 *  
	 * @param dStr -- Decorated string to add
	 * 
	 * @throws TFException
	 */
	public void AddString(DecoratedStr dStr) 
		throws TFException {
		
		if ( dStr.str.length() > width - buff.length() )
			throw new
				TFException( getID(), "[AddString] Decorated string [%s] is too long for this ParaLine. Only %d symbols left", 
									  dStr.str, width - buff.length());
		
		int pos = buff.length();
		
		buff.append(dStr.str);
		
		for ( Decor d : dStr.dpl )
			InsertDecor( d.getCmd(), d.getPos() + pos, d.getData());
	}	
	
	/**
	 * Adds new decoration on ParaLine if the position is out of 
	 * ParaLine length, it added to the end of the ParaLine
	 * 
	 * @param dec    -- Decoration command to add
	 * @param pos	 -- position of decoration command starts at
	 * @throws TFException
	 */
	public void InsertDecor( Decor.DeCmd dec, int pos, Object data)
		throws TFException	{
		
		if ( pos > buff.length() )
			pos = buff.length();
		
		decors.add(new Decor(this, dec, pos, data));
		
		decors.sort(decors.get(0));
	}
	
	
	/**
	 * Returns an array of decorations linked to the position
	 * 
	 * @param pos  -- position to check at
	 * 
	 * @return array of linked to position decorations. Might be empty 
	 */
	public Decor[] GetDecorAt( int pos ) {
		
		List<Decor> dl = new ArrayList<Decor>();
		
		for ( Decor dec : decors )
			if ( dec.getPos() == pos )
				dl.add(dec);
		
		return dl.toArray(new Decor[0]);
	}

	
	/**
	 * Returns string enclosed by two decoration commands cmd1 and cmd2.
	 * Search starts from position fromPos
	 * 
	 * @param cmd1    -- opening command 
	 * @param cmd2    -- closing command
	 * @param fromPos -- search starting point in ParaLine
	 *  
	 * @return String enclosed between cmd1 and cmd2 or empty string if 
	 *         search fails
	 * 
	 * @throws TFException
	 */
	public String GetStringBetweenDecors ( Decor.DeCmd cmd1,
			                               Decor.DeCmd cmd2,
			                               int fromPos)
		throws TFException {
		
		if ( fromPos < 0 || fromPos > buff.length() - 1 )
			throw new
				TFException( getID(), "GetStringBetweenDecors: fromPos is out of bounds!!!");
		
		int sPos = 0, 
			ePos = 0;
		Decor[] dl = decors.toArray(new Decor[0]);
		
		Arrays.sort(dl, dl[0]);
		
		for ( Decor d : dl )
			if ( d.getCmd() == cmd1 && d.getPos() >= fromPos )
				sPos = d.getPos();
			else 
				if ( d.getCmd() == cmd2 && d.getPos() >= fromPos )
					ePos = d.getPos();
		
		return buff.substring(sPos, ePos);
	}
	
	/**
	 * Returns first decoration object in the ParaLine substring
	 * @param dc    -- Decoration command to look for
	 * @param from  -- substring starting point
	 * 
	 * @return First decoration object found in ParaLine substring
	 */
	public Decor GetFirstDecorFrom(Decor.DeCmd dc, int from) {
		
		for ( Decor dec : decors )
			if ( dec.getCmd() == dc && dec.getPos() >= from )
				return dec;
		
		return null; 
	}
	
	/**
	 * Returns length of the buffer
	 * @return buffer length
	 */
	public int GetLength() {
		
		return buff.length();
	}
	
	
	public String GetStr() {
		
		return buff.toString();
		
	}
	
	
	/**
	 * Processes the input string with decoration command embedded 
	 * into it and creates new DecoratedStr
	 * 
	 * @param str -- string to process
	 * 
	 * @return new DecoratedStr
	 * 
	 * @throws TFException
	 */
	public static DecoratedStr PrepareString(String str)
		throws TFException {
		
		StringBuilder sb = new StringBuilder(str.length());
		int lastPos = 0,
			compensator = 0;
		
		List<Decor> decors = new ArrayList<Decor>();
		Decor.DeCmd dCmd;
		
		Matcher m = Pattern.compile("\\&(\\w?)([\\+|-]?)(\\{(\\w*)\\})?+").matcher(str);
		
		while ( m.find() ) {
			sb.append(str.substring(lastPos, m.start()));
			
			switch ( m.group(1) ) {
				case "B" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_BOLD;
					else
						dCmd = Decor.DeCmd.DCE_BOLD;
					break;

				case "I" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_ITALIC;
					else
						dCmd = Decor.DeCmd.DCE_ITALIC;
					break;

				case "U" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_UNDERLINE;
					else
						dCmd = Decor.DeCmd.DCE_UNDERLINE;
					break;

				case "X" : 
					if ( m.group(2).charAt(0) == '+' )
						dCmd = Decor.DeCmd.DCS_UPIDX;
					else
						dCmd = Decor.DeCmd.DCE_UPIDX;
					break;

				case "x" : 
					if ( m.group(2).charAt(0) == '+')
						dCmd = Decor.DeCmd.DCS_DNIDX;
					else
						dCmd = Decor.DeCmd.DCE_DNIDX;
					break;
					
				case "F" : 
					if ( m.group(2).charAt(0) == '+')
						dCmd = Decor.DeCmd.DCS_FNOTE;
					else
						dCmd = Decor.DeCmd.DCE_FNOTE;
					break;
					
				default :
					throw new
						TFException("PARALINE", 
								String.format("[PerpareString] Invalid decoration command [%s] at position [%d] in str\n\"%s\"", 
											  m.group(1), m.start(), str));
			}
			
			decors.add(new Decor(dCmd, m.start() - compensator, m.group(4)));
				
			
			// compensation offset for the DecoPair position
			// it should be decreased for the length of previous match
			compensator += m.group().length();
			
			lastPos = m.end();
		}
		
		if ( lastPos < str.length() )
			sb.append(str.substring(lastPos));
		
		
		return new DecoratedStr(sb.toString(), decors.toArray(new Decor[0]));
	}
	
	/**
	 * Returns content of ParaLine as a Decorated string object
	 * 
	 * @return decorated string equal to content of the ParaLine
	 */
	public DecoratedStr GetDecoratedStr() {
		
		return new DecoratedStr( buff.toString(), decors.toArray( new Decor[0] ) );
	}
	
	
	 
	 //* @see textformatter.TFExcDataLoad#getID()
	 
	@Override
	public String getID() {
		return "PARALINE: ";
	}
}
//-----------------------------------------------------------------------------