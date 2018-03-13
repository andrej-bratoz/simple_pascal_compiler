package compiler.semanal;

import compiler.abstree.*;
import compiler.abstree.tree.*;
import compiler.report.Report;
import compiler.semanal.type.*;

public class SemTypeChecker implements AbsVisitor
{
	public boolean error = false;
	public final SemType ERROR   = new SemTypeError();
	public final SemType BOOL    = new SemAtomType(SemAtomType.BOOL);
    public final SemType CHAR    = new SemAtomType(SemAtomType.CHAR);
    public final SemType INTEGER = new SemAtomType(SemAtomType.INT);
    public final SemType VOID    = new SemAtomType(SemAtomType.VOID);
    private AbsFunDecl currentFunDecl = null;
	
    private void RaiseError(AbsTree acceptor, String msg) 
    {
    	Report.warning(msg, acceptor.begLine, acceptor.begColumn, acceptor.endLine, acceptor.endColumn);
    	error = true; SemDesc.setActualType(acceptor, ERROR);acceptor.error = true;
    }
    
    
    private void AddIntristicFunctionByName(String name, SemType parameter, SemType returnType) 
    {
    	SemSubprogramType type = new SemSubprogramType(returnType);
    	if (parameter != null) type.addParType(parameter);
    	SemDesc.setActualType(SemTable.fnd(name), type);
    }

    public SemTypeChecker()
    {
    	AddIntristicFunctionByName("putint", INTEGER, VOID);
    	AddIntristicFunctionByName("putch", CHAR, VOID);
    	AddIntristicFunctionByName("getch", null, CHAR);
    	AddIntristicFunctionByName("getint", null, INTEGER);
    	AddIntristicFunctionByName("ord", CHAR, INTEGER);
    	AddIntristicFunctionByName("chr", INTEGER, CHAR);
    	AddIntristicFunctionByName("free", new SemPointerType(VOID), VOID);
    }
    
	@Override
	public void visit(AbsAlloc acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.type.accept(this);
		SemType type = SemDesc.getActualType(acceptor.type);
		SemDesc.setActualType(acceptor, new SemPointerType(type));
	}

	@Override
	public void visit(AbsArrayType acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.type.accept(this);
		acceptor.loBound.accept(this);
		acceptor.hiBound.accept(this);
		SemType loType = SemDesc.getActualType(acceptor.loBound);
		SemType hiType = SemDesc.getActualType(acceptor.hiBound);
		SemType arrType = SemDesc.getActualType(acceptor.type);
		if (loType.coercesTo(INTEGER) && hiType.coercesTo(INTEGER))
		{

			SemDesc.setActualType(acceptor, arrType);
			Integer lo = SemDesc.getActualConst(acceptor.loBound);
			Integer hi = SemDesc.getActualConst(acceptor.hiBound);
			if (lo != null && hi != null)
			{
				SemDesc.setActualType(acceptor, new SemArrayType(arrType, lo, hi));
			}
			else
			{
				RaiseError(acceptor, "Array bounds must be CONST");
			}
			
		}
		else
		{
			RaiseError(acceptor, "Array bounds must be INTEGER type");
		}
		
	}

