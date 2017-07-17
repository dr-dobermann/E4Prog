/********************************************************
* Project : TextFormatter
* 
* Filename: Para.java
* Package : textformatter
* 
* Author: dr.Dobermann (c) 2017
********************************************************/
package textformatter;


import java.util.ArrayList;
import java.util.HashMap;

import lombok.*;
import textformatter.Para.PAlign;

class Para {

	enum PAlign { 
		PA_AS_IS,
		PA_LEFT,
		PA_RIGHT,
		PA_CENTER,
		PA_FILL
	};
	
	enum ParagraphCommand {
	  PC_SNTC_END,
		PC_BEGIN,
		PC_NONE,
		PC_END
	}
	
}

class Footnote {
	
	private @Getter ArrayList<ParaLine> lines = new ArrayList<>();
	
	private @Getter int NoteID;
	
	public Footnote( int id ) {
		NoteID = id;
	}
	
	public void AddLine( ParaLine pl ) {
		
		lines.add( pl );
	}
} 

class ParaSettings {
	
	private @Getter @Setter int width = 72;
	private @Getter @Setter int interval = 1;
	private @Getter @Setter int spaces[] = new int[] {0, 0};
	private @Getter @Setter int margins[] = new int[] {0, 0};
	private @Getter @Setter int indent = 0;
	
	private @Getter @Setter PAlign align;
	
	public ParaSettings() {
	}
	
	public ParaSettings( int fw,
			             int intrv,
			             int sp[],
			             int mr[],
			             int ind,
			             PAlign algn ) {
		
		width = fw;
		interval = intrv;
		spaces = sp;
		margins = mr;
		indent = ind;
		align = algn;
	}
	
	public ParaSettings Copy() {
		
		ParaSettings nSet = new ParaSettings();
		
		nSet.width = width;
		nSet.indent = indent;
		nSet.interval = interval;
		nSet.margins = margins;
		nSet.spaces = spaces;
		nSet.align = align;
		
		return nSet;
	}
	
	public int GetTextWidht( boolean firstLine ) {
		
		return width - 
			   margins[0] - 
			   margins[1] - 
			   ( firstLine ? indent : 0 );
	}
	
}

class Command {
	
	enum CommandName { 
		CMD_SIZE, 
		CMD_ALIGN,
		CMD_PAR,
		CMD_MARGIN,
		CMD_INTERVAL,
		CMD_FEEDLINE,
		CMD_FEED,
		CMD_NEWPAGE,
		CMD_LEFT,
		CMD_HEADER, 
		CMD_PNUM, 
		CMD_PB, 
		CMD_FOOTNOTE,
		CMD_ALIAS
	}
	
	private CommandName command;
	
	private HashMap<String, String> params = new HashMap<>();
	
	public Command( Command.CommandName cmd ) {
		
		command = cmd;
	}
	
	public Command AddParam( String pname, String pvalue ) {
		
		params.put( pname, pvalue );
		
		return this;
	}
}
