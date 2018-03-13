///
/// MY

package compiler.imcode;

import java.util.HashMap;
import java.util.LinkedList;

import compiler.abstree.AbsVisitor;
import compiler.abstree.tree.AbsAlloc;
import compiler.abstree.tree.AbsArrayType;
import compiler.abstree.tree.AbsAssignStmt;
import compiler.abstree.tree.AbsAtomConst;
import compiler.abstree.tree.AbsAtomType;
import compiler.abstree.tree.AbsBinExpr;
import compiler.abstree.tree.AbsBlockStmt;
import compiler.abstree.tree.AbsCallExpr;
import compiler.abstree.tree.AbsConstDecl;
import compiler.abstree.tree.AbsDecl;
import compiler.abstree.tree.AbsDeclName;
import compiler.abstree.tree.AbsDecls;
import compiler.abstree.tree.AbsExprStmt;
import compiler.abstree.tree.AbsForStmt;
import compiler.abstree.tree.AbsFunDecl;
import compiler.abstree.tree.AbsIfStmt;
import compiler.abstree.tree.AbsNilConst;
import compiler.abstree.tree.AbsPointerType;
import compiler.abstree.tree.AbsProcDecl;
import compiler.abstree.tree.AbsProgram;
import compiler.abstree.tree.AbsRecordType;
import compiler.abstree.tree.AbsStmt;
import compiler.abstree.tree.AbsStmts;
import compiler.abstree.tree.AbsTree;
import compiler.abstree.tree.AbsTypeDecl;
import compiler.abstree.tree.AbsTypeName;
import compiler.abstree.tree.AbsUnExpr;
import compiler.abstree.tree.AbsValExpr;
import compiler.abstree.tree.AbsValExprs;
import compiler.abstree.tree.AbsValName;
import compiler.abstree.tree.AbsVarDecl;
import compiler.abstree.tree.AbsWhileStmt;
import compiler.frames.FrmAccess;
import compiler.frames.FrmArgAccess;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.frames.FrmLocAccess;
import compiler.frames.FrmVarAccess;
import compiler.report.Report;
import compiler.semanal.SemDesc;
import compiler.semanal.type.SemArrayType;
import compiler.semanal.type.SemRecordType;
import compiler.semanal.type.SemType;
import compiler.semanal.type.SemTypeError;

public class IMCodeGenerator implements AbsVisitor 
{
	public final SemType ERROR   = new SemTypeError();
    private void RaiseError(AbsTree acceptor, String msg) 
    {
    	Report.warning(msg, acceptor.begLine, acceptor.begColumn, acceptor.endLine, acceptor.endColumn);
    	acceptor.error = true; SemDesc.setActualType(acceptor, ERROR);acceptor.error = true;
    }
    
    private FrmFrame thisFrame = null;
    
    private ImcExpr GetVariableMemoryLocation(AbsValName valName)
    {
    	AbsVarDecl varDecl 		= (AbsVarDecl)SemDesc.getNameDecl(valName);
    	FrmAccess frameAcceess 	= FrmDesc.getAccess(varDecl);
    	ImcExpr returnExpr = null;
    	//global variable
    	if (frameAcceess instanceof FrmVarAccess)
    	{
    		FrmVarAccess acc = (FrmVarAccess)frameAcceess;
    		returnExpr = new ImcNAME(acc.label);
    	}
    	else
    	{
    		int diff = thisFrame.level - SemDesc.getScope(varDecl)+ 1;
    		ImcExpr basePtr = new ImcTEMP(thisFrame.FP);
    		for(int i = 0; i < diff; i++)
    		{
    			basePtr = new ImcMEM(basePtr);
    		}
    		
    		if (frameAcceess instanceof FrmArgAccess)
    		{
    			FrmArgAccess frm = (FrmArgAccess)frameAcceess;
    			returnExpr = new ImcBINOP(ImcBINOP.ADD, basePtr, new ImcCONST(frm.offset));
    		}
    		if (frameAcceess instanceof FrmLocAccess)
    		{
    			FrmLocAccess loc = (FrmLocAccess)frameAcceess;
    			returnExpr = new ImcBINOP(ImcBINOP.ADD, basePtr, new ImcCONST(loc.offset));
    		}
    	}
    	if(returnExpr == null)
    	{
    		RaiseError(valName, "Holy crap! That is bad news!");
    		return null;
    	}
    	return returnExpr;
    }