	@Override
	public void visit(AbsAssignStmt acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		if (!acceptor.SpecialAuto)
		acceptor.dstExpr.accept(this);
		acceptor.srcExpr.accept(this);
		boolean legalAssig = false;
		SemType dstType = SemDesc.getActualType(acceptor.srcExpr);
		SemType srcType = SemDesc.getActualType(acceptor.srcExpr);
		
        
        /*if (((srcType instanceof SemAtomType) && (dstType instanceof SemPointerType))
        		|| ((dstType instanceof SemAtomType) && (srcType instanceof SemPointerType)))
        {
        	RaiseError(acceptor,"Cannot assign ATOM type and POINTER type");
        }*/
		
		if ((srcType == null) || (dstType == null))
		{
			if (srcType == null) System.out.println("RIGHT IS NULL");
			if (dstType == null) System.out.println("LEFT IS NULL");
			RaiseError(acceptor, "Type could not be identified");
			return;
		}
		
		if (!(srcType.coercesTo(dstType)) && !(dstType instanceof SemSubprogramType))
		{
			System.out.println(srcType);
			System.out.println(dstType);
			RaiseError(acceptor,"Cannot assign ATOM type and POINTER type");
			return;
		}
		if (acceptor.dstExpr instanceof AbsValName)
		{
			AbsDecl decl = SemDesc.getNameDecl(acceptor.dstExpr);
			if (decl instanceof AbsVarDecl)
			{
				legalAssig = true;
			}
		}
		else if (acceptor.dstExpr instanceof AbsBinExpr)  // DST
		{
			AbsBinExpr expr = (AbsBinExpr)acceptor.dstExpr;
			legalAssig = (expr.oper == AbsBinExpr.ARRACCESS || expr.oper == AbsBinExpr.RECACCESS);
		}
		 else if (acceptor.dstExpr instanceof AbsUnExpr) 
	    {
	        AbsUnExpr expression = (AbsUnExpr) acceptor.dstExpr;
	        legalAssig = (expression.oper == AbsUnExpr.VAL);
	    }
	
	    // legal destination must be either atomic or a pointer
	    if ((legalAssig && (dstType instanceof SemAtomType || dstType instanceof SemPointerType) || dstType instanceof SemSubprogramType)) 
        {
	        if (dstType instanceof SemSubprogramType) 
	        {
		        dstType = ((SemSubprogramType) dstType).getResultType();
	        }
	        boolean typesMatch = dstType.coercesTo(srcType);
	        if (!typesMatch) 
	        {
	        	RaiseError(acceptor,String.format("Assignment type mismatch"));
	        }
	    }
	    else 
	    {
	        RaiseError(acceptor,String.format("Illegal assignment destination"));
	        return;
	    }
	    SemDesc.setActualType(acceptor, SemDesc.getActualType(acceptor.dstExpr));
	}

	@Override
	public void visit(AbsAtomConst acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		if (acceptor.type == AbsAtomConst.BOOL)
		{
			SemDesc.setActualType(acceptor, BOOL);
			SemDesc.setActualConst(acceptor, (acceptor.value.equals("true") ? 1 : 0));
		}
		else if (acceptor.type == AbsAtomConst.INT)
		{
			SemDesc.setActualType(acceptor, INTEGER);
			SemDesc.setActualConst(acceptor, Integer.parseInt(acceptor.value));
		}

		else if (acceptor.type == AbsAtomConst.CHAR)
		{
			SemDesc.setActualType(acceptor, CHAR);
		}
		else
		{
			RaiseError(acceptor, "Not a atom const type");
			SemDesc.setActualConst(acceptor, (int)(acceptor.value.charAt(0)));
		}

	}

	@Override
	public void visit(AbsAtomType acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		if(acceptor.type == AbsAtomConst.BOOL)
		{
			SemDesc.setActualType(acceptor, BOOL);
		}	
		else if (acceptor.type == AbsAtomConst.CHAR)
		{
			SemDesc.setActualType(acceptor, CHAR);
		}
		else if (acceptor.type == AbsAtomConst.INT)
		{
			SemDesc.setActualType(acceptor, INTEGER);
		}
		else
		{
			RaiseError(acceptor, "Not a valid atom CONST type");
		}
		
	}

