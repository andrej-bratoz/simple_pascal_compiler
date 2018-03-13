package compiler.frames;

import compiler.abstree.*;
import compiler.abstree.tree.*;
import compiler.report.Report;
import compiler.semanal.*;
import compiler.semanal.type.SemType;
import compiler.semanal.type.SemTypeError;

public class FrmEvaluator extends AbsEmptyVisitor
{
	private final int STATIC_LINK = 4;
	private int ArgSize = 0;
	private boolean procCallDetected = false;
	public final SemType ERROR   = new SemTypeError();
	
    private void RaiseError(AbsTree acceptor, String msg) 
    {
    	Report.warning(msg, acceptor.begLine, acceptor.begColumn, acceptor.endLine, acceptor.endColumn);
    	SemDesc.setActualType(acceptor, ERROR);
    }
	
    private void AddIntristicFunctionByName(String name) 
    {
    	try
    	{
    		AbsFunDecl declaration = (AbsFunDecl) SemTable.fnd(name);
    		FrmDesc.setFrame(declaration, new FrmFrame(declaration, 1));
    	}
    	catch(Exception e)
    	{
    		AbsProcDecl declaration = (AbsProcDecl) SemTable.fnd(name);
    		FrmDesc.setFrame(declaration, new FrmFrame(declaration, 1));
    	}
    	
    }
    
    public FrmEvaluator()
    {
    	AddIntristicFunctionByName("putint");
    	AddIntristicFunctionByName("putch");
    	AddIntristicFunctionByName("getch");
    	AddIntristicFunctionByName("getint");
    	AddIntristicFunctionByName("ord");
    	AddIntristicFunctionByName("chr");
    	AddIntristicFunctionByName("free");
    }
    

	@Override
	public void visit(AbsAlloc acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsArrayType acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.hiBound.accept(this);
		acceptor.loBound.accept(this);
		acceptor.type.accept(this);

	}

	@Override
	public void visit(AbsAssignStmt acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.dstExpr.accept(this);
		acceptor.srcExpr.accept(this);
	}
	
	@Override
	public void visit(AbsAtomConst acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}
	
	@Override
	public void visit(AbsAtomType acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}

	
	@Override
	public void visit(AbsBinExpr acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.fstExpr.accept(this);
		acceptor.sndExpr.accept(this);
		return;
	}

	@Override
	public void visit(AbsBlockStmt acceptor)
	{
		if (acceptor.error) {return;}
		acceptor.stmts.accept(this);
		return;
	}

	@Override
	public void visit(AbsCallExpr acceptor) 
	{
		if (acceptor.error) {return;}
		int argumentSize = 0;
		procCallDetected = true;
		for(AbsValExpr e : acceptor.args.exprs)
		{
			if (SemDesc.getActualType(e) != null)
			{
				argumentSize += SemDesc.getActualType(e).size();
			}
			else
			{
				RaiseError(acceptor, "Type is null");
			}
		}
		if (argumentSize > ArgSize)
			ArgSize = argumentSize;
	}

	@Override
	public void visit(AbsConstDecl acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}
	
	@Override
	public void visit(AbsDeclName acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}

	@Override
	public void visit(AbsDecls acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}

	@Override
	public void visit(AbsExprStmt acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.expr.accept(this);
		return;
	}

	@Override
	public void visit(AbsForStmt acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.loBound.accept(this);
		acceptor.hiBound.accept(this);
		acceptor.stmt.accept(this);
		return;
	}
	
	@Override
	public void visit(AbsFunDecl acceptor) 
	{
		if (acceptor.error) {return;}
		FrmFrame frame = new FrmFrame(acceptor, SemDesc.getScope(acceptor));
		for (AbsDecl decl : acceptor.pars.decls)
		{
			if (decl instanceof AbsVarDecl) 
			{
				AbsVarDecl varDecl = (AbsVarDecl)decl;
				FrmArgAccess access = new FrmArgAccess(varDecl, frame);
				FrmDesc.setAccess(varDecl, access);
			}
			decl.accept(this);
		}
		
		for (AbsDecl decl : acceptor.decls.decls)
		{
			if (decl instanceof AbsVarDecl) 
			{
				AbsVarDecl varDecl = (AbsVarDecl)decl;
				FrmLocAccess access = new FrmLocAccess(varDecl, frame);
				FrmDesc.setAccess(varDecl, access);
			}
			decl.accept(this);
		}
		
		ArgSize = 0;
		procCallDetected = false;
		acceptor.stmt.accept(this);
		frame.sizeArgs = ArgSize;
		if (procCallDetected)
			frame.sizeArgs += STATIC_LINK;
		FrmDesc.setFrame(acceptor, frame);
	}