	public LinkedList<ImcChunk> chunks = new LinkedList<ImcChunk>();
	private HashMap<AbsTree, ImcExpr> exprContainer = new HashMap<AbsTree, ImcExpr>();
	private HashMap<AbsTree, ImcStmt> stmtContainer = new HashMap<AbsTree, ImcStmt>();

	@Override
	public void visit(AbsAlloc acceptor) 
	{
		FrmLabel malloc = FrmLabel.newLabel("malloc");
		SemType type = SemDesc.getActualType(acceptor.type);
		
		ImcCALL call = new ImcCALL(malloc);
		call.args.add(new ImcCONST(0));           // null static link
		call.size.add(4);
		call.args.add(new ImcCONST(type.size())); // type size		
		call.size.add(4);
		exprContainer.put(acceptor, call);
	}

	@Override
	public void visit(AbsArrayType acceptor) 
	{
		//if (acceptor.error) {return;}
	}

	@Override
	public void visit(AbsAssignStmt acceptor) 
	{
		acceptor.dstExpr.accept(this);
        acceptor.srcExpr.accept(this);

        ImcExpr source      = exprContainer.get(acceptor.srcExpr);
        ImcExpr destination = exprContainer.get(acceptor.dstExpr);

        // function return value
        if (SemDesc.getNameDecl(acceptor.dstExpr) instanceof AbsFunDecl &&  acceptor.dstExpr instanceof AbsValName) 
        {
            destination = new ImcTEMP(thisFrame.RV);
        }
        stmtContainer.put(acceptor, new ImcMOVE(destination, source));
		
	}

	@Override
	public void visit(AbsAtomConst acceptor) 
	{
		ImcCONST imConst;
		switch(acceptor.type)
		{
		case AbsAtomConst.INT:
			imConst = new ImcCONST(Integer.parseInt(acceptor.value));
			break;
		case AbsAtomConst.BOOL:
			if(acceptor.value.equals("true"))
			{
				imConst = new ImcCONST(1);
			}
			else
			{
				imConst = new ImcCONST(0);
			}
			break;
		case AbsAtomConst.CHAR:
			imConst = new ImcCONST((int)acceptor.value.charAt(1));
			break;
		default:
			RaiseError(acceptor, "Unknown ATOM CONST type!");
			//throw new Exception("Weird error");
			return;
		}
		exprContainer.put(acceptor, imConst);
				
	}

	@Override
	public void visit(AbsAtomType acceptor) 
	{
		//if (acceptor.error) {return;}
	}

	@Override
	public void visit(AbsBinExpr acceptor) 
	{
		switch(acceptor.oper)
		{
		case AbsBinExpr.ARRACCESS:
			acceptor.fstExpr.accept(this);
			acceptor.sndExpr.accept(this);
			
			SemArrayType arr = (SemArrayType)SemDesc.getActualType(acceptor.fstExpr);
			
			ImcExpr loc = ((ImcMEM)exprContainer.get(acceptor.fstExpr)).expr;
			ImcExpr idx = exprContainer.get(acceptor.sndExpr);
			
			ImcExpr relativeOffset = new ImcBINOP(ImcBINOP.SUB, idx, new ImcCONST(arr.loBound));
			ImcExpr zeroBasedOffset = new ImcBINOP(ImcBINOP.MUL, new ImcCONST(arr.type.size()), relativeOffset);
			ImcExpr absoluteOffset = new ImcBINOP(ImcBINOP.ADD, loc,zeroBasedOffset);
			exprContainer.put(acceptor, new ImcMEM(absoluteOffset));
			break;
		case AbsBinExpr.RECACCESS:
			acceptor.fstExpr.accept(this);
			
			SemRecordType type = (SemRecordType)SemDesc.getActualType(acceptor.fstExpr);
			int offset = 0;
			for(int i = 0; i < type.getNumFields(); i++)
			{
				if (((AbsValName)acceptor.sndExpr).name.equals(type.getFieldName(i).name))
				{
					 ImcExpr recordLocation = ((ImcMEM) exprContainer.get(acceptor.fstExpr)).expr;
					 ImcExpr fieldAddr = new ImcBINOP(ImcBINOP.ADD, recordLocation, new ImcCONST(offset));
					 exprContainer.put(acceptor, new ImcMEM(fieldAddr));
					 break;
				}
				offset += type.getFieldType(i).size();
			}
			break;
		default:
			acceptor.fstExpr.accept(this);
			acceptor.sndExpr.accept(this);
			exprContainer.put(acceptor, new ImcBINOP(acceptor.oper, exprContainer.get(acceptor.fstExpr), exprContainer.get(acceptor.sndExpr)));
			break;
		}
		
	}