	@Override
	public void visit(AbsBinExpr acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		final int oper = acceptor.oper;
		SemType t1, t2;
		Integer i1, i2;
		if (oper == AbsBinExpr.ADD || oper == AbsBinExpr.SUB
				|| oper == AbsBinExpr.MUL || oper == AbsBinExpr.DIV)
		{
			acceptor.fstExpr.accept(this);
		    acceptor.sndExpr.accept(this);
		    t1 = SemDesc.getActualType(acceptor.fstExpr);
		    t2 = SemDesc.getActualType(acceptor.sndExpr);
		    

		    if (t1.coercesTo(t2) && t1.coercesTo(INTEGER))
		    {
		    	SemDesc.setActualType(acceptor, INTEGER);
		    	i1 = SemDesc.getActualConst(acceptor.fstExpr);
		    	i2 = SemDesc.getActualConst(acceptor.sndExpr);
		    	if (i1 != null && i2 != null)
		    	{
		    		switch(oper)
		    		{
		    		case AbsBinExpr.ADD:
		    			SemDesc.setActualConst(acceptor, i1+i2);
		    			break;
		    		case AbsBinExpr.SUB:
		    			SemDesc.setActualConst(acceptor, i1-i2);
		    			break;
		    		case AbsBinExpr.MUL:
		    			SemDesc.setActualConst(acceptor, i1*i2);
		    			break;
		    		case AbsBinExpr.DIV:
		    			if (i2 != 0)
		    			{
		    				SemDesc.setActualConst(acceptor, i1/i2);
		    			}
		    			else
		    			{
		    				RaiseError(acceptor, "INTEGER division by zero is not allowed");
		    			}
		    		}
		    	}
		    }
		    else
		    {
		    	RaiseError(acceptor, "Operation ADD, SUB, MUL, DIV require INTEGER operands");
		    }
		}
		else if (oper == AbsBinExpr.AND || oper == AbsBinExpr.OR)
		{
			acceptor.fstExpr.accept(this);
		    acceptor.sndExpr.accept(this);
		    t1 = SemDesc.getActualType(acceptor.fstExpr);
		    t2 = SemDesc.getActualType(acceptor.sndExpr);
		    if (t1.coercesTo(t2) && t1.coercesTo(BOOL))
		    {
		    	SemDesc.setActualType(acceptor, BOOL);
		    	i1 = SemDesc.getActualConst(acceptor.fstExpr);
		    	i2 = SemDesc.getActualConst(acceptor.sndExpr);
		    	if (i1 != null && i2 != null)
		    	{
		    		switch(oper)
		    		{
		    		case AbsBinExpr.OR:
		    			if (i1 == 1 || i2 == 1)
		    			{
		    				SemDesc.setActualConst(acceptor, 1);
		    			}
		    			else
		    			{
		    				SemDesc.setActualConst(acceptor, 0);
		    			}
		    			break;
		    		case AbsBinExpr.ADD:
		    			if (i1 == 0 || i2 == 0)
		    			{
		    				SemDesc.setActualConst(acceptor, 0);
		    			}
		    			else
		    			{
		    				SemDesc.setActualConst(acceptor, 1);
		    			}
		    			break;
		    		}
		    	}
		    }
		    else
		    {
		    	RaiseError(acceptor, "Operators AND, OR require BOOL operands");
		    }
		}
		else if (oper == AbsBinExpr.EQU || oper == AbsBinExpr.GEQ
				|| oper == AbsBinExpr.GTH || oper == AbsBinExpr.LEQ 
				|| oper == AbsBinExpr.LTH)
		{
			acceptor.fstExpr.accept(this);
		    acceptor.sndExpr.accept(this);
			t1 = SemDesc.getActualType(acceptor.fstExpr);
			t2 = SemDesc.getActualType(acceptor.sndExpr);
			if (t1.coercesTo(t2))
			{
				SemDesc.setActualType(acceptor, BOOL);
			}
			else
			{
				RaiseError(acceptor, "Only comparison of equal type elements  is allowed");
			}
		}
		else if (oper == AbsBinExpr.ARRACCESS)
		{
			acceptor.fstExpr.accept(this);
		    acceptor.sndExpr.accept(this);
		    t1 = SemDesc.getActualType(acceptor.fstExpr);
		    t2 = SemDesc.getActualType(acceptor.sndExpr);
		    if (t2.coercesTo(INTEGER) && t1 instanceof SemArrayType)
		    {
		    	SemType destElementType = ((SemArrayType)t1).type;
		    	SemDesc.setActualType(acceptor, destElementType);
		    }
		    else
		    {
		    	if (!t2.coercesTo(INTEGER))
		    	{
		    		RaiseError(acceptor, "ARRAY index must be of type INEGER");
		    	}
		    	if (!(t1 instanceof SemArrayType))
		    	{
		    		RaiseError(acceptor, "Trying to acces an element of a non-ARRAY type");
		    	}
		    }
		}
		else if (oper == AbsBinExpr.RECACCESS)
		{
			acceptor.fstExpr.accept(this);
			t1 = SemDesc.getActualType(acceptor.fstExpr);
			if (t1 instanceof SemRecordType)
			{
				SemRecordType recType = ((SemRecordType)t1);
				SemType finalResult = null;
				String right = ((AbsValName)acceptor.sndExpr).name;
				int idx = 0;
				for(int i = 0; i < recType.getNumFields(); i++)
				{
					if (recType.getFieldName(i).name.equals(right))
					{
						finalResult = recType.getFieldType(i);
						idx = i;
					}
				}
				if (finalResult != null)
				{
					SemDesc.setNameDecl(acceptor.sndExpr, recType.getFieldName(idx));
					SemDesc.setActualType(acceptor, finalResult);
				}
			}
			else
			{
				RaiseError(acceptor,"Not a valid struct");
			}
		}
	}

