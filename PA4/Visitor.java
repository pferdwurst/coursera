/**
 * /**
 * 
 * @author compilers
 * 
 */
public interface Visitor {

	public void visit(class_c c);

	public void visit(bool_const b);

	public void visit(string_const s);

public void visit(Program p);
public void visit(Expression e);
public void visit(Formal f);

	public void visit(Feature f);
	
	public void visit(method m);

	public void visit(attr a);

	public void visit(formalc f);

	public void visit(branch b);

	public void visit(assign a);

	public void visit(static_dispatch sd);

	public void visit(dispatch d);

	public void visit(cond c);

	public void visit(loop l);

	public void visit(typcase t);

	public void visit(block b);

	public void visit(let l);

	public void visit(plus p);

	public void visit(sub s);

	public void visit(mul m);

	public void visit(divide d);

	public void visit(neg n);

	public void visit(lt lt);

	public void visit(eq eq);

	public void visit(leq leq);

	public void visit(comp c);

	public void visit(int_const i);

	public void visit(new_ n);

	public void visit(isvoid iv);

	public void visit(no_expr ne);

	public void visit(object o);

}
