import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;

/**
 * 
 */

/**
 * @author compilers
 * 
 */
public class ASTVisitor implements Visitor {

	static final String[] keywords = { "case", "if" };

	private ClassTable classTable;
	private SymbolTable symbolTable;
	private PrintStream errorOut;
	private boolean typeCheck = false;

	/**
	 * 
	 */
	public ASTVisitor(ClassTable ct, boolean isTypeCheckingPass) {
		classTable = ct;
		symbolTable = new SymbolTable();
		symbolTable.enterScope();
		typeCheck = isTypeCheckingPass;
	}

	private AbstractSymbol lookup(AbstractSymbol key) {
		if (key.equalString("self", 4)) {
			return TreeConstants.SELF_TYPE;
		}
		AbstractSymbol val = null;

		val = (AbstractSymbol) symbolTable.probe(key);

		if (val == null) {
			val = (AbstractSymbol) symbolTable.lookup(key);
		}
		if (val == null) {
			System.err.println("Symbol " + key + " not found!");
			val = TreeConstants.No_type;
		}
		return val;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Visitor#accept(Class_)
	 */
	@Override
	public void visit(class_c c) {

		// Check that it doesn't inherit Int, Bool or String?
		if (c.parent == TreeConstants.Bool || c.parent == TreeConstants.Str
				|| c.parent == TreeConstants.Int) {

			errorOut = classTable.semantError(c);
			errorOut.println("Class " + c.name + " cannot inherit class "
					+ c.parent + ".");
		}
		symbolTable.addId(c.name, c.name);

		for (Enumeration e = c.features.getElements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			((TreeNode) o).accept(this);
		}
	}

	@Override
	public void visit(method m) {

		for (Enumeration e = m.formals.getElements(); e.hasMoreElements();) {
			((TreeNode) e.nextElement()).accept(this);
		}
		symbolTable.addId(m.name, m.return_type);

		m.expr.accept(this);

		if (false) {
			if (lub(m.expr.get_type(), m.return_type) != m.return_type) {
				classTable.error(m, "Inferred return type " + m.expr.get_type()
						+ " of method " + m.name
						+ " does not conform to declared return type "
						+ m.return_type + ".");
			}
		}
	}

	@Override
	public void visit(attr a) {
		if (a.name.equalString("self", 4)) {
			classTable.error(a, "Cannot assign to self.");
		}
		a.init.accept(this);

		symbolTable.addId(a.name, a.type_decl);
	}

	@Override
	public void visit(formalc f) {
		symbolTable.addId(f.name, f.type_decl);
	}

	@Override
	public void visit(branch b) {
		symbolTable.enterScope();

		symbolTable.addId(b.name, b.type_decl);
		b.expr.accept(this);

		symbolTable.exitScope();

	}

	@Override
	public void visit(typcase t) {
		t.expr.accept(this);

		AbstractSymbol case_type = TreeConstants.No_type;

		for (Enumeration e = t.cases.getElements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			((TreeNode) o).accept(this);
			case_type = lub(case_type, ((branch) o).type_decl);
		}
		t.set_type(case_type);
	}

	@Override
	public void visit(assign a) {
		((TreeNode) a.expr).accept(this);

		a.set_type(lookup(a.name));
	}

	/**
	 * expr[@TYPE].ID(expr[[,expr]]*)
	 */
	@Override
	public void visit(static_dispatch sd) {

		for (@SuppressWarnings("rawtypes")
		Enumeration e = sd.actual.getElements(); e.hasMoreElements();) {
			((TreeNode) e.nextElement()).accept(this);
		}
		((TreeNode) sd.expr).accept(this);
		sd.expr.set_type(sd.expr.get_type());

		AbstractSymbol id_type = sd.expr.get_type();
		AbstractSymbol dispatch_method_type = lookup(sd.name);

		if (dispatch_method_type == TreeConstants.SELF_TYPE) {
			sd.set_type(id_type);
		} else {
			sd.set_type(dispatch_method_type);
		}

	}

	@Override
	public void visit(dispatch d) {
		/*
		 * e0.f(e1...en)
		 * 
		 * e0 -> d.expr f -> d.name e1..en -> d.actual
		 */

		for (@SuppressWarnings("rawtypes")
		Enumeration e = d.actual.getElements(); e.hasMoreElements();) {
			((TreeNode) e.nextElement()).accept(this);
		}

		// Visit e0
		((TreeNode) d.expr).accept(this);

		/*
		 * Type of dispatch is the value of the 'expr' If the 'expr' is of type
		 * SELF_TYPE then the dispatch type is the type of the id receiving the
		 * dispatch. Otherwise it's the type of 'expr'
		 */
		AbstractSymbol id_type = d.expr.get_type();
		AbstractSymbol dispatch_method_type = lookup(d.name);

		if (dispatch_method_type == TreeConstants.SELF_TYPE) {
			d.set_type(id_type);
		} else {
			d.set_type(dispatch_method_type);
		}

	}

	/**
	 * Find the least common ancestor of 2 objects. If the A inherits from B
	 * then B can be the LCA of the pair
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private AbstractSymbol lub(AbstractSymbol t1, AbstractSymbol t2) {
		AbstractSymbol lub = TreeConstants.Object_;

		try {

			Deque<AbstractSymbol> t1Stack = new ArrayDeque<AbstractSymbol>();

			// find ancestor path of t1
			class_c t1_class = classTable.getClassFor(t1);

			t1Stack.push(t1);

			while (t1Stack.peek() != TreeConstants.Object_) {
				t1Stack.push(t1_class.getParent());
				t1_class = classTable.getClassFor(t1_class.getParent());
			}

			Deque<AbstractSymbol> t2Stack = new ArrayDeque<AbstractSymbol>();
			t2Stack.push(t2);
			class_c t2_class = classTable.getClassFor(t2);

			/*
			 * while first element in deque not equal to
			 */
			while (t2Stack.peek() != TreeConstants.Object_) {
				t2Stack.push(t2_class.getParent());
				t2_class = classTable.getClassFor(t2_class.getParent());
			}

			while (t1Stack.peek() == t2Stack.peek() && !t1Stack.isEmpty()
					&& !t2Stack.isEmpty()) {
				lub = t1Stack.peek();
				t1Stack.pop();
				t2Stack.pop();
			}

		} catch (Exception e) {
			// swallow error. Yikes!
		}
		return lub;
	}

	@Override
	public void visit(cond c) {
		c.pred.accept(this);
		c.pred.set_type(TreeConstants.Bool);
		c.else_exp.accept(this);
		c.then_exp.accept(this);
		c.set_type(lub(c.else_exp.get_type(), c.then_exp.get_type()));
	}

	@Override
	public void visit(loop l) {

		l.pred.accept(this);
		l.body.accept(this);
		l.set_type(l.body.get_type());
	}

	@Override
	public void visit(block b) {

		AbstractSymbol blockName = new StringSymbol("BLOCK"
				+ String.format("%03d", b.lineNumber), 8, 8);
		symbolTable.enterScope();

		for (Enumeration e = b.body.getElements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			((TreeNode) o).accept(this);

			symbolTable.addId(blockName, ((Expression) o).get_type());
		}

		b.set_type((AbstractSymbol) symbolTable.probe(blockName));
		symbolTable.exitScope();

	}

	@Override
	public void visit(let l) {

		AbstractSymbol blockName = new StringSymbol("LET"
				+ String.format("%03d", l.lineNumber), 6, 8);
		symbolTable.enterScope();

		if (l.identifier.equalString("self", 4)) {
			classTable.error(l, "'self' cannot be bound in a 'let' expression");
		}

		symbolTable.addId(l.identifier, l.type_decl);

		// Verify init if exists
		if (!(l.init instanceof no_expr)) {
			l.init.accept(this);

			if (typeCheck) {
				if (lub(l.init.get_type(), l.type_decl) != l.type_decl) {
					classTable
							.error(l,
									"Inferred type "
											+ l.init.get_type()
											+ " of initialization of "
											+ l.identifier
											+ " does not conform to identifier's declared type "
											+ l.type_decl);
				}
			}
		}
		l.body.accept(this);

		l.set_type(l.body.get_type());
		symbolTable.exitScope();
	}

	@Override
	public void visit(plus p) {
		p.e1.accept(this);
		p.e2.accept(this);

		if (p.e1.get_type() != TreeConstants.Int
				|| p.e2.get_type() != TreeConstants.Int) {

			classTable.error(p, "non-Int arguments: " + p.e1.get_type() + " + "
					+ p.e2.get_type());
		}

		p.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(sub s) {
		s.e1.accept(this);
		s.e2.accept(this);

		if (s.e1.get_type() != TreeConstants.Int
				|| s.e2.get_type() != TreeConstants.Int) {

			classTable.error(s, "non-Int arguments: " + s.e1.get_type() + " - "
					+ s.e2.get_type());
		}
		s.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(mul m) {
		m.e1.accept(this);
		m.e2.accept(this);

		if (m.e1.get_type() != TreeConstants.Int
				|| m.e2.get_type() != TreeConstants.Int) {

			classTable.error(m, "non-Int arguments: " + m.e1.get_type() + " * "
					+ m.e2.get_type());
		}
		m.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(divide d) {
		d.e1.accept(this);
		d.e2.accept(this);

		if (d.e1.get_type() != TreeConstants.Int
				|| d.e2.get_type() != TreeConstants.Int) {

			classTable.error(d, "non-Int arguments: " + d.e1.get_type() + " / "
					+ d.e2.get_type());
		}

		d.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(neg n) {
		n.e1.accept(this);
		n.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(lt lt) {
		lt.e1.accept(this);
		lt.e2.accept(this);
		if (lt.e1.get_type() != TreeConstants.Int
				|| lt.e2.get_type() != TreeConstants.Int) {

			classTable.error(lt, "non-Int arguments: " + lt.e1.get_type()
					+ " < " + lt.e2.get_type());
		}
		lt.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(eq eq) {
		eq.e1.accept(this);

		eq.e2.accept(this);

		eq.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(leq leq) {
		leq.e1.accept(this);
		leq.e2.accept(this);
		if (leq.e1.get_type() != TreeConstants.Int
				|| leq.e2.get_type() != TreeConstants.Int) {
			errorOut.println("non-Int arguments:" + leq.e1.get_type() + " <= "
					+ leq.e2.get_type());
		}
		leq.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(comp c) {
		c.e1.accept(this);

		c.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(int_const i) {
		i.set_type(TreeConstants.Int);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Visitor#accept(bool_const)
	 */
	@Override
	public void visit(bool_const b) {
		// TODO Auto-generated method stub
		b.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(string_const s) {
		s.set_type(TreeConstants.Str);
	}

	@Override
	public void visit(new_ n) {
		n.set_type(n.type_name);
	}

	@Override
	public void visit(isvoid iv) {
		iv.e1.accept(this);
		iv.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(no_expr ne) {
		/*
		 * Is this void?
		 */
	}

	@Override
	public void visit(object o) {

		o.set_type((AbstractSymbol) lookup(o.name));
	}

	@Override
	public void visit(Program p) {

		System.out.println("visiting a Program");
	}

	@Override
	public void visit(Expression e) {
		System.out.println("visiting an expression");

	}

	@Override
	public void visit(Formal f) {
		System.out.println("visiting a formal");

	}

	@Override
	public void visit(Feature f) {
		System.out.println("visiting a feature");

	}

	public void typeCheck() {
		this.typeCheck = true;
	}

}
