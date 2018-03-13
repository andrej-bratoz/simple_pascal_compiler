package compiler.lexanal;

import java.io.*;

import compiler.report.*;
import compiler.synanal.*;

%%

%class      PascalLex
%public

%line
%column

/* Vzpostavimo zdruzljivost z orodjem Java Cup.
 * To bi lahko naredili tudi z ukazom %cup,
 * a v tem primeru ne bi mogli uporabiti razreda compiler.lexanal.PascalSym
 * namesto razreda java_cup.runtime.Symbol za opis osnovnih simbolov. */
%cupsym     compiler.synanal.PascalTok
%implements java_cup.runtime.Scanner
%function   next_token
%type       PascalSym
%eofval{
	if (nestedDepth > 0) unclosed();
    if(errors > 0) EndErrorReport();
    return new PascalSym(PascalTok.EOF);
%eofval}
%eofclose

%{

	private static int errors = 0;
	
    private PascalSym sym(int type) 
    {
        return new PascalSym(type, yyline + 1, yycolumn + 1, yytext());
    }
    
	private void unclosed() {
    	Report.warning("Error: (Unclosed comment block) - each '{' must be closed by '}'", commentStartRow, commentStartCol);
    	errors++;
    }
    
	private static int nestedDepth = 0;
	private static int commentStartRow = 0;
	private static int commentStartCol = 0;
    
    private void startComment() {
    	if (nestedDepth == 0) {
    		commentStartRow = yyline + 1;
    		commentStartCol = yycolumn + 1;
    		yybegin(COMMENT);
    	}
    	nestedDepth++;
    }
    
    private void endComment() {
    	nestedDepth--;
    	if (nestedDepth == 0) yybegin(YYINITIAL);
    }
    
    private String errorArrow(int offset)
    {
    	String s = "";
    	for(int i = 0; i < offset; i++)
    	{
    		s += " ";
    	}
    	return "\n"+s+"^";
    }
    
    private void EndErrorReport()
    {
    	System.out.println("Total errors encountered: " + errors + "\nCompilation terminated due to errors...\n"); 
    	System.exit(1);
    }
    
    private void IncreaseErrorCount()
    {
    	errors++;
    	if (errors > 100) 
    	{
    		System.out.println("More than 100 errors...Abroting compilation");
    		System.exit(1);
    	}
    }
    
    private void ReportError()
    {
    	
    	if (yytext().equals("}")) Report.warning("Error (unrecognised token): '"+ yytext() + "' (comment may be closed too many times)" , yyline+1, yycolumn+1);
    	else Report.warning("Error (unrecognised token): '"+ yytext() + "' (charachter not defined)" , yyline+1, yycolumn+1);;
    }
%}

Comment = \{.*\}	

%eof{

%eof}

%xstate COMMENT
		
%%

<YYINITIAL> 
{

	[ \n\t]+						{ }
	"true"|"false"					{ return sym(PascalTok.BOOL_CONST); }
	"<"								{ return sym(PascalTok.LTH); 		}
	"function"						{ return sym(PascalTok.FUNCTION);	}
	"["								{ return sym(PascalTok.LBRACKET);	}
	"const"							{ return sym(PascalTok.CONST); 		}
	"char"							{ return sym(PascalTok.CHAR);		}
	";"								{ return sym(PascalTok.SEMIC);		}
	"integer"						{ return sym(PascalTok.INT);		}
	"array"							{ return sym(PascalTok.ARRAY);		}
	"for"							{ return sym(PascalTok.FOR);		}
	"not"							{ return sym(PascalTok.NOT);		}
	"and"							{ return sym(PascalTok.AND);		}
	"record"						{ return sym(PascalTok.RECORD);		}
	"type"							{ return sym(PascalTok.TYPE);		}
	"nil"							{ return sym(PascalTok.NIL);		}
	"or"							{ return sym(PascalTok.OR);			}
	"boolean"						{ return sym(PascalTok.BOOL);		}
	","								{ return sym(PascalTok.COMMA);		}
	"begin"							{ return sym(PascalTok.BEGIN);		}
	"div"							{ return sym(PascalTok.DIV);		}
	">="							{ return sym(PascalTok.GEQ);		}
	"if"							{ return sym(PascalTok.IF);			}	
	":="							{ return sym(PascalTok.ASSIGN);		}
	"."								{ return sym(PascalTok.DOT);		}
	"\^"							{ return sym(PascalTok.PTR);		}
	"of"							{ return sym(PascalTok.OF);			}
	".."							{ return sym(PascalTok.DOTS);		}
	"]"								{ return sym(PascalTok.RBRACKET);	}
	[0-9][0-9]*	 					{ return sym(PascalTok.INT_CONST);	}
	"program"						{ return sym(PascalTok.PROGRAM);	}
	"typeof"						{ return sym(PascalTok.AUTO);		}
	"*"								{ return sym(PascalTok.MUL);		}
	"+"								{ return sym(PascalTok.ADD);		}
	"("								{ return sym(PascalTok.LPARENTHESIS);}
	"<>"							{ return sym(PascalTok.NEQ);		}
	">"								{ return sym(PascalTok.GTH);		}
	"="								{ return sym(PascalTok.EQU);		}
	":"								{ return sym(PascalTok.COLON);		}
	"else"							{ return sym(PascalTok.ELSE);		}
	"to"							{ return sym(PascalTok.TO);			}
	"while"							{ return sym(PascalTok.WHILE);		}
	"?"								{ return sym(PascalTok.QUEST);		}
	"then"							{ return sym(PascalTok.THEN);		}
	")"								{ return sym(PascalTok.RPARENTHESIS);}
	"<="							{ return sym(PascalTok.LEQ);		}
	"end"							{ return sym(PascalTok.END);		}
	"var"							{ return sym(PascalTok.VAR);		}
	"procedure"						{ return sym(PascalTok.PROCEDURE);	}
	"do"							{ return sym(PascalTok.DO);			}
	[_a-zA-Z][a-zA-Z_0-9]*			{ return sym(PascalTok.IDENTIFIER);	}
	"-"								{ return sym(PascalTok.SUB);		}
	\'(\'\'|[^'])\'					{ return sym(PascalTok.CHAR_CONST);	}
	"{"								{ startComment(); }
	.								{ ReportError();IncreaseErrorCount();}
	
	<COMMENT> 
	{
	
		"{"						{ startComment(); }
		"}"						{ endComment(); }
		[ \n\r\t]+				{ }
		.						{ }
	}
}