	@Override
	public void visit(AbsBlockStmt acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.stmts.accept(this);
		
	}

	@Override
	public void visit(AbsCallExpr acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.name.accept(this);
		acceptor.args.accept(this);
		AbsDecl decl = SemDesc.getNameDecl(acceptor.name);
		if (SemDesc.getActualType(decl) instanceof SemSubprogramType)
		{
			SemSubprogramType subProgType = (SemSubprogramType)SemDesc.getActualType(decl);
			AbsValExpr [] expr = acceptor.args.exprs.toArray(new AbsValExpr[acceptor.args.exprs.size()]);
			if (acceptor.args.exprs.size() == subProgType.getNumPars())
			{
				for(int i = 0; i < subProgType.getNumPars(); i++)
				{
					if (!subProgType.getParType(i).coercesTo(SemDesc.getActualType(expr[i])))
					{
						RaiseError(acceptor, String.format("Element %d in function '%s' is not of valid type", i, acceptor.name.name));
					}
				}
				SemDesc.setActualType(acceptor, subProgType.getResultType());
			}
			else
			{
				RaiseError(acceptor, String.format("Invalid number of arguments when calling '%s' (%d provided/%d required)", 
						acceptor.name.name, acceptor.args.exprs.size(), subProgType.getNumPars()));
			}
		}
		else
		{
			RaiseError(acceptor, String.format("Subprogam '%s' does not exsist", acceptor.name.name));
		}
	}

	@Override
	public void visit(AbsConstDecl acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.name.accept(this);
		acceptor.value.accept(this);
		SemType st = SemDesc.getActualType(acceptor.value);
		SemDesc.setActualType(acceptor, st);
		Integer val = SemDesc.getActualConst(acceptor.value);
		if (val != null)
		{
			SemDesc.setActualConst(acceptor, val);
		}

		
		
	}

	@Override
	public void visit(AbsDeclName acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
	}

	@Override
	public void visit(AbsDecls acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		for(AbsDecl decl : acceptor.decls)
		{
			decl.accept(this);
		}
		
	}

	@Override
	public void visit(AbsExprStmt acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.expr.accept(this);
		if (acceptor.expr instanceof AbsCallExpr)
		{
			AbsDecl decl = SemDesc.getNameDecl(((AbsCallExpr)acceptor.expr).name);
			if (decl instanceof AbsFunDecl)
			{
				RaiseError(acceptor, "Functions must be assigned to something");
			}
		}
	}

	@Override
	public void visit(AbsForStmt acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.name.accept(this);
		acceptor.loBound.accept(this);
		acceptor.hiBound.accept(this);
		SemType loType = SemDesc.getActualType(acceptor.loBound);
		SemType hiType = SemDesc.getActualType(acceptor.hiBound);
		if (!SemDesc.getActualType(acceptor.name).coercesTo(INTEGER))
		{
			RaiseError(acceptor, "Iterator variable in FOR loop must be of INTEGER type");
		}

		if (loType.coercesTo(hiType) && loType.coercesTo(INTEGER))
		{
			acceptor.stmt.accept(this);
		}
		else
		{
			RaiseError(acceptor, "FOR loop requires integer boudns");
		}
	}

	@Override
	public void visit(AbsFunDecl acceptor) // RETURN
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		
		acceptor.name.accept(this);
		acceptor.pars.accept(this);
		acceptor.type.accept(this);
		
		SemSubprogramType type;
		
		// return value must be either atomic or a pointer type
		if (SemDesc.getActualType(acceptor.type) instanceof SemAtomType || SemDesc.getActualType(acceptor.type) instanceof SemPointerType) 
		{
		    type = new SemSubprogramType(SemDesc.getActualType(acceptor.type));
		} 
		else 
		{
		    type = new SemSubprogramType(ERROR);
		    String msg = "Return value's type must be either atomic or a pointer";
		    RaiseError(acceptor, msg);
		}
		
