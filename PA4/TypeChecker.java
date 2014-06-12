import java.io.PrintStream;
import java.util.Enumeration;
import java.util.List;

/**
 * 
 */

/**
 * @author compilers
 * 
 */
public class TypeChecker implements Visitor {

	static final String[] keywords = { "case", "if" };

	private ClassTable classTable;
	private SymbolTable symbolTable;
	private PrintStream errorOut;

	/**
	 * 
	 */
	public TypeChecker(ClassTable ct) {
		classTable = ct;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Visitor#accept(Class_)
	 */
	@Override
	public void visit(class_c c) {

		symbolTable = c.getSymbolTable();

		if (c.name != TreeConstants.Object_) {
			try {
				// Check that parent exists
				classTable.getClassFor(c.parent);
			} catch (Exception e) {
				classTable
						.error(c, "Class " + c.name
								+ " inherits from an undefined class "
								+ c.parent + ".");
			}
		}

		for (Enumeration e = c.features.getElements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			((TreeNode) o).accept(this);
		}
	}

	@Override
	public void visit(method m) {
		symbolTable.enterScope();

		
		for (int i = 0; i < m.formals.getLength(); i++) {
			formalc f = (formalc) m.formals.getNth(i);
			f.accept(this);
		}
		m.expr.accept(this);
		symbolTable.exitScope();

		// Verify that return type is known
		if (!classTable.contains(m.return_type)) {
			classTable.error(m, "Undefined return type " + m.return_type
					+ " in method " + m.name + ".");
		}
		symbolTable.addId(m.name, m.return_type);

		AbstractSymbol return_type = m.expr.get_type();
		if (return_type != m.return_type) {
			if (return_type == TreeConstants.SELF_TYPE) {
				return_type = symbolTable.find(AbstractTable.idtable
						.lookup("type_of_self"));
			}

			if (classTable.lub(return_type, m.return_type) != m.return_type) {
				classTable.error(m, "Inferred return type " + m.expr.get_type()
						+ " of method " + m.name
						+ " does not conform to declared return type "
						+ m.return_type + ".");
			}
		}
	}

	@Override
	public void visit(attr a) {
		a.init.accept(this);
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
			case_type = classTable.lub(case_type, ((branch) o).type_decl);
		}
		t.set_type(case_type);
	}

	@Override
	public void visit(assign a) {
		// do we need this 2nd pass?
		a.expr.accept(this);

		AbstractSymbol id_type = symbolTable.find(a.name);

		if (classTable.lub(a.expr.get_type(), id_type) != id_type) {
			classTable
					.error(a,
							"Type "
									+ a.expr.get_type()
									+ " of assigned expression does not conform to declared type "
									+ id_type + " of identifier " + a.name
									+ ".");
		}

		a.set_type(id_type);
	}