	@Override
	public void visit(AbsBlockStmt acceptor) 
	{
		acceptor.stmts.accept(this);
		stmtContainer.put(acceptor, stmtContainer.get(acceptor.stmts));
		
	}

	@Override
	public void visit(AbsCallExpr acceptor) 
	{
		FrmFrame frame 		= FrmDesc.getFrame(SemDesc.getNameDecl(acceptor.name));
		ImcCALL callExpr	= new ImcCALL(frame.label);
		ImcExpr sl = new ImcCONST(0);
		
		if (frame.level > 1)
		{
			sl = new ImcTEMP(thisFrame.FP);
			int diff = thisFrame.level - frame.level;
			for(int i = 0; i <= diff; i++) // TODO is it 1?
			{
				sl = new ImcMEM(sl);
				
			}
		}
		callExpr.args.add(sl);
		callExpr.size.add(4);
		
		for(AbsValExpr expr : acceptor.args.exprs)
		{
			expr.accept(this);
			callExpr.args.add(exprContainer.get(expr));
			callExpr.size.add(4);
		}
		exprContainer.put(acceptor, callExpr);
	}

	@Override
	public void visit(AbsConstDecl acceptor) 
	{
		acceptor.value.accept(this);
		//AbsValExpr ex = acceptor.value;
		exprContainer.put(acceptor,exprContainer.get(acceptor.value));	
	}

	@Override
	public void visit(AbsDeclName acceptor) 
	{
		//if (acceptor.error) {return;}		
	}

	@Override
	public void visit(AbsDecls acceptor) 
	{
		//if (acceptor.error) {return;}
		for(AbsDecl decl : acceptor.decls)
		{
			if (decl instanceof AbsFunDecl || decl instanceof AbsProcDecl)
			{
				decl.accept(this);
			}
		}
	}

	@Override
	public void visit(AbsExprStmt acceptor) 
	{
		//if (acceptor.error) {return;}
		acceptor.expr.accept(this);
		ImcExpr expr = exprContainer.get(acceptor.expr);
		stmtContainer.put(acceptor, new ImcEXP(expr));
	}

	@Override
	public void visit(AbsForStmt acceptor) 
	{
		//if (acceptor.error) {return;}
		acceptor.name.accept(this);
		acceptor.loBound.accept(this);
		acceptor.hiBound.accept(this);
		acceptor.stmt.accept(this);
		
		ImcExpr nameExpression    = exprContainer.get(acceptor.name);
		ImcExpr loBoundExpression = exprContainer.get(acceptor.loBound);
		ImcExpr hiBoundExpression = exprContainer.get(acceptor.hiBound);
		ImcStmt statement         = stmtContainer.get(acceptor.stmt);
		
		ImcLABEL label1 = new ImcLABEL(FrmLabel.newLabel()); // first condition check label
		ImcLABEL label2 = new ImcLABEL(FrmLabel.newLabel()); // loop body label
		ImcLABEL label3 = new ImcLABEL(FrmLabel.newLabel()); // second condition check label
		ImcLABEL label4 = new ImcLABEL(FrmLabel.newLabel()); // exit label
		
		ImcMOVE setIndex       = new ImcMOVE(nameExpression, loBoundExpression);
		ImcExpr condition      = new ImcBINOP(ImcBINOP.LEQ, nameExpression, hiBoundExpression);
		ImcExpr condition2     = new ImcBINOP(ImcBINOP.LTH, nameExpression, hiBoundExpression);
		ImcMOVE incrementIndex = new ImcMOVE(nameExpression, new ImcBINOP(ImcBINOP.ADD, nameExpression, new ImcCONST(1)));
		
		ImcCJUMP cjump  = new ImcCJUMP(condition, label2.label, label4.label);
		ImcCJUMP cjump2 = new ImcCJUMP(condition2, label3.label, label4.label);
		ImcJUMP  jump   = new ImcJUMP(label1.label);
		
		ImcSEQ seq = new ImcSEQ();
		seq.stmts.add(setIndex);
		seq.stmts.add(label1);
		seq.stmts.add(cjump);
		seq.stmts.add(label2);
		seq.stmts.add(statement);
		seq.stmts.add(cjump2);
		seq.stmts.add(label3);
		seq.stmts.add(incrementIndex);
		seq.stmts.add(jump);
		seq.stmts.add(label4);
		
		stmtContainer.put(acceptor, seq);
		
	}

