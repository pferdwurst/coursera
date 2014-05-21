/*
 *  The scanner definition for COOL.
 */
import java_cup.runtime.Symbol;


class CoolLexer implements java_cup.runtime.Scanner {
	private final int YY_BUFFER_SIZE = 512;
	private final int YY_F = -1;
	private final int YY_NO_STATE = -1;
	private final int YY_NOT_ACCEPT = 0;
	private final int YY_START = 1;
	private final int YY_END = 2;
	private final int YY_NO_ANCHOR = 4;
	private final int YY_BOL = 128;
	private final int YY_EOF = 129;

/*  Stuff enclosed in %{ %} is copied verbatim to the lexer class
 *  definition, all the extra variables/functions you want to use in the
 *  lexer actions should go here.  Don't remove or modify anything that
 *  was there initially.  */
    // Max size of string constants
    static int MAX_STR_CONST = 1025;
    // For assembling string constants
    StringBuffer string_buf = new StringBuffer();
    /* String processing flags */
    private boolean nullInString 	= false;
    /* set every time a "\\" is encountered 
	(that's not escaping a newline)
	Reset when a new \ is encountered or after a newline
	PER LINE ONLY
   */
    private boolean backslashEscaped	= false;
    int stringLength() {
	return string_buf.length();
    }
    void close_string() {
	nullInString = false;
	backslashEscaped = false;
	yybegin(YYINITIAL);
	string_buf.delete(0, stringLength() > 0 ? stringLength() : 0) ;
    }
    // For assembling comments (debugging only)
    StringBuffer comment_buf = new StringBuffer();
    private int nested = 0;
    void close_comments() {
	//System.out.println("COMMENT: " + comment_buf.toString()); 
	comment_buf.delete(0, comment_buf.length() > 0 ? comment_buf.length() : 0 );
    }
    private int curr_lineno = 1;
    int get_curr_lineno() {
	return curr_lineno;
    }
    enum CommentType { DOUBLE_DASH, PAREN_STAR};
    private CommentType current_comment_type;
    private AbstractSymbol filename;
    void set_filename(String fname) {
	filename = AbstractTable.stringtable.addString(fname);
    }
    AbstractSymbol curr_filename() {
	return filename;
    }
	private java.io.BufferedReader yy_reader;
	private int yy_buffer_index;
	private int yy_buffer_read;
	private int yy_buffer_start;
	private int yy_buffer_end;
	private char yy_buffer[];
	private int yychar;
	private int yyline;
	private boolean yy_at_bol;
	private int yy_lexical_state;

	CoolLexer (java.io.Reader reader) {
		this ();
		if (null == reader) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(reader);
	}

	CoolLexer (java.io.InputStream instream) {
		this ();
		if (null == instream) {
			throw (new Error("Error: Bad input stream initializer."));
		}
		yy_reader = new java.io.BufferedReader(new java.io.InputStreamReader(instream));
	}

	private CoolLexer () {
		yy_buffer = new char[YY_BUFFER_SIZE];
		yy_buffer_read = 0;
		yy_buffer_index = 0;
		yy_buffer_start = 0;
		yy_buffer_end = 0;
		yychar = 0;
		yyline = 0;
		yy_at_bol = true;
		yy_lexical_state = YYINITIAL;

/*  Stuff enclosed in %init{ %init} is copied verbatim to the lexer
 *  class constructor, all the extra initialization you want to do should
 *  go here.  Don't remove or modify anything that was there initially. */
    // empty for now
	}

