bool r, s, p, q;
init
{
	do
	:: atomic { 
if
		:: true -> r = 0;
		:: true -> r = 1; 
		fi;
if
		:: true -> s = 0;
		:: true -> s = 1; 
		fi;
if
		:: true -> p = 0;
		:: true -> p = 1; 
		fi;
if
		:: true -> q = 0;
		:: true -> q = 1; 
		fi;
	}
	od;
}
ltl {!( (([](((s) || (!q)))) && ([](((!p) || (<>(q))))) && (((p) || ([](((!r) || ([](!s))))))) ) )}
