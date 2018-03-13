package compiler.semanal;

import compiler.abstree.AbsVisitor;
import compiler.abstree.tree.*;
import compiler.report.Report;
import compiler.semanal.type.SemAtomType;
import compiler.semanal.type.SemRecordType;
import compiler.semanal.type.SemType;
import compiler.semanal.type.SemTypeError;

public class SemNameResolver implements AbsVisitor 
{
    public boolean error = false;

	public final SemType BOOL    = new SemAtomType(SemAtomType.BOOL);
    public final SemType CHAR    = new SemAtomType(SemAtomType.CHAR);
    public final SemType INTEGER = new SemAtomType(SemAtomType.INT);
    public final SemType VOID    = new SemAtomType(SemAtomType.VOID);
    private void RaiseError(AbsTree acceptor, String msg)
    {
    	error = true;
    	Report.warning(msg, acceptor.begLine, acceptor.begColumn, acceptor.endLine, acceptor.endColumn);
    	SemDesc.setActualType(acceptor, new SemTypeError());
    	acceptor.error = true;
    }
    
    private void AddIntristicFunctionByName(String name)
    {
    	try 
    	{
			SemTable.ins(name, new AbsProcDecl(new AbsDeclName(name), null, null, null));
		} 
    	catch (SemIllegalInsertException e) 
		{
			System.out.println("Internal compiler error: Unable to se up intristic procesures");
			//e.printStackTrace();
		}
    }
    