	private boolean yy_eof_done = false;
	private final int STRING = 1;
	private final int YYINITIAL = 0;
	private final int COMMENT = 2;
	private final int yy_state_dtrans[] = {
		0,
		47,
		57
	};
	private void yybegin (int state) {
		yy_lexical_state = state;
	}
	private int yy_advance ()
		throws java.io.IOException {
		int next_read;
		int i;
		int j;

		if (yy_buffer_index < yy_buffer_read) {
			return yy_buffer[yy_buffer_index++];
		}

		if (0 != yy_buffer_start) {
			i = yy_buffer_start;
			j = 0;
			while (i < yy_buffer_read) {
				yy_buffer[j] = yy_buffer[i];
				++i;
				++j;
			}
			yy_buffer_end = yy_buffer_end - yy_buffer_start;
			yy_buffer_start = 0;
			yy_buffer_read = j;
			yy_buffer_index = j;
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}

		while (yy_buffer_index >= yy_buffer_read) {
			if (yy_buffer_index >= yy_buffer.length) {
				yy_buffer = yy_double(yy_buffer);
			}
			next_read = yy_reader.read(yy_buffer,
					yy_buffer_read,
					yy_buffer.length - yy_buffer_read);
			if (-1 == next_read) {
				return YY_EOF;
			}
			yy_buffer_read = yy_buffer_read + next_read;
		}
		return yy_buffer[yy_buffer_index++];
	}
	private void yy_move_end () {
		if (yy_buffer_end > yy_buffer_start &&
		    '\n' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
		if (yy_buffer_end > yy_buffer_start &&
		    '\r' == yy_buffer[yy_buffer_end-1])
			yy_buffer_end--;
	}
	private boolean yy_last_was_cr=false;
	private void yy_mark_start () {
		int i;
		for (i = yy_buffer_start; i < yy_buffer_index; ++i) {
			if ('\n' == yy_buffer[i] && !yy_last_was_cr) {
				++yyline;
			}
			if ('\r' == yy_buffer[i]) {
				++yyline;
				yy_last_was_cr=true;
			} else yy_last_was_cr=false;
		}
		yychar = yychar
			+ yy_buffer_index - yy_buffer_start;
		yy_buffer_start = yy_buffer_index;
	}
	private void yy_mark_end () {
		yy_buffer_end = yy_buffer_index;
	}
	private void yy_to_mark () {
		yy_buffer_index = yy_buffer_end;
		yy_at_bol = (yy_buffer_end > yy_buffer_start) &&
		            ('\r' == yy_buffer[yy_buffer_end-1] ||
		             '\n' == yy_buffer[yy_buffer_end-1] ||
		             2028/*LS*/ == yy_buffer[yy_buffer_end-1] ||
		             2029/*PS*/ == yy_buffer[yy_buffer_end-1]);
	}
	private java.lang.String yytext () {
		return (new java.lang.String(yy_buffer,
			yy_buffer_start,
			yy_buffer_end - yy_buffer_start));
	}
	private int yylength () {
		return yy_buffer_end - yy_buffer_start;
	}
	private char[] yy_double (char buf[]) {
		int i;
		char newbuf[];
		newbuf = new char[2*buf.length];
		for (i = 0; i < buf.length; ++i) {
			newbuf[i] = buf[i];
		}
		return newbuf;
	}
	private final int YY_E_INTERNAL = 0;
	private final int YY_E_MATCH = 1;
	private java.lang.String yy_error_string[] = {
		"Error: Internal error.\n",
		"Error: Unmatched input.\n"
	};
	private void yy_error (int code,boolean fatal) {
		java.lang.System.out.print(yy_error_string[code]);
		java.lang.System.out.flush();
		if (fatal) {
			throw new Error("Fatal Error.\n");
		}
	}
	private int[][] unpackFromString(int size1, int size2, String st) {
		int colonIndex = -1;
		String lengthString;
		int sequenceLength = 0;
		int sequenceInteger = 0;

		int commaIndex;
		String workString;

		int res[][] = new int[size1][size2];
		for (int i= 0; i < size1; i++) {
			for (int j= 0; j < size2; j++) {
				if (sequenceLength != 0) {
					res[i][j] = sequenceInteger;
					sequenceLength--;
					continue;
				}
				commaIndex = st.indexOf(',');
				workString = (commaIndex==-1) ? st :
					st.substring(0, commaIndex);
				st = st.substring(commaIndex+1);
				colonIndex = workString.indexOf(':');
				if (colonIndex == -1) {
					res[i][j]=Integer.parseInt(workString);
					continue;
				}
				lengthString =
					workString.substring(colonIndex+1);
				sequenceLength=Integer.parseInt(lengthString);
				workString=workString.substring(0,colonIndex);
				sequenceInteger=Integer.parseInt(workString);
				res[i][j] = sequenceInteger;
				sequenceLength--;
			}
		}
		return res;
	}
	private int yy_acpt[] = {
		/* 0 */ YY_NO_ANCHOR,
		/* 1 */ YY_NO_ANCHOR,
		/* 2 */ YY_NO_ANCHOR,
		/* 3 */ YY_NO_ANCHOR,
		/* 4 */ YY_NO_ANCHOR,
		/* 5 */ YY_NO_ANCHOR,
		/* 6 */ YY_NO_ANCHOR,
		/* 7 */ YY_NO_ANCHOR,
		/* 8 */ YY_NO_ANCHOR,
		/* 9 */ YY_NO_ANCHOR,
		/* 10 */ YY_NO_ANCHOR,
		/* 11 */ YY_NO_ANCHOR,
		/* 12 */ YY_NO_ANCHOR,
		/* 13 */ YY_NO_ANCHOR,
		/* 14 */ YY_NO_ANCHOR,
		/* 15 */ YY_NO_ANCHOR,
		/* 16 */ YY_NO_ANCHOR,
		/* 17 */ YY_NO_ANCHOR,
		/* 18 */ YY_NO_ANCHOR,
		/* 19 */ YY_NO_ANCHOR,
		/* 20 */ YY_NO_ANCHOR,
		/* 21 */ YY_NO_ANCHOR,
		/* 22 */ YY_NO_ANCHOR,
		/* 23 */ YY_NO_ANCHOR,
		/* 24 */ YY_NO_ANCHOR,
		/* 25 */ YY_NO_ANCHOR,
		/* 26 */ YY_NO_ANCHOR,
		/* 27 */ YY_NO_ANCHOR,
		/* 28 */ YY_NO_ANCHOR,
		/* 29 */ YY_NO_ANCHOR,
		/* 30 */ YY_NO_ANCHOR,
		/* 31 */ YY_NO_ANCHOR,
		/* 32 */ YY_NO_ANCHOR,
		/* 33 */ YY_NO_ANCHOR,
		/* 34 */ YY_NO_ANCHOR,
		/* 35 */ YY_NO_ANCHOR,
		/* 36 */ YY_NO_ANCHOR,
		/* 37 */ YY_NO_ANCHOR,
		/* 38 */ YY_NO_ANCHOR,
		/* 39 */ YY_NO_ANCHOR,
		/* 40 */ YY_NO_ANCHOR,
		/* 41 */ YY_NO_ANCHOR,
		/* 42 */ YY_NO_ANCHOR,
		/* 43 */ YY_NO_ANCHOR,
		/* 44 */ YY_NO_ANCHOR,
		/* 45 */ YY_NO_ANCHOR,
		/* 46 */ YY_NO_ANCHOR,
		/* 47 */ YY_NOT_ACCEPT,
		/* 48 */ YY_NO_ANCHOR,
		/* 49 */ YY_NO_ANCHOR,
		/* 50 */ YY_NO_ANCHOR,
		/* 51 */ YY_NO_ANCHOR,
		/* 52 */ YY_NO_ANCHOR,
		/* 53 */ YY_NO_ANCHOR,
		/* 54 */ YY_NO_ANCHOR,
		/* 55 */ YY_NO_ANCHOR,
		/* 56 */ YY_NO_ANCHOR,
		/* 57 */ YY_NOT_ACCEPT,
		/* 58 */ YY_NO_ANCHOR,
		/* 59 */ YY_NO_ANCHOR,
		/* 60 */ YY_NO_ANCHOR,
		/* 61 */ YY_NO_ANCHOR,
		/* 62 */ YY_NO_ANCHOR,
		/* 63 */ YY_NO_ANCHOR,
		/* 64 */ YY_NO_ANCHOR,
		/* 65 */ YY_NO_ANCHOR,
		/* 66 */ YY_NO_ANCHOR,
		/* 67 */ YY_NO_ANCHOR,
		/* 68 */ YY_NO_ANCHOR,
		/* 69 */ YY_NO_ANCHOR,
		/* 70 */ YY_NO_ANCHOR,
		/* 71 */ YY_NO_ANCHOR,
		/* 72 */ YY_NO_ANCHOR,
		/* 73 */ YY_NO_ANCHOR,
		/* 74 */ YY_NO_ANCHOR,
		/* 75 */ YY_NO_ANCHOR,
		/* 76 */ YY_NO_ANCHOR,
		/* 77 */ YY_NO_ANCHOR,
		/* 78 */ YY_NO_ANCHOR,
		/* 79 */ YY_NO_ANCHOR,
		/* 80 */ YY_NO_ANCHOR,
		/* 81 */ YY_NO_ANCHOR,
		/* 82 */ YY_NO_ANCHOR,
		/* 83 */ YY_NO_ANCHOR,
		/* 84 */ YY_NO_ANCHOR,
		/* 85 */ YY_NO_ANCHOR,
		/* 86 */ YY_NO_ANCHOR,
		/* 87 */ YY_NO_ANCHOR,
		/* 88 */ YY_NO_ANCHOR,
		/* 89 */ YY_NO_ANCHOR,
		/* 90 */ YY_NO_ANCHOR,
		/* 91 */ YY_NO_ANCHOR,
		/* 92 */ YY_NO_ANCHOR,
		/* 93 */ YY_NO_ANCHOR,
		/* 94 */ YY_NO_ANCHOR,
		/* 95 */ YY_NO_ANCHOR,
		/* 96 */ YY_NO_ANCHOR,
		/* 97 */ YY_NO_ANCHOR,
		/* 98 */ YY_NO_ANCHOR,
		/* 99 */ YY_NO_ANCHOR,
		/* 100 */ YY_NO_ANCHOR,
		/* 101 */ YY_NO_ANCHOR,
		/* 102 */ YY_NO_ANCHOR,
		/* 103 */ YY_NO_ANCHOR
	};
	private int yy_cmap[] = unpackFromString(1,130,
"7:9,2,1,2:3,7:18,2,7,8,7:5,3,6,4,28,26,5,29,27,39:10,33,23,24,25,38,7,37,14" +
",40,22,32,12,13,40,19,17,40:2,15,40,18,21,20,40,10,16,9,11,31,30,40:3,7:4,4" +
"1,7,14,40,22,32,12,13,40,19,17,40:2,15,40,18,21,20,40,10,16,9,11,31,30,40:3" +
",36,7,35,34,7,0:2")[0];

	private int yy_rmap[] = unpackFromString(1,104,
"0,1:2,2,3,4,1:3,5,1,6,7,1:9,8,1:3,9:2,10,9,1:3,9:14,11,1:7,12,13,14,15,16,1" +
"7,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,9,37,38,39,40,41" +
",42,43,44,45,46,47,48,49,50,51,52,53,54,9,55,56,57,58")[0];

	private int yy_nxt[][] = unpackFromString(59,42,
"1,2,55,3,4,5,6,7,8,9,99:2,100,56,99,76,99,59,77,99,101,61,102,10,11,12,13,1" +
"4,15,16,103,99:2,17,18,19,20,21,7,22,99,7,-1:46,23,-1:43,24,-1:40,25,-1:45," +
"99,78,99:8,79,99:3,-1:7,99:3,-1:6,80,99,80,-1:5,30,-1:19,31,-1:54,32,-1:42," +
"22,-1:11,99:14,-1:7,99:3,-1:6,80,99,80,-1:9,99:10,92,99:3,-1:7,99:3,-1:6,80" +
",99,80,1,48,49:6,50,49:33,-1:2,55,-1:48,99:5,83,99:2,26,99:5,-1:7,99:3,-1:6" +
",80,99,80,1,51,52,58,60,52:37,-1:4,53,-1:46,99:4,27,99:2,85,99,28,99:4,-1:7" +
",99:3,-1:6,80,99,80,-1:6,54,-1:44,99:4,29,99:9,-1:7,99:3,-1:6,80,99,80,-1:9" +
",33,99:13,-1:7,99:3,-1:6,80,99,80,-1:9,99:14,-1:7,34,99:2,-1:6,80,99,80,-1:" +
"9,35,99:13,-1:7,99:3,-1:6,80,99,80,-1:9,99:3,36,99:10,-1:7,99:3,-1:6,80,99," +
"80,-1:9,99:9,37,99:4,-1:7,99:3,-1:6,80,99,80,-1:9,99:3,38,99:10,-1:7,99:3,-" +
"1:6,80,99,80,-1:9,99:13,39,-1:7,99:3,-1:6,80,99,80,-1:9,99:11,40,99:2,-1:7," +
"99:3,-1:6,80,99,80,-1:9,99:6,41,99:7,-1:7,99:3,-1:6,80,99,80,-1:9,99:3,42,9" +
"9:10,-1:7,99:3,-1:6,80,99,80,-1:9,99:7,43,99:6,-1:7,99:3,-1:6,80,99,80,-1:9" +
",99:3,44,99:10,-1:7,99:3,-1:6,80,99,80,-1:9,99:14,-1:7,99:2,45,-1:6,80,99,8" +
"0,-1:9,99:7,46,99:6,-1:7,99:3,-1:6,80,99,80,-1:9,99:3,62,99:8,84,99,-1:7,99" +
":3,-1:6,80,99,80,-1:9,99:3,63,99:8,64,99,-1:7,99:3,-1:6,80,99,80,-1:9,99:2," +
"65,99:11,-1:7,99:3,-1:6,80,99,80,-1:9,99:3,66,99:10,-1:7,99:3,-1:6,80,99,80" +
",-1:9,99:7,67,99:6,-1:7,99:3,-1:6,80,99,80,-1:9,99:5,68,99:8,-1:7,99:3,-1:6" +
",80,99,80,-1:9,99:6,90,99:7,-1:7,99:3,-1:6,80,99,80,-1:9,99:12,69,99,-1:7,9" +
"9:3,-1:6,80,99,80,-1:9,99:14,-1:7,99,91,99,-1:6,80,99,80,-1:9,99:12,70,99,-" +
"1:7,99:3,-1:6,80,99,80,-1:9,99:7,71,99:6,-1:7,99:3,-1:6,80,99,80,-1:9,99:5," +
"93,99:8,-1:7,99:3,-1:6,80,99,80,-1:9,99:8,94,99:5,-1:7,99:3,-1:6,80,99,80,-" +
"1:9,99:7,65,99:6,-1:7,99:3,-1:6,80,99,80,-1:9,99:12,95,99,-1:7,99:3,-1:6,80" +
",99,80,-1:9,99:3,96,99:10,-1:7,99:3,-1:6,80,99,80,-1:9,99:7,72,99:6,-1:7,99" +
":3,-1:6,80,99,80,-1:9,99:6,73,99:7,-1:7,99:3,-1:6,80,99,80,-1:9,99:8,74,99:" +
"5,-1:7,99:3,-1:6,80,99,80,-1:9,99,97,99:12,-1:7,99:3,-1:6,80,99,80,-1:9,99:" +
"8,98,99:5,-1:7,99:3,-1:6,80,99,80,-1:9,75,99:13,-1:7,99:3,-1:6,80,99,80,-1:" +
"9,99:6,81,82,99:6,-1:7,99:3,-1:6,80,99,80,-1:9,99:12,86,99,-1:7,99:3,-1:6,8" +
"0,99,80,-1:9,99:5,87,88,99:7,-1:7,99:3,-1:6,80,99,80,-1:9,99:10,89,99:3,-1:" +
"7,99:3,-1:6,80,99,80");

	public java_cup.runtime.Symbol next_token ()
		throws java.io.IOException {
		int yy_lookahead;
		int yy_anchor = YY_NO_ANCHOR;
		int yy_state = yy_state_dtrans[yy_lexical_state];
		int yy_next_state = YY_NO_STATE;
		int yy_last_accept_state = YY_NO_STATE;
		boolean yy_initial = true;
		int yy_this_accept;

		yy_mark_start();
		yy_this_accept = yy_acpt[yy_state];
		if (YY_NOT_ACCEPT != yy_this_accept) {
			yy_last_accept_state = yy_state;
			yy_mark_end();
		}
		while (true) {
			if (yy_initial && yy_at_bol) yy_lookahead = YY_BOL;
			else yy_lookahead = yy_advance();
			yy_next_state = YY_F;
			yy_next_state = yy_nxt[yy_rmap[yy_state]][yy_cmap[yy_lookahead]];
			if (YY_EOF == yy_lookahead && true == yy_initial) {

/*  Stuff enclosed in %eofval{ %eofval} specifies java code that is
 *  executed when end-of-file is reached.  If you use multiple lexical
 *  states and want to do something special if an EOF is encountered in
 *  one of those states, place your code in the switch statement.
 *  Ultimately, you should return the EOF symbol, or your lexer won't
 *  work.  */
    switch(yy_lexical_state) {
    	case YYINITIAL:
	/* nothing special to do in the initial state */
	   break;
	case STRING:
	   yybegin(YYINITIAL);
	   return new Symbol(TokenConstants.ERROR, new String("EOF in string constant")); 
	case COMMENT:
	   yybegin(YYINITIAL);
  	   if (current_comment_type == CommentType.PAREN_STAR) {
	   	return new Symbol(TokenConstants.ERROR, new String("EOF in comment")); 
	   }
    	}
    return new Symbol(TokenConstants.EOF);
			}
			if (YY_F != yy_next_state) {
				yy_state = yy_next_state;
				yy_initial = false;
				yy_this_accept = yy_acpt[yy_state];
				if (YY_NOT_ACCEPT != yy_this_accept) {
					yy_last_accept_state = yy_state;
					yy_mark_end();
				}
			}
			else {
				if (YY_NO_STATE == yy_last_accept_state) {
					throw (new Error("Lexical Error: Unmatched Input."));
				}
				else {
					yy_anchor = yy_acpt[yy_last_accept_state];
					if (0 != (YY_END & yy_anchor)) {
						yy_move_end();
					}
					yy_to_mark();
					switch (yy_last_accept_state) {
					case 0:
						{}
					case -2:
						break;
					case 1:
						
					case -3:
						break;
					case 2:
						{ curr_lineno++;}
					case -4:
						break;
					case 3:
						{ return new Symbol(TokenConstants.LPAREN); }
					case -5:
						break;
					case 4:
						{ return new Symbol(TokenConstants.MULT); }
					case -6:
						break;
					case 5:
						{ return new Symbol(TokenConstants.MINUS); }
					case -7:
						break;
					case 6:
						{ return new Symbol(TokenConstants.RPAREN); }
					case -8:
						break;
					case 7:
						{ /* This rule should be the very last
                                     in your lexical specification and
                                     will match match everything not
                                     matched by other lexical rules. */
                                  //System.err.println("LEXER BUG - UNMATCHED::: " + yytext()); 
                                  return new Symbol(TokenConstants.ERROR, new String( yytext() ));
                                }
					case -9:
						break;
					case 8:
						{ yybegin(STRING); }
					case -10:
						break;
					case 9:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -11:
						break;
					case 10:
						{ return new Symbol(TokenConstants.SEMI); }
					case -12:
						break;
					case 11:
						{ return new Symbol(TokenConstants.LT); }
					case -13:
						break;
					case 12:
						{ return new Symbol(TokenConstants.EQ); }
					case -14:
						break;
					case 13:
						{ return new Symbol(TokenConstants.COMMA); }
					case -15:
						break;
					case 14:
						{ return new Symbol(TokenConstants.DIV); }
					case -16:
						break;
					case 15:
						{ return new Symbol(TokenConstants.PLUS); }
					case -17:
						break;
					case 16:
						{ return new Symbol(TokenConstants.DOT); }
					case -18:
						break;
					case 17:
						{ return new Symbol(TokenConstants.COLON); }
					case -19:
						break;
					case 18:
						{ return new Symbol(TokenConstants.NEG); }
					case -20:
						break;
					case 19:
						{ return new Symbol(TokenConstants.RBRACE); }
					case -21:
						break;
					case 20:
						{ return new Symbol(TokenConstants.LBRACE); }
					case -22:
						break;
					case 21:
						{ return new Symbol(TokenConstants.AT); }
					case -23:
						break;
					case 22:
						{ /* integer */ return new Symbol(TokenConstants.INT_CONST, AbstractTable.inttable.addString(yytext())); }
					case -24:
						break;
					case 23:
						{ yybegin(COMMENT); current_comment_type = CommentType.PAREN_STAR; }
					case -25:
						break;
					case 24:
						{ return new Symbol(TokenConstants.ERROR, new String("Unmatched *)" )); }
					case -26:
						break;
					case 25:
						{ yybegin(COMMENT); current_comment_type = CommentType.DOUBLE_DASH; }
					case -27:
						break;
					case 26:
						{ return new Symbol(TokenConstants.FI); }
					case -28:
						break;
					case 27:
						{ return new Symbol(TokenConstants.IF); }
					case -29:
						break;
					case 28:
						{ return new Symbol(TokenConstants.IN); }
					case -30:
						break;
					case 29:
						{ return new Symbol(TokenConstants.OF); }
					case -31:
						break;
					case 30:
						{ return new Symbol(TokenConstants.ASSIGN); }
					case -32:
						break;
					case 31:
						{ return new Symbol(TokenConstants.LE); }
					case -33:
						break;
					case 32:
						{ return new Symbol(TokenConstants.DARROW); }
					case -34:
						break;
					case 33:
						{ return new Symbol(TokenConstants.LET); }
					case -35:
						break;
					case 34:
						{ return new Symbol(TokenConstants.NEW); }
					case -36:
						break;
					case 35:
						{ return new Symbol(TokenConstants.NOT); }
					case -37:
						break;
					case 36:
						{ char s0 = yytext().charAt(0);
                                                  if ( Character.isUpperCase(s0) ) {
						     return new Symbol( TokenConstants.TYPEID , AbstractTable.idtable.addString(yytext()));
						  } else {
						     return new Symbol(TokenConstants.BOOL_CONST, yytext().toLowerCase());
						  }
						}
					case -38:
						break;
					case 37:
						{ return new Symbol(TokenConstants.THEN); }
					case -39:
						break;
					case 38:
						{ return new Symbol(TokenConstants.ELSE); }
					case -40:
						break;
					case 39:
						{ return new Symbol(TokenConstants.ESAC); }
					case -41:
						break;
					case 40:
						{ return new Symbol(TokenConstants.LOOP); }
					case -42:
						break;
					case 41:
						{ return new Symbol(TokenConstants.POOL); }
					case -43:
						break;
					case 42:
						{ return new Symbol(TokenConstants.CASE); }
					case -44:
						break;
					case 43:
						{ return new Symbol(TokenConstants.CLASS); }
					case -45:
						break;
					case 44:
						{ return new Symbol(TokenConstants.WHILE); }
					case -46:
						break;
					case 45:
						{ return new Symbol(TokenConstants.ISVOID); }
					case -47:
						break;
					case 46:
						{ return new Symbol(TokenConstants.INHERITS); }
					case -48:
						break;
					case 48:
						{
				  		   curr_lineno++;  
						   if (string_buf.length() ==  0) {
								close_string();
								return new Symbol(TokenConstants.ERROR, new String("Unterminated string constant")); 
						   }
						   /* Handle a line break within a string */
						   int lastCharIdx = string_buf.length()-1;
						   char lastChar = string_buf.charAt(lastCharIdx);
							if (lastChar == '\\'  && !backslashEscaped ) {
								/* This is an escaped newline, append to the string buffer */
								string_buf.setCharAt(lastCharIdx, '\n'); 
								backslashEscaped = false;
								nullInString = false;
						   	} else {
								try {
								   if (!nullInString) {
									return new Symbol(TokenConstants.ERROR, new String("Unterminated string constant")); 
								   }
 								} finally {
								close_string();
								}
							}	  
						}
					case -49:
						break;
					case 49:
						{ if ( string_buf.length() == 0)  {
								/* If this is the 1st character, just append it */
					  			string_buf.append(yytext());
					  } else {
								int lastCharIdx = string_buf.length()-1;
				  				char lastChar = string_buf.charAt(lastCharIdx);
								char nextChar = yytext().charAt(0);
								switch (lastChar) {
								   case  '\\': 
					    				  	/* Only \n, \b, \t, \f are allowed escaped characters */
										switch(nextChar) 
						    				{ case 'n': 
											string_buf.setCharAt(lastCharIdx, '\n');
											break;
									  	case 't':
									  		string_buf.setCharAt(lastCharIdx, '\t');
											break;
									  	case 'b':
									  		string_buf.setCharAt(lastCharIdx, '\b');
											break;
									  	case 'f':
									  		string_buf.setCharAt(lastCharIdx, '\f');
											break;
										case '\0': 
											if (!nullInString) {
												nullInString = true;
                                                          					return new Symbol(TokenConstants.ERROR, new String("String contains escaped null character."));
											}
											break;
									  	case '\\':
								       			if (backslashEscaped) {
											  string_buf.append(nextChar);
											  backslashEscaped = false;
											} else {
									  		  string_buf.setCharAt(lastCharIdx, nextChar);
											  backslashEscaped = true;
											}
											break;
									  	default:
								       			if (backslashEscaped) {
											   string_buf.append(nextChar);
											} else {
											  string_buf.setCharAt(lastCharIdx, nextChar);
											}
											backslashEscaped = false;
										}
										break;
							 	   case '\0':  
									if (!nullInString) {
								        	/* error check: is there a null character in the string */
										nullInString = true;
                                                          			return new Symbol(TokenConstants.ERROR, new String("String contains null character."));
									}
									break;
								   default:	
									// Unescaped character
					  				string_buf.append(nextChar);
								}
							} 
					}
					case -50:
						break;
					case 50:
						{ if (string_buf.length() >= MAX_STR_CONST ) { 
						/* error check: is the string too long*/
						close_string();
						return new Symbol(TokenConstants.ERROR, new String("String constant too long")); 
				    	  }
					  if (stringLength() == 0 ) {
							close_string();
				    			return new Symbol( TokenConstants.STR_CONST,  AbstractTable.stringtable.addString(string_buf.toString())); 
					  }
					  /* if previous character is \ and is not escaped, append the quotation mark " */
					  int lastCharIdx = string_buf.length()-1;
				  	  char lastChar = string_buf.charAt(lastCharIdx);
					  if ( lastChar == '\\' && !backslashEscaped ) {
						string_buf.setCharAt(lastCharIdx, '\"');
					  } else { 
					  try {
						    if ( !nullInString) {
				    	  		String s  = string_buf.toString();
				    			return new Symbol( TokenConstants.STR_CONST,  AbstractTable.stringtable.addString(s)); 
						   }
					  } finally {
				    	  	close_string();
					  }
					  }
				   	}
					case -51:
						break;
					case 51:
						{ if ( current_comment_type == CommentType.DOUBLE_DASH) { 
									yybegin(YYINITIAL);	
									close_comments();
			    				} else {
				   					comment_buf.append(yytext());
				     			}
						curr_lineno++; 
					}
					case -52:
						break;
					case 52:
						{ comment_buf.append(yytext()); }
					case -53:
						break;
					case 53:
						{ if ( current_comment_type == CommentType.PAREN_STAR) {
						nested++ ;
					  }
					   comment_buf.append(yytext()); 
					}
					case -54:
						break;
					case 54:
						{ if (nested > 0) 
								{
					    			comment_buf.append(yytext());
					    			nested--;
					  			} else {
					    			yybegin(YYINITIAL); 
					    			close_comments();
					  			}
							}
					case -55:
						break;
					case 55:
						{}
					case -56:
						break;
					case 56:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -57:
						break;
					case 58:
						{ comment_buf.append(yytext()); }
					case -58:
						break;
					case 59:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -59:
						break;
					case 60:
						{ comment_buf.append(yytext()); }
					case -60:
						break;
					case 61:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -61:
						break;
					case 62:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -62:
						break;
					case 63:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -63:
						break;
					case 64:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -64:
						break;
					case 65:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -65:
						break;
					case 66:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -66:
						break;
					case 67:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -67:
						break;
					case 68:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -68:
						break;
					case 69:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -69:
						break;
					case 70:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -70:
						break;
					case 71:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -71:
						break;
					case 72:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -72:
						break;
					case 73:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -73:
						break;
					case 74:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -74:
						break;
					case 75:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -75:
						break;
					case 76:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -76:
						break;
					case 77:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -77:
						break;
					case 78:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -78:
						break;
					case 79:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -79:
						break;
					case 80:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -80:
						break;
					case 81:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -81:
						break;
					case 82:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -82:
						break;
					case 83:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -83:
						break;
					case 84:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -84:
						break;
					case 85:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -85:
						break;
					case 86:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -86:
						break;
					case 87:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -87:
						break;
					case 88:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -88:
						break;
					case 89:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -89:
						break;
					case 90:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -90:
						break;
					case 91:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -91:
						break;
					case 92:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -92:
						break;
					case 93:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -93:
						break;
					case 94:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -94:
						break;
					case 95:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -95:
						break;
					case 96:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -96:
						break;
					case 97:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -97:
						break;
					case 98:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -98:
						break;
					case 99:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -99:
						break;
					case 100:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -100:
						break;
					case 101:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -101:
						break;
					case 102:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -102:
						break;
					case 103:
						{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}
					case -103:
						break;
					default:
						yy_error(YY_E_INTERNAL,false);
					case -1:
					}
					yy_initial = true;
					yy_state = yy_state_dtrans[yy_lexical_state];
					yy_next_state = YY_NO_STATE;
					yy_last_accept_state = YY_NO_STATE;
					yy_mark_start();
					yy_this_accept = yy_acpt[yy_state];
					if (YY_NOT_ACCEPT != yy_this_accept) {
						yy_last_accept_state = yy_state;
						yy_mark_end();
					}
				}
			}
		}
	}
}
