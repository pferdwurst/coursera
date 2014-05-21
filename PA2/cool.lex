/*
 *  The scanner definition for COOL.
 */

import java_cup.runtime.Symbol;



%%

%{

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
%}

%init{

/*  Stuff enclosed in %init{ %init} is copied verbatim to the lexer
 *  class constructor, all the extra initialization you want to do should
 *  go here.  Don't remove or modify anything that was there initially. */

    // empty for now
%init}

%eofval{

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
%eofval}

%class CoolLexer
%cup
%state STRING
%state COMMENT
%char
%line


Digit 		= [0-9]
Letter 		= [a-zA-Z]
Whitespace 	= [ \t\r\013\f]

%ignorecase

%%


<YYINITIAL>\n				{ curr_lineno++;}
<YYINITIAL>{Whitespace}*	{}


<YYINITIAL>"(*"				{ yybegin(COMMENT); current_comment_type = CommentType.PAREN_STAR; }
<YYINITIAL>--				{ yybegin(COMMENT); current_comment_type = CommentType.DOUBLE_DASH; } 
<YYINITIAL>"*)"				{ return new Symbol(TokenConstants.ERROR, new String("Unmatched *)" )); }
<COMMENT>"(*"				{ if ( current_comment_type == CommentType.PAREN_STAR) {
						nested++ ;
					  }
					   comment_buf.append(yytext()); 
					} 
<COMMENT>"*)"				{ if (nested > 0) 
								{
					    			comment_buf.append(yytext());
					    			nested--;
					  			} else {
					    			yybegin(YYINITIAL); 
					    			close_comments();
					  			}
							}
<COMMENT>\n				{ if ( current_comment_type == CommentType.DOUBLE_DASH) { 
									yybegin(YYINITIAL);	
									close_comments();
			    				} else {
				   					comment_buf.append(yytext());
				     			}
						curr_lineno++; 
					}
<COMMENT>[^\n]				{ comment_buf.append(yytext()); }


<YYINITIAL>\"				{ yybegin(STRING); } 
<STRING>\"				{ if (string_buf.length() >= MAX_STR_CONST ) { 
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
<STRING>[^\n]				{ if ( string_buf.length() == 0)  {
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
<STRING>\n	 				{
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


<YYINITIAL>true|false					{ char s0 = yytext().charAt(0);
                                                  if ( Character.isUpperCase(s0) ) {
						     return new Symbol( TokenConstants.TYPEID , AbstractTable.idtable.addString(yytext()));
						  } else {
						     return new Symbol(TokenConstants.BOOL_CONST, yytext().toLowerCase());
						  }
						}
<YYINITIAL>inherits				{ return new Symbol(TokenConstants.INHERITS); }
<YYINITIAL>pool					{ return new Symbol(TokenConstants.POOL); }
<YYINITIAL>case					{ return new Symbol(TokenConstants.CASE); }
<YYINITIAL>\(					{ return new Symbol(TokenConstants.LPAREN); }
<YYINITIAL>\)					{ return new Symbol(TokenConstants.RPAREN); }
<YYINITIAL>;					{ return new Symbol(TokenConstants.SEMI); }
<YYINITIAL>-					{ return new Symbol(TokenConstants.MINUS); }
<YYINITIAL>not					{ return new Symbol(TokenConstants.NOT); }
<YYINITIAL><					{ return new Symbol(TokenConstants.LT); }
<YYINITIAL><=					{ return new Symbol(TokenConstants.LE); }
<YYINITIAL>in					{ return new Symbol(TokenConstants.IN); }
<YYINITIAL>,		{ return new Symbol(TokenConstants.COMMA); }
<YYINITIAL>class		{ return new Symbol(TokenConstants.CLASS); }
<YYINITIAL>fi		{ return new Symbol(TokenConstants.FI); }
<YYINITIAL>/		{ return new Symbol(TokenConstants.DIV); }
<YYINITIAL>loop		{ return new Symbol(TokenConstants.LOOP); }
<YYINITIAL>"+"		{ return new Symbol(TokenConstants.PLUS); }
<YYINITIAL><-		{ return new Symbol(TokenConstants.ASSIGN); }
<YYINITIAL>if		{ return new Symbol(TokenConstants.IF); }
<YYINITIAL>"."		{ return new Symbol(TokenConstants.DOT); }
<YYINITIAL>OF		{ return new Symbol(TokenConstants.OF); }
<YYINITIAL>new		{ return new Symbol(TokenConstants.NEW); }
<YYINITIAL>ISVOID	{ return new Symbol(TokenConstants.ISVOID); }
<YYINITIAL>=		{ return new Symbol(TokenConstants.EQ); }
<YYINITIAL>:		{ return new Symbol(TokenConstants.COLON); }
<YYINITIAL>~		{ return new Symbol(TokenConstants.NEG); }
<YYINITIAL>else		{ return new Symbol(TokenConstants.ELSE); }
<YYINITIAL>while		{ return new Symbol(TokenConstants.WHILE); }
<YYINITIAL>esac		{ return new Symbol(TokenConstants.ESAC); }
<YYINITIAL>let		{ return new Symbol(TokenConstants.LET); }
<YYINITIAL>}		{ return new Symbol(TokenConstants.RBRACE); }
<YYINITIAL>"{"		{ return new Symbol(TokenConstants.LBRACE); }
<YYINITIAL>then		{ return new Symbol(TokenConstants.THEN); }
<YYINITIAL>@					{ return new Symbol(TokenConstants.AT); }
<YYINITIAL>\*					{ return new Symbol(TokenConstants.MULT); }
<YYINITIAL>"=>"					{ return new Symbol(TokenConstants.DARROW); }
<YYINITIAL>{Digit}+		 		{ /* integer */ return new Symbol(TokenConstants.INT_CONST, AbstractTable.inttable.addString(yytext())); }
						
<YYINITIAL>{Letter}({Digit}|{Letter}|_)*	
								{
									/* Test if identifier begins with a capital or lower case letter */
					         		char s0 = yytext().charAt(0);	
						 			int tokenId =  ( Character.isUpperCase(s0) ? TokenConstants.TYPEID : TokenConstants.OBJECTID ); 
						  			return new Symbol(tokenId, AbstractTable.idtable.addString(yytext())); 
								}


.                               { /* This rule should be the very last
                                     in your lexical specification and
                                     will match match everything not
                                     matched by other lexical rules. */
                                  //System.err.println("LEXER BUG - UNMATCHED::: " + yytext()); 
                                  return new Symbol(TokenConstants.ERROR, new String( yytext() ));
                                }