	/**
	 * expr[@TYPE].ID(expr[[,expr]]*)
	 */
	@Override
	public void visit(static_dispatch sd) {

		for (@SuppressWarnings("rawtypes")
		Enumeration e = sd.actual.getElements(); e.hasMoreElements();) {
			Object n = e.nextElement();
			if (((Expression) n).get_type() == null) {
				((TreeNode) n).accept(this);
			}
		}
		if (sd.expr.get_type() == null) {
			// Visit e0
			sd.expr.accept(this);
		}

		AbstractSymbol id_type = sd.expr.get_type();

		if (classTable.lub(id_type, sd.type_name) != sd.type_name) {
			classTable.error(sd, "Expression type " + id_type
					+ " does not conform to declared static dispatch type "
					+ sd.type_name + ".");
		}

		AbstractSymbol dispatch_method_type = null;

		if (id_type != TreeConstants.SELF_TYPE) {
			class_c method_class;
			try {
				method_class = classTable.getClassFor(id_type);
				dispatch_method_type = method_class.getSymbolTable().find(
						sd.name);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			dispatch_method_type = symbolTable.find(sd.name);
		}

		if (dispatch_method_type == TreeConstants.SELF_TYPE) {
			sd.set_type(id_type);
		} else {
			sd.set_type(dispatch_method_type);
		}

	}

	@Override
	public void visit(dispatch d) {
		/*
		 * e.g. e0.f(e1...en)
		 * 
		 * e0 -> d.expr
		 * 
		 * f -> d.name
		 * 
		 * e1..en -> d.actual
		 */

		List<AbstractSymbol> formalTypes = (List) symbolTable
				.lookup(AbstractTable.idtable.lookup(d.name.str + "_formals"));

		for (int i = 0; i < d.actual.getLength(); i++) {
			Expression f = (Expression) d.actual.getNth(i);

			f.accept(this);

			if (formalTypes != null && f.get_type() != TreeConstants.SELF_TYPE
					&& f.get_type() != formalTypes.get(i)) {
				classTable.error(
						d,
						"In call of method " + d.name + ", type "
								+ f.get_type() + " of parameter " + i
								+ " does not conform to declared type "
								+ formalTypes.get(i) + ".");
			}

		}

		for (@SuppressWarnings("rawtypes")
		Enumeration e = d.actual.getElements(); e.hasMoreElements();) {
			Expression expr = (Expression) e.nextElement();
			expr.accept(this);

		}
		if (d.expr.get_type() == null) {
			// Visit e0
			d.expr.accept(this);
		}

		AbstractSymbol id_type = d.expr.get_type();
		AbstractSymbol dispatch_method_type = TreeConstants.No_type;

		if (id_type != TreeConstants.SELF_TYPE) {
			class_c method_class;
			try {
				method_class = classTable.getClassFor(id_type);
				dispatch_method_type = method_class.getSymbolTable().find(
						d.name);
				if (dispatch_method_type == null) {

					classTable.error(d, "Dispatch to undefined method "
							+ d.name + ".");
				}
			} catch (Exception e) {
				// Ooops, swallow error
				// No class found
			}
		} else {
			dispatch_method_type = symbolTable.find(d.name);
		}

		/*
		 * Type of dispatch is the value of the 'expr'
		 * 
		 * If the 'expr' is of type SELF_TYPE then the dispatch type is the type
		 * of the id receiving the dispatch.
		 * 
		 * Otherwise it's the type of 'expr'
		 */

		if (dispatch_method_type == TreeConstants.SELF_TYPE) {
			d.set_type(id_type);
		} else {
			d.set_type(dispatch_method_type);
		}

	}

	@Override
	public void visit(cond c) {
		c.pred.accept(this);
		c.pred.set_type(TreeConstants.Bool);
		c.else_exp.accept(this);
		c.then_exp.accept(this);
		c.set_type(classTable.lub(c.else_exp.get_type(), c.then_exp.get_type()));
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

		symbolTable.addId(l.identifier, l.type_decl);

		// Verify init if exists
		if (!(l.init instanceof no_expr)) {
			l.init.accept(this);

			if (classTable.lub(l.init.get_type(), l.type_decl) != l.type_decl) {
				classTable.error(l, "Inferred type " + l.init.get_type()
						+ " of initialization of " + l.identifier
						+ " does not conform to identifier's declared type "
						+ l.type_decl);
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
		n.set_type(TreeConstants.Int);
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

		if (eq.e1.get_type() == TreeConstants.Bool
				|| eq.e1.get_type() == TreeConstants.Int
				|| eq.e1.get_type() == TreeConstants.Str) {
			if (eq.e1.get_type() != eq.e2.get_type()) {
				classTable.error(eq, "Illegal comparison with a basic type.");
			}
		}

		eq.set_type(TreeConstants.Bool);
	}

	@Override
	public void visit(leq leq) {
		leq.e1.accept(this);
		leq.e2.accept(this);

		if (leq.e1.get_type() != TreeConstants.Int
				|| leq.e2.get_type() != TreeConstants.Int) {
			classTable.error(leq, "non-Int arguments:" + leq.e1.get_type()
					+ " <= " + leq.e2.get_type());
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Visitor#accept(bool_const)
	 */
	@Override
	public void visit(bool_const b) {
	}

	@Override
	public void visit(string_const s) {
	}

	@Override
	public void visit(new_ n) {
		if (!classTable.contains(n.type_name)) {
			classTable.error(n, "'new' used with undefined class "
					+ n.type_name + ".");
		}
	}

	@Override
	public void visit(isvoid iv) {
		iv.e1.accept(this);
	}

	@Override
	public void visit(no_expr ne) {
		/*
		 * Is this void?
		 */
	}

	@Override
	public void visit(object o) {
		if (o.equals("self")) {
			o.set_type(TreeConstants.SELF_TYPE);
		} else {
			AbstractSymbol type = symbolTable.find(o.name);
			if (type == null) {
				classTable.error(o, "Undeclared identifier " + o.name + ".");
			}
			o.set_type(type);

		}
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

}
