package compiler.abstree.tree;

import compiler.abstree.AbsVisitor;

/**
 * Stavek 'if'.
 */
public class AbsIfStmt extends AbsStmt {
	
	/** Pogoj. */
	public AbsValExpr cond;

	/** Stavek ob izpolnjenem pogoju. */
	public AbsStmt thenStmt;
	
	/** Stavek ob neizpolnjenem pogoju. */
	public AbsStmt elseStmt;
	
	public boolean IsASpecialOperator = false;
	
	public AbsIfStmt(AbsValExpr cond, AbsStmt thenStmt, AbsStmt elseStmt) {
		this.cond = cond;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
		this.IsASpecialOperator = false;
	}
	
	public AbsIfStmt(AbsValExpr cond, AbsStmt thenStmt, AbsStmt elseStmt, boolean specialOperator) {
		this.cond = cond;
		this.thenStmt = thenStmt;
		this.elseStmt = elseStmt;
		this.IsASpecialOperator = true;
	}
	
	public void accept(AbsVisitor visitor) {
		visitor.visit(this);
	}

}