		// parameters must be either atomic or pointer types
		for (AbsDecl decl : acceptor.pars.decls) {
		    AbsVarDecl varDecl = (AbsVarDecl) decl;
		    varDecl.accept(this);
		    
		    if (SemDesc.getActualType(varDecl) instanceof SemAtomType || SemDesc.getActualType(varDecl) instanceof SemPointerType) 
		    {
				type.addParType(SemDesc.getActualType(varDecl));
		    } 
		    else 
		    {
				type.addParType(ERROR);
				String msg = "Parameter's type must be either atomic or a pointer";
				RaiseError(acceptor, msg);
		    }
		}
		SemDesc.setActualType(acceptor, type);
		
		acceptor.decls.accept(this);
		
		currentFunDecl = acceptor; // needed for return value assignment
		acceptor.stmt.accept(this);
		currentFunDecl = null;
	    }

	@Override
	public void visit(AbsIfStmt acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.cond.accept(this);
		SemType condType = SemDesc.getActualType(acceptor.cond);
		if (condType.coercesTo(BOOL))
		{
				acceptor.thenStmt.accept(this);
				acceptor.elseStmt.accept(this);
				if (acceptor.IsASpecialOperator)
				{
					SemType t = SemDesc.getActualType(acceptor.thenStmt);
					SemType t2 = SemDesc.getActualType(acceptor.elseStmt);
					if (t.coercesTo(t2))
					{
						System.out.println("HELO");
						SemDesc.setActualType(acceptor, t);
					}
					else
					{
						RaiseError(acceptor,"Invalid use of ternary ?: operator" );
					}
				}
		}
		else
		{
			RaiseError(acceptor, "IF statement condition must be of type BOOL");
		}
	}

	@Override
	public void visit(AbsNilConst acceptor)
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		SemDesc.setActualType(acceptor, new SemPointerType(VOID));
		SemDesc.setActualConst(acceptor, 0);
	}

	@Override
	public void visit(AbsPointerType acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.type.accept(this);
		SemDesc.setActualType(acceptor, new SemPointerType(SemDesc.getActualType(acceptor.type)));
		
	}

	@Override
	public void visit(AbsProcDecl acceptor) 
	{
		
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.name.accept(this);
		acceptor.pars.accept(this);
		SemSubprogramType type = new SemSubprogramType(VOID);
		
		// parameters must be either atomic or pointer types
		for (AbsDecl decl : acceptor.pars.decls) 
		{
		    AbsVarDecl varDecl = (AbsVarDecl) decl;
		    varDecl.accept(this);

		    if (SemDesc.getActualType(varDecl) instanceof SemAtomType || SemDesc.getActualType(varDecl) instanceof SemPointerType) 
		    {
		    	type.addParType(SemDesc.getActualType(varDecl));
		    } 
		    else 
		    {
		    	type.addParType(ERROR);
		    	String msg = "Parameter's type must be either atomic or a pointer";
		    	RaiseError(varDecl, msg);
		    }
		}
		SemDesc.setActualType(acceptor, type);
		acceptor.decls.accept(this);
		acceptor.stmt.accept(this);
	}

	@Override
	public void visit(AbsProgram acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.name.accept(this);
		acceptor.decls.accept(this);
		acceptor.stmt.accept(this);	
	}

	@Override
	public void visit(AbsRecordType acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		SemRecordType rectype = new SemRecordType();
		for(AbsDecl decl : acceptor.fields.decls)
		{
			if (decl instanceof AbsVarDecl)
			{
				AbsVarDecl varDecl = (AbsVarDecl) decl;
				varDecl.accept(this);
				rectype.addField(varDecl.name, SemDesc.getActualType(varDecl));
			}
		}
		SemDesc.setActualType(acceptor, rectype);
	}

	@Override
	public void visit(AbsStmts acceptor)
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		for (AbsStmt stmt : acceptor.stmts)
		    stmt.accept(this);
	}

	@Override
	public void visit(AbsTypeDecl acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.name.accept(this);
	    acceptor.type.accept(this);
	    SemDesc.setActualType(acceptor, SemDesc.getActualType(acceptor.type));
		
	}

	@Override
	public void visit(AbsTypeName acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		AbsDecl decl = SemDesc.getNameDecl(acceptor);
		if (decl != null)
		{
			SemDesc.setActualType(acceptor, SemDesc.getActualType(decl));
		}
		else
		{
			RaiseError(acceptor, "Type name error");
		}
		
	}

	@Override
	public void visit(AbsUnExpr acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.expr.accept(this);
		switch (acceptor.oper)
		{
		case AbsUnExpr.ADD: break;
		case AbsUnExpr.SUB:
			SemType type = SemDesc.getActualType(acceptor.expr);
			if (!type.coercesTo(INTEGER))
			{
				RaiseError(acceptor, "UNARY '-' can only be used on INTEGER types");
			}
			SemDesc.setActualType(acceptor, type);
			Integer val = SemDesc.getActualConst(acceptor.expr);
			if (val != null)
			{
				SemDesc.setActualConst(acceptor, -val);
			}
			break;
		case AbsUnExpr.NOT:
			SemType typeNot = SemDesc.getActualType(acceptor.expr);
			if (!typeNot.coercesTo(BOOL))
			{
				RaiseError(acceptor, "NOT can only be used on BOOL types");
			}
			SemDesc.setActualType(acceptor, typeNot);
			Integer valNot = SemDesc.getActualConst(acceptor.expr);
			if (valNot != null)
			{
				if(valNot == 0)
				{
					SemDesc.setActualConst(acceptor, 1);
				}
				else
				{
					SemDesc.setActualConst(acceptor, 1);
				}
			}
			break;
		case AbsUnExpr.MEM:
			SemType typeMem = SemDesc.getActualType(acceptor.expr);
			if (acceptor.expr instanceof AbsValName 
					&& SemDesc.getNameDecl(acceptor.expr) instanceof AbsVarDecl)
			{
				SemDesc.setActualType(acceptor, new SemPointerType(typeMem));
			}
			else
			{
				RaiseError(acceptor, "Invalid pointer operator usage '^'");
			}
			break;
		case AbsUnExpr.VAL:
			SemType typeVal = SemDesc.getActualType(acceptor.expr);
			if (typeVal instanceof  SemPointerType)
			{
				SemType pointerType = ((SemPointerType) SemDesc.getActualType(acceptor.expr)).type;
				SemDesc.setActualType(acceptor, pointerType);
			}
			else
			{
				RaiseError(acceptor, "Invalid pointer operator usage '^' (tyring to dereference a non pointer");
			}
			break;
		}
		
	}

	@Override
	public void visit(AbsValExprs acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		for (AbsValExpr expr : acceptor.exprs)
		    expr.accept(this);	
	}

	@Override
	public void visit(AbsValName acceptor) 
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		AbsDecl decl = SemDesc.getNameDecl(acceptor);
		if (decl != null)
		{
			SemDesc.setActualType(acceptor, SemDesc.getActualType(decl));
		}
		else
		{
			RaiseError(acceptor, "Value name error");
		}
		if (SemDesc.getActualConst(decl) != null)
		{
			SemDesc.setActualConst(acceptor, SemDesc.getActualConst(decl));
		}
		
	}

	@Override
	public void visit(AbsVarDecl acceptor)
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		if(!acceptor.AutoType)
		{	
			acceptor.name.accept(this);
			acceptor.type.accept(this);
			SemType type = SemDesc.getActualType(acceptor.type);
			SemDesc.setActualType(acceptor, type);
			//SemDesc.setActualType(acceptor, SemDesc.setActualType(node, type))
		}
		else
		{
			//AbsDecl d = (AbsDecl)acceptor.name2;
			
			acceptor.name.accept(this);
			acceptor.name2.accept(this);
			
			acceptor.type = acceptor.name2.type;
			SemType type = SemDesc.getActualType(acceptor.type);
			acceptor.type.accept(this);
			SemDesc.setActualType(acceptor, type);
		}
		
	}

	@Override
	public void visit(AbsWhileStmt acceptor)
	{
		if (acceptor.error) { SemDesc.setActualType(acceptor, ERROR); return; }
		acceptor.cond.accept(this);
		SemType condType = SemDesc.getActualType(acceptor.cond);
		if (condType.coercesTo(BOOL))
		{
			acceptor.stmt.accept(this);
		}
		else
		{
			RaiseError(acceptor, "WHILE loop condition must be a BOOL value");
		}
	}
    
}