	@Override
	public void visit(AbsFunDecl acceptor) 
	{	
		acceptor.decls.accept(this);
		thisFrame = FrmDesc.getFrame(acceptor);
		acceptor.stmt.accept(this);
		ImcStmt body = stmtContainer.get(acceptor.stmt);
		ImcTEMP retValue = new ImcTEMP(thisFrame.RV);
		ImcMOVE setRetVal = new ImcMOVE(retValue, new ImcESEQ(body, retValue));
		chunks.add(new ImcCodeChunk(thisFrame, setRetVal));
	}

	@Override
	public void visit(AbsIfStmt acceptor) 
	{
		//if (acceptor.error) {return;}
		acceptor.cond.accept(this);
		acceptor.thenStmt.accept(this);
		acceptor.elseStmt.accept(this);
		
		ImcLABEL L_then = new ImcLABEL(FrmLabel.newLabel());
		ImcLABEL L_else = new ImcLABEL(FrmLabel.newLabel());
		ImcLABEL L_exit = new ImcLABEL(FrmLabel.newLabel());
		
		ImcExpr cond = exprContainer.get(acceptor.cond);
		ImcStmt then = stmtContainer.get(acceptor.thenStmt);
		ImcStmt els_ = stmtContainer.get(acceptor.elseStmt);
		
		ImcCJUMP cJump = new ImcCJUMP(cond, L_then.label, L_else.label);
		ImcJUMP exitJ = new ImcJUMP(L_exit.label);
		
		ImcSEQ seq = new ImcSEQ();
		seq.stmts.add(cJump);
		seq.stmts.add(L_then);
		seq.stmts.add(then);
		seq.stmts.add(exitJ);
		seq.stmts.add(L_else);
		seq.stmts.add(els_);
		seq.stmts.add(L_exit);
		
		stmtContainer.put(acceptor, seq);
	}

	@Override
	public void visit(AbsNilConst acceptor)
	{
		//if (acceptor.error) {return;}
		exprContainer.put(acceptor, new ImcCONST(0));
	}

	@Override
	public void visit(AbsPointerType acceptor) 
	{
		//if (acceptor.error) {return;}		
	}

	@Override
	public void visit(AbsProcDecl acceptor) 
	{
		//if (acceptor.error) {return;}
		acceptor.decls.accept(this);
		thisFrame = FrmDesc.getFrame(acceptor);
		acceptor.stmt.accept(this);
		ImcStmt body = stmtContainer.get(acceptor.stmt);
		chunks.add(new ImcCodeChunk(thisFrame, body));		
	}

	@Override
	public void visit(AbsProgram acceptor) 
	{
		 // variables declared here are all global, in their own fragments
        for (AbsDecl decl : acceptor.decls.decls)
        {
            if (decl instanceof AbsVarDecl)
            {
                AbsVarDecl varDecl = (AbsVarDecl) decl;

                FrmVarAccess access = (FrmVarAccess) FrmDesc.getAccess(varDecl);
                SemType type = SemDesc.getActualType(decl);

                chunks.add(new ImcDataChunk(access.label, type.size()));
            }

            decl.accept(this);
        }

        // main program code
        thisFrame = FrmDesc.getFrame(acceptor);
        acceptor.stmt.accept(this);

        chunks.add(new ImcCodeChunk(thisFrame, stmtContainer.get(acceptor.stmt)));
		
	}