	@Override
	public void visit(AbsIfStmt acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.cond.accept(this);
		acceptor.thenStmt.accept(this);
		acceptor.elseStmt.accept(this);
		return;
	}

	@Override
	public void visit(AbsNilConst acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}

	@Override
	public void visit(AbsPointerType acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.type.accept(this);
		return;
	}
	
	public void visit(AbsProcDecl acceptor) 
	{
		if (acceptor.error) {return;}
		FrmFrame frame = new FrmFrame(acceptor, SemDesc.getScope(acceptor));
		for (AbsDecl decl : acceptor.pars.decls) 
		{
			if (decl instanceof AbsVarDecl) 
			{
				AbsVarDecl varDecl = (AbsVarDecl)decl;
				FrmArgAccess access = new FrmArgAccess(varDecl, frame);
				FrmDesc.setAccess(varDecl, access);
			}
		}
		for (AbsDecl decl : acceptor.decls.decls)
		{
			if (decl instanceof AbsVarDecl)
			{
				AbsVarDecl varDecl = (AbsVarDecl)decl;
				FrmLocAccess access = new FrmLocAccess(varDecl, frame);
				FrmDesc.setAccess(varDecl, access);
			}
			decl.accept(this);
		}
		ArgSize = 0;
		procCallDetected = false;
		acceptor.stmt.accept(this);
		frame.sizeArgs = ArgSize;
		if (procCallDetected)
			frame.sizeArgs += STATIC_LINK;
		FrmDesc.setFrame(acceptor, frame);
	}
	
	@Override
	public void visit(AbsProgram acceptor)
	{
		if (acceptor.error) {return;}
		FrmFrame frame = new FrmFrame(acceptor, 0);
		for (AbsDecl decl : acceptor.decls.decls) 
		{
			if (decl instanceof AbsVarDecl)
			{
				AbsVarDecl varDecl = (AbsVarDecl)decl;
				FrmVarAccess access = new FrmVarAccess(varDecl);
				FrmDesc.setAccess(varDecl, access);
			}
			decl.accept(this);
		}
		
		ArgSize = 0;
		procCallDetected = false;
		acceptor.stmt.accept(this);
		frame.sizeArgs = ArgSize;
		if (procCallDetected)
		{
			frame.sizeArgs += STATIC_LINK;
		}
		FrmDesc.setFrame(acceptor, frame);
	}

	@Override
	public void visit(AbsRecordType acceptor)
	{
		if (acceptor.error) {return;}
		int offset = 0;
		for (AbsDecl decl : acceptor.fields.decls)
		{
			if (decl instanceof AbsVarDecl)
			{
				AbsVarDecl varDecl = (AbsVarDecl)decl;
				FrmCmpAccess access = new FrmCmpAccess(varDecl, offset);
				FrmDesc.setAccess(varDecl, access);
				offset = offset + SemDesc.getActualType(varDecl.type).size();
			}
		}
	}
	
	@Override
	public void visit(AbsStmts acceptor) 
	{
		if (acceptor.error) {return;}
		for(AbsStmt stm : acceptor.stmts)
		{
			stm.accept(this);
		}
		return;
	}
	
	@Override
	public void visit(AbsTypeDecl acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.type.accept(this);
		return;
	}
	
	@Override
	public void visit(AbsTypeName acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}

	@Override
	public void visit(AbsUnExpr acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsValExprs acceptor) 
	{
		if (acceptor.error) {return;}
		for(AbsValExpr ex : acceptor.exprs)
		{
			ex.accept(this);
		}
	}

	@Override
	public void visit(AbsValName acceptor) 
	{
		if (acceptor.error) {return;}
		return;
	}
	
	@Override
	public void visit(AbsVarDecl acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.type.accept(this);
		return;
	}
	
	@Override
	public void visit(AbsWhileStmt acceptor) 
	{
		if (acceptor.error) {return;}
		acceptor.cond.accept(this);
		acceptor.stmt.accept(this);
	}
}