import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;

/**
 * This class may be used to contain the semantic information such as the
 * inheritance graph. You may use it or not as you like: it is only here to
 * provide a container for the supplied methods.
 */
class ClassTable {
	private int semantErrors;
	private PrintStream errorStream;

	private AbstractSymbol filename;
	public Classes baseClasses;
	public Classes allClasses;

	/**
	 * Creates data structures representing basic Cool classes (Object, IO, Int,
	 * Bool, String). Please note: as is this method does not do anything
	 * useful; you will need to edit it to make if do what you want.
	 * */
	private void installBasicClasses(ASTVisitor visitor) {
		AbstractSymbol filename = AbstractTable.stringtable
				.addString("<basic class>");

		// The following demonstrates how to create dummy parse trees to
		// refer to basic Cool classes. There's no need for method
		// bodies -- these are already built into the runtime system.

		// IMPORTANT: The results of the following expressions are
		// stored in local variables. You will want to do something
		// with those variables at the end of this method to make this
		// code meaningful.

		// The Object class has no parent class. Its methods are
		// cool_abort() : Object aborts the program
		// type_name() : Str returns a string representation
		// of class name
		// copy() : SELF_TYPE returns a copy of the object

		class_c Object_class = new class_c(
				0,
				TreeConstants.Object_,
				TreeConstants.No_class,
				new Features(0)
						.appendElement(
								new method(0, TreeConstants.cool_abort,
										new Formals(0), TreeConstants.Object_,
										new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.type_name,
										new Formals(0), TreeConstants.Str,
										new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.copy,
										new Formals(0),
										TreeConstants.SELF_TYPE, new no_expr(0))),
				filename);

		// The IO class inherits from Object. Its methods are
		// out_string(Str) : SELF_TYPE writes a string to the output
		// out_int(Int) : SELF_TYPE "    an int    " "     "
		// in_string() : Str reads a string from the input
		// in_int() : Int "   an int     " "     "

		class_c IO_class = new class_c(
				0,
				TreeConstants.IO,
				TreeConstants.Object_,
				new Features(0)
						.appendElement(
								new method(0, TreeConstants.out_string,
										new Formals(0)
												.appendElement(new formalc(0,
														TreeConstants.arg,
														TreeConstants.Str)),
										TreeConstants.SELF_TYPE, new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.out_int,
										new Formals(0)
												.appendElement(new formalc(0,
														TreeConstants.arg,
														TreeConstants.Int)),
										TreeConstants.SELF_TYPE, new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.in_string,
										new Formals(0), TreeConstants.Str,
										new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.in_int,
										new Formals(0), TreeConstants.Int,
										new no_expr(0))), filename);

		// The Int class has no methods and only a single attribute, the
		// "val" for the integer.

		class_c Int_class = new class_c(0, TreeConstants.Int,
				TreeConstants.Object_, new Features(0).appendElement(new attr(
						0, TreeConstants.val, TreeConstants.prim_slot,
						new no_expr(0))), filename);

		// Bool also has only the "val" slot.
		class_c Bool_class = new class_c(0, TreeConstants.Bool,
				TreeConstants.Object_, new Features(0).appendElement(new attr(
						0, TreeConstants.val, TreeConstants.prim_slot,
						new no_expr(0))), filename);

		// The class Str has a number of slots and operations:
		// val the length of the string
		// str_field the string itself
		// length() : Int returns length of the string
		// concat(arg: Str) : Str performs string concatenation
		// substr(arg: Int, arg2: Int): Str substring selection

		class_c Str_class = new class_c(
				0,
				TreeConstants.Str,
				TreeConstants.Object_,
				new Features(0)
						.appendElement(
								new attr(0, TreeConstants.val,
										TreeConstants.Int, new no_expr(0)))
						.appendElement(
								new attr(0, TreeConstants.str_field,
										TreeConstants.prim_slot, new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.length,
										new Formals(0), TreeConstants.Int,
										new no_expr(0)))
						.appendElement(
								new method(0, TreeConstants.concat,
										new Formals(0)
												.appendElement(new formalc(0,
														TreeConstants.arg,
														TreeConstants.Str)),
										TreeConstants.Str, new no_expr(0)))
						.appendElement(
								new method(
										0,
										TreeConstants.substr,
										new Formals(0)
												.appendElement(
														new formalc(
																0,
																TreeConstants.arg,
																TreeConstants.Int))
												.appendElement(
														new formalc(
																0,
																TreeConstants.arg2,
																TreeConstants.Int)),
										TreeConstants.Str, new no_expr(0))),
				filename);

		/*
		 * Do somethind with Object_class, IO_class, Int_class, Bool_class, and
		 * Str_class here
		 */
		baseClasses = new Classes(1);
		baseClasses.appendElement(Object_class).appendElement(IO_class)
				.appendElement(Int_class).appendElement(Bool_class)
				.appendElement(Str_class);

		for (Enumeration e = baseClasses.getElements(); e.hasMoreElements();) {
			class_c c = (class_c) e.nextElement();

			SymbolTable st = baseSymbolTable();

			visitor.setSymbolTable(st);
			c.accept(visitor);
			c.setSymbolTable(st);

			allClasses.appendElement(c);
		}

	}

