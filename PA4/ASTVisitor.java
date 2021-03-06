import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 
 */

/**
 * @author compilers
 * 
 */
public class ASTVisitor implements Visitor {

	static final String[] keywords = { "case", "if" };

	protected ClassTable classTable;

	private SymbolTable symbolTable;

	/**
	 * 
	 */
	public ASTVisitor(ClassTable ct) {
		classTable = ct;
	}

	public void setSymbolTable(SymbolTable st) {
		symbolTable = st;
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
				|| c.parent == TreeConstants.Int
				|| c.parent == TreeConstants.SELF_TYPE) {

			classTable.error(c, "Class " + c.name + " cannot inherit class "
					+ c.parent + ".");
		}
		symbolTable.addId(AbstractTable.idtable.addString("type_of_self"),
				c.name);
		for (Enumeration e = c.features.getElements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			((TreeNode) o).accept(this);
		}

	}

	@Override
	public void visit(method m) {

		// Check if this is an override
		if (symbolTable.find(m.name) != null
				&& m.name != TreeConstants.cool_abort
				&& m.name != TreeConstants.copy
				&& m.name != TreeConstants.type_name) {

			List<AbstractSymbol> formalTypes = (List) symbolTable
					.lookup(AbstractTable.idtable.lookup(m.name.str
							+ "_formals"));

			if (formalTypes != null
					&& m.formals.getLength() != formalTypes.size()) {
				classTable.error(m,
						"Incompatible number of formal parameters in redefined method "
								+ m.name + ".");
			}
		}

		symbolTable.enterScope();

		// Check for duplicate formals
		Set<AbstractSymbol> formalIds = new HashSet<AbstractSymbol>();
		List<AbstractSymbol> formalTypes = new LinkedList<AbstractSymbol>();

		for (Enumeration e = m.formals.getElements(); e.hasMoreElements();) {
			formalc f = (formalc) e.nextElement();
			if (!formalIds.add(f.name)) {
				classTable.error(f, "Formal parameter " + f.name
						+ " is multiply defined.");
			}
			formalTypes.add(f.type_decl);
			f.accept(this);
		}
		m.expr.accept(this);
		symbolTable.exitScope();
		// Add parameter type information
		symbolTable.addId(
				AbstractTable.idtable.addString(m.name.str + "_formals"),
				formalTypes);
		symbolTable.addId(m.name, m.return_type);
	}

	@Override
	public void visit(attr a) {
		if (a.name.equalString("self", 4)) {
			classTable.error(a, "Cannot assign to 'self'.");
		}
		if (symbolTable.find(a.name) != null) {
			classTable.error(a, "Attribute " + a.name
					+ " is an attribute of an inherited class");
		}
		a.init.accept(this);

		symbolTable.addId(a.name, a.type_decl);
	}

	@Override
	public void visit(formalc f) {
		if (f.name.equalString("self", 4)) {
			classTable.error(f,
					"'self' cannot be the name of a formal parameter.");
		}
		if (f.type_decl == TreeConstants.SELF_TYPE) {
			classTable.error(f, "Formal parameter " + f.type_decl
					+ " cannot have type SELF_TYPE.");
		}
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

		// Check for duplicate branches
		Set<AbstractSymbol> branch_types = new HashSet<AbstractSymbol>();

		for (Enumeration e = t.cases.getElements(); e.hasMoreElements();) {
			Object o = e.nextElement();
			// visit branch
			branch b = (branch) o;
			b.accept(this);
			if (!branch_types.add(b.type_decl)) {
				classTable.error(t, "Duplicate branch " + b.type_decl
						+ " in case statement.");
			}
			case_type = classTable.lub(case_type, b.type_decl);
		}
		t.set_type(case_type);
	}

	@Override
	public void visit(assign a) {
		if (a.name.equalString("self", 4)) {
			classTable.error(a, "Cannot assign to 'self'.");
		}
		a.expr.accept(this);

		a.set_type(symbolTable.find(a.name));
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
		AbstractSymbol dispatch_method_type = symbolTable.find(sd.name);

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

		for (@SuppressWarnings("rawtypes")
		Enumeration e = d.actual.getElements(); e.hasMoreElements();) {
			((TreeNode) e.nextElement()).accept(this);
		}

		// Visit e0
		d.expr.accept(this);

		/*
		 * Type of dispatch is the value of the 'expr' If the 'expr' is of type
		 * SELF_TYPE then the dispatch type is the type of the id receiving the
		 * dispatch. Otherwise it's the type of 'expr'
		 */
		AbstractSymbol id_type = d.expr.get_type();
		AbstractSymbol dispatch_method_type = TreeConstants.No_type;

		if (id_type != TreeConstants.SELF_TYPE) {
			class_c method_class;
			try {
				method_class = classTable.getClassFor(id_type);
				dispatch_method_type = method_class.getSymbolTable().find(
						d.name);
			} catch (Exception e) {
				// TODO Yikes, swallowing error
				// e.printStackTrace();
			}
		} else {
			dispatch_method_type = symbolTable.find(d.name);
		}

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
		if (l.pred.get_type() != TreeConstants.Bool) {
			classTable.error(l, "Loop condition does not have type Bool");
		}
		l.body.accept(this);
		l.set_type(l.body.get_type());
	}

	@Override
	public void visit(block b) {

		AbstractSymbol blockName = new StringSymbol("BLOCK"
				+ String.format("%03d", b.lineNumber), 8, 8);
		symbolTable.enterScope();

		for (Enumeration e = b.body.getElements(); e.hasMoreElements();) {
			Expression expr = (Expression) e.nextElement();
			expr.accept(this);
			AbstractSymbol expr_type = expr.get_type();
			symbolTable.addId(blockName, expr_type);
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
		}
		l.body.accept(this);

		l.set_type(l.body.get_type());
		symbolTable.exitScope();
	}

	@Override
	public void visit(plus p) {
		p.e1.accept(this);
		p.e2.accept(this);

		p.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(sub s) {
		s.e1.accept(this);
		s.e2.accept(this);

		s.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(mul m) {
		m.e1.accept(this);
		m.e2.accept(this);

		m.set_type(TreeConstants.Int);
	}

	@Override
	public void visit(divide d) {
		d.e1.accept(this);
		d.e2.accept(this);

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
		if (o.equals("self")) {
			o.set_type(TreeConstants.SELF_TYPE);
		} else {
			AbstractSymbol type = symbolTable.find(o.name);
			if (type == null) {
				classTable.error(o, "Undeclared identifier " + o.name + ".");
				type = TreeConstants.No_type;
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
