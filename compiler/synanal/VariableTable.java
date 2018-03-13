package compiler.synanal;

import java.util.HashMap;
import java.util.*;
import compiler.abstree.tree.AbsVarDecl;
public class VariableTable 
{
	public static Map<String, AbsVarDecl> _vars_ = new HashMap<String, AbsVarDecl>();
	public static void PutVariable(String s, AbsVarDecl d)
	{
		_vars_.put(s, d);
	}
	
	public static AbsVarDecl GetVar(String id)
	{
		return _vars_.get(id);
	}
}