	private SymbolTable baseSymbolTable() {
		SymbolTable st = new SymbolTable();
		st.enterScope();
		st.addId(TreeConstants.Object_, TreeConstants.Object_);
		st.addId(TreeConstants.cool_abort, TreeConstants.Object_);
		st.addId(TreeConstants.copy, TreeConstants.SELF_TYPE);
		st.addId(TreeConstants.type_name, TreeConstants.Str);
		// for (AbstractSymbol cn : baseClasses) {
		// st.addId(cn, cn);
		// }
		return st;
	}

	/**
	 * Find the least common ancestor of 2 objects. If the A inherits from B
	 * then B can be the LCA of the pair
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public AbstractSymbol lub(AbstractSymbol t1, AbstractSymbol t2) {
		// Short-circuit if both types are identical
		if (t1 == t2) {
			return t1;
		}
		
		AbstractSymbol lub = TreeConstants.Object_;

		try {

			Deque<AbstractSymbol> t1Stack = new ArrayDeque<AbstractSymbol>();

			// find ancestor path of t1
			class_c t1_class = getClassFor(t1);

			t1Stack.push(t1);

			while (t1Stack.peek() != TreeConstants.Object_) {
				t1Stack.push(t1_class.getParent());
				t1_class = getClassFor(t1_class.getParent());
			}

			Deque<AbstractSymbol> t2Stack = new ArrayDeque<AbstractSymbol>();
			t2Stack.push(t2);
			class_c t2_class = getClassFor(t2);

			/*
			 * while first element in deque not equal to
			 */
			while (t2Stack.peek() != TreeConstants.Object_) {
				t2Stack.push(t2_class.getParent());
				t2_class = getClassFor(t2_class.getParent());
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

	public ClassTable(Classes cls) {
		semantErrors = 0;
		errorStream = System.err;
		allClasses = new Classes(1);
		boolean containsMain = false;

		/* fill this in */
		ASTVisitor astv = new ASTVisitor(this);

		installBasicClasses(astv);

		// Grab the filename from the first class
		class_c firstClass = ((class_c) cls.getElements().nextElement());
		filename = firstClass.filename;

		/* some semantic analysis code may go here */
		for (Enumeration e = cls.getElements(); e.hasMoreElements();) {
			class_c c = (class_c) e.nextElement();
			if (c.name == TreeConstants.Main) {
				containsMain = true;
			}

			SymbolTable st;
			try {
				if (c.parent == null) {
					c.parent = TreeConstants.Object_;
				}
				st = getClassFor(c.parent).getSymbolTable().copy();

			} catch (Exception ex) {
				// Yikes, swallow another error!
				st = new SymbolTable();
				st.enterScope();
			}

			// Is a basic class being redefined?
			if (baseClasses.contains(c.name) ) {
				semantError(filename, c).println(
						"Redefinition of basic class " + c.name + ".");
			}
			// Verify that we're not redefining a class
			if (allClasses.contains(c.name)) {
				semantError(c).println(
						"Class " + c.name + " was previously defined.");
			}

			st.enterScope();
			astv.setSymbolTable(st);
			c.accept(astv);
			c.setSymbolTable(st);

			allClasses.appendElement((TreeNode) c);

		}

		if (!containsMain) {
			semantError().println("Class Main is not defined.");
		}
		if (errors()) {
			System.err
					.println("Compilation halted due to static semantic errors.");
			System.exit(1);
		}
	}

	public Classes getClasses() {
		return allClasses;
	}

	/**
	 * Prints line number and file name of the given class.
	 * 
	 * Also increments semantic error count.
	 * 
	 * @param c
	 *            the class
	 * @return a print stream to which the rest of the error message is to be
	 *         printed.
	 * 
	 * */
	public PrintStream semantError(class_c c) {
		return semantError(c.getFilename(), c);
	}

	/**
	 * Prints the file name and the line number of the given tree node.
	 * 
	 * Also increments semantic error count.
	 * 
	 * @param filename
	 *            the file name
	 * @param t
	 *            the tree node
	 * @return a print stream to which the rest of the error message is to be
	 *         printed.
	 * 
	 * */
	public PrintStream semantError(AbstractSymbol filename, TreeNode t) {
		errorStream.print(filename + ":" + t.getLineNumber() + ": ");
		return semantError();
	}

	public void error(TreeNode t, String errorMsg) {
		PrintStream ps = semantError(filename, t);
		ps.println(errorMsg);
	}

	/**
	 * Increments semantic error count and returns the print stream for error
	 * messages.
	 * 
	 * @return a print stream to which the error message is to be printed.
	 * 
	 * */
	public PrintStream semantError() {
		semantErrors++;
		return errorStream;
	}

	/** Returns true if there are any static semantic errors. */
	public boolean errors() {
		return semantErrors != 0;
	}

	public class_c getClassFor(AbstractSymbol t1) throws Exception {
		for (Enumeration<class_c> e = allClasses.getElements(); e
				.hasMoreElements();) {
			class_c k1 = e.nextElement();
			if (k1.getName().equalString(t1.str, t1.str.length())) {
				return k1;
			}
		}
		throw new Exception("Cannot find class for " + t1);
	}

	public boolean contains(AbstractSymbol class_name) {
		return allClasses.contains(class_name);
	}
}
