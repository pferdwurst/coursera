class Bar inherits IO {

     h : Int <- 1;


     c : Int <- doh();

     d : Object <- printh();

     printh() : Int { 
	         { 
		  outuint(h); 
		  0; 
		 } 
		};

     doh() : Int { (let i: Int <- h in { h <- h + 1; i; } ) };

};

(* scary . . . *)
class Main {
  d : Bar <- new Bar;

  main(): String { "do nothing" };

};



