package compiler.abstree.tree;

import compiler.abstree.AbsVisitor;

/**
 * Deklaracija spremenljivke.
 */
public class AbsVarDecl extends AbsDecl {

	/** Ime spremenljivke. */
	public AbsDeclName name;
	public AbsVarDecl name2;
	
	/** Tip spremenljivke. */
	public AbsTypeExpr type;
	
	public boolean AutoType = false;
	public AbsVarDecl(AbsDeclName name, AbsTypeExpr type) {
		this.name = name;
		this.type = type;
	}
	
	public AbsVarDecl(AbsDeclName name, AbsVarDecl name2, boolean SetType) {
		this.name = name;
		this.name2 = name2;
		this.AutoType = SetType;
	}
	
	
	

	public void accept(AbsVisitor visitor) {
		visitor.visit(this);
	}

}