    public SemNameResolver()
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
		if (acceptor.error) return;
		acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsArrayType acceptor) 
    {
    	if (acceptor.error) return;
	
		acceptor.type.accept(this);
		acceptor.loBound.accept(this);
		acceptor.hiBound.accept(this);
    }

    @Override
    public void visit(AbsAssignStmt acceptor) 
    {
    	if (acceptor.error) return;
    	if(acceptor.SpecialAuto)
    	{
	    	SemDesc.setNameDecl(acceptor.dstExpr, acceptor.SpecialVarDecl);
	    	SemDesc.setActualType(acceptor.dstExpr, SemDesc.getActualType(acceptor.srcExpr));
    	}
    	acceptor.dstExpr.accept(this);
    	acceptor.srcExpr.accept(this);
    	
    }

    @Override
    public void visit(AbsAtomConst acceptor) 
    {
    	if (acceptor.error) return;
    }

    @Override
    public void visit(AbsAtomType acceptor) 
    {
    	if (acceptor.error) return;
    }

    @Override
    public void visit(AbsBinExpr acceptor) 
    {
		if (acceptor.error) return;
		
		if (acceptor.oper == AbsBinExpr.RECACCESS) 
		{
		    acceptor.fstExpr.accept(this);
		} 
		else
		{
		    acceptor.fstExpr.accept(this);
		    acceptor.sndExpr.accept(this);
		}
    }

    @Override
    public void visit(AbsBlockStmt acceptor) 
    {
		if (acceptor.error) return;	
		acceptor.stmts.accept(this);
    }

    @Override
    public void visit(AbsCallExpr acceptor) 
    {
		if (acceptor.error) return;
		
		acceptor.name.accept(this);
		acceptor.args.accept(this);
    }

    @Override
    public void visit(AbsConstDecl acceptor) 
    {
		if (acceptor.error) return;
		try 
		{
		    SemTable.ins(acceptor.name.name, acceptor);
		} 
		catch (SemIllegalInsertException _) 
		{
		   RaiseError(acceptor,  String.format("Already declared in this scope: '%s'", acceptor.name.name));
		}
		acceptor.value.accept(this);	
    }

    @Override
    public void visit(AbsDeclName acceptor) 
    {
    	if (acceptor.error) return;
    }

    @Override
    public void visit(AbsDecls acceptor) 
    {
		if (acceptor.error) return;
		
		for (AbsDecl decl : acceptor.decls)
		    decl.accept(this);
    }

    @Override
    public void visit(AbsExprStmt acceptor)
    {
		if (acceptor.error) return;
		
		acceptor.expr.accept(this);
    }

    @Override
    public void visit(AbsForStmt acceptor)
    {
		if (acceptor.error) return;
		
		SemTable.newScope();
		acceptor.name.accept(this);
		acceptor.loBound.accept(this);
		acceptor.hiBound.accept(this);
		acceptor.stmt.accept(this);
		SemTable.oldScope();
    }

    @Override
    public void visit(AbsFunDecl acceptor) {
		if (acceptor.error) return;
		
		try 
		{
		    SemTable.ins(acceptor.name.name, acceptor);
		} 
		catch (SemIllegalInsertException _) 
		{
		    RaiseError(acceptor, String.format("Already declared in this scope: '%s'", acceptor.name.name));
		}
	
		SemTable.newScope();
		try { SemTable.ins(acceptor.name.name, acceptor); } catch (SemIllegalInsertException _) {}
		acceptor.pars.accept(this);
		acceptor.type.accept(this);
		acceptor.decls.accept(this);
		acceptor.stmt.accept(this);
		SemTable.oldScope();
		
		SemDesc.setScope(acceptor, SemDesc.getScope(acceptor) - 1);
    }

    @Override
    public void visit(AbsIfStmt acceptor) 
    {
		if (acceptor.error) return;
		
		acceptor.cond.accept(this);
	
		SemTable.newScope();
		acceptor.thenStmt.accept(this);
		SemTable.oldScope();
	
		SemTable.newScope();
		acceptor.elseStmt.accept(this);
		SemTable.oldScope();
    }

    @Override
    public void visit(AbsNilConst acceptor) 
    {
    	if (acceptor.error) return;
    }

    @Override
    public void visit(AbsPointerType acceptor) 
    {
		if (acceptor.error) return;
		
		acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsProcDecl acceptor)
    {
		if (acceptor.error) return;
		
		try 
		{
		    SemTable.ins(acceptor.name.name, acceptor);
		} 
		catch (SemIllegalInsertException _)
		{
		    RaiseError(acceptor, String.format("Already declared in this scope: '%s'", acceptor.name.name));
		}
	
		SemTable.newScope();
		try { SemTable.ins(acceptor.name.name, acceptor); } catch (SemIllegalInsertException _) {}
		acceptor.pars.accept(this);
		acceptor.decls.accept(this);
		acceptor.stmt.accept(this);
		SemTable.oldScope();
		
		SemDesc.setScope(acceptor, SemDesc.getScope(acceptor) - 1);
    }

    @Override
    public void visit(AbsProgram acceptor) 
    {
		if (acceptor.error) return;
		
		SemTable.newScope();
		acceptor.decls.accept(this);
		acceptor.stmt.accept(this);
		SemTable.oldScope();
    }

    @Override
    public void visit(AbsRecordType acceptor) 
    {
		if (acceptor.error) return;
		
		for (AbsDecl decl : acceptor.fields.decls) 
		{
		    AbsVarDecl field = (AbsVarDecl) decl;
		    field.type.accept(this);
		}
    }

    @Override
    public void visit(AbsStmts acceptor) 
    {
    	if (acceptor.error) return;
		for (AbsStmt stmt : acceptor.stmts)
		    stmt.accept(this);
    }

    @Override
    public void visit(AbsTypeDecl acceptor) 
    {
    	if (acceptor.error) return;
	
		try 
		{
		    SemTable.ins(acceptor.name.name, acceptor);
		} 
		catch (SemIllegalInsertException _) 
		{
		    RaiseError(acceptor, String.format("Already declared in this scope: '%s'", acceptor.name.name));
		}
	
		acceptor.type.accept(this);
    }

    @Override
    public void visit(AbsTypeName acceptor) 
    {
		if (acceptor.error) return;
		
		AbsDecl declaration = SemTable.fnd(acceptor.name);
		if (declaration instanceof AbsTypeDecl) 
		{
		    SemDesc.setNameDecl(acceptor, declaration);
		} 
		else 
		{
		    RaiseError(acceptor, String.format("Undeclared type name: '%s'", acceptor.name));
		}
    }

    @Override
    public void visit(AbsUnExpr acceptor) 
    {
		if (acceptor.error) return;
		acceptor.expr.accept(this);
    }

    @Override
    public void visit(AbsValExprs acceptor) 
    {
		if (acceptor.error) return;
		for (AbsValExpr expr : acceptor.exprs) 
			expr.accept(this);
    }

    @Override
    public void visit(AbsValName acceptor) 
    {
	    if (acceptor.error) return;
	    AbsDecl declaration = SemTable.fnd(acceptor.name);
	    if (declaration != null && !(declaration instanceof AbsTypeDecl)) 
	    {
	        SemDesc.setNameDecl(acceptor, declaration);
	        Integer var = SemDesc.getActualConst(declaration);
	        if (var != null)
	        {
	        	SemDesc.setActualConst(acceptor, var);
	        }
	    } 
	    else 
	    {
	        RaiseError(acceptor,String.format("Undeclared value name: '%s'", acceptor.name));
	    }
    }

    @Override
    public void visit(AbsVarDecl acceptor) 
    {
		if (acceptor.error) return;
		
		try 
		{
		    SemTable.ins(acceptor.name.name, acceptor);
		} 
		catch (SemIllegalInsertException _) 
		{
		    RaiseError(acceptor, String.format("Already declared in this scope: '%s'", acceptor.name.name));
		}
		
		
		if(!acceptor.AutoType)
		{
			acceptor.type.accept(this);
		}
		else
		{
			//acceptor.name2.accept(this);
			//SemDesc.setActualType(acceptor.name, SemDesc.getActualType(acceptor.name2));
		}
    }

    @Override
    public void visit(AbsWhileStmt acceptor) 
    {
		if (acceptor.error) return;
		
		acceptor.cond.accept(this);
		SemTable.newScope();
		acceptor.stmt.accept(this);
		SemTable.oldScope();
    }

}