	@Override
	public void visit(AbsRecordType acceptor) 
	{
		
	}

	@Override
	public void visit(AbsStmts acceptor) 
	{
		//if (acceptor.error) {return;}
		ImcSEQ seq = new ImcSEQ();
		for(AbsStmt s : acceptor.stmts)
		{
			s.accept(this);
			seq.stmts.add(stmtContainer.get(s));
		}
		stmtContainer.put(acceptor, seq);
	}

	@Override
	public void visit(AbsTypeDecl acceptor) 
	{
		//if (acceptor.error) {return;}
		//acceptor.type.accept(this);
		//System.out.println("TYPE: "  + FrmDesc.getFrame(acceptor.type));
		//exprContainer.put(acceptor, exprContainer.get(acceptor.type));
	}

	@Override
	public void visit(AbsTypeName acceptor) 
	{
		//if (acceptor.error) {return;}
		
	}

	@Override
	public void visit(AbsUnExpr acceptor) 
	{
		//if (acceptor.error) {return;}
		acceptor.expr.accept(this);
		switch(acceptor.oper)
		{
		case AbsUnExpr.ADD:
			exprContainer.put(acceptor, exprContainer.get(acceptor.expr));
			break;
		case AbsUnExpr.SUB:
			exprContainer.put(acceptor, new ImcBINOP(ImcBINOP.SUB, new ImcCONST(0), exprContainer.get(acceptor.expr)));
			break;
		case AbsUnExpr.VAL:
			exprContainer.put(acceptor, new ImcMEM(exprContainer.get(acceptor.expr)));
			break;
		case AbsUnExpr.NOT:
			exprContainer.put(acceptor, new ImcBINOP(ImcBINOP.SUB, new ImcCONST(1), exprContainer.get(acceptor.expr)));
		case AbsUnExpr.MEM:
			ImcExpr expr = GetVariableMemoryLocation((AbsValName)acceptor.expr);
			exprContainer.put(acceptor, expr);
			break;
		default:
			RaiseError(acceptor,"Wierd UnaryExpr error!");
		}
	}

	@Override
	public void visit(AbsValExprs acceptor)
	{
		//if (acceptor.error) {return;}
		
	}

	@Override
	public void visit(AbsValName acceptor)
	{
		//if (acceptor.error) {return;}
		AbsDecl decl = SemDesc.getNameDecl(acceptor);
		if(decl instanceof AbsConstDecl)
		{
			exprContainer.put(acceptor,exprContainer.get(decl));
		}
		else if (decl instanceof AbsVarDecl)
		{
			ImcExpr loc = GetVariableMemoryLocation(acceptor);
			exprContainer.put(acceptor, new ImcMEM(loc));
		}
		
	}

	@Override
	public void visit(AbsVarDecl acceptor) 
	{
		//if (acceptor.error) {return;}		
	}

	@Override
	public void visit(AbsWhileStmt acceptor) 
	{
		//if (acceptor.error) {return;}
		acceptor.cond.accept(this);
		acceptor.stmt.accept(this);
	
		ImcExpr cond = exprContainer.get(acceptor.cond);
		ImcStmt stmt = stmtContainer.get(acceptor.stmt);
		
		ImcLABEL L_condCheck = new ImcLABEL(FrmLabel.newLabel());
		ImcLABEL L_true = new ImcLABEL(FrmLabel.newLabel());
		ImcLABEL L_exit = new ImcLABEL(FrmLabel.newLabel());
		
		ImcCJUMP cJump = new ImcCJUMP(cond, L_true.label, L_exit.label);
		ImcJUMP  jump  = new ImcJUMP(L_condCheck.label);
		
		ImcSEQ seq = new ImcSEQ();
		seq.stmts.add(L_condCheck);
		seq.stmts.add(cJump);
		seq.stmts.add(L_true);
		seq.stmts.add(stmt);
		seq.stmts.add(jump);
		seq.stmts.add(L_exit);
		
		stmtContainer.put(acceptor, seq);
	}
	

}
