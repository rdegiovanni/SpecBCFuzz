
bool h,m,p;

init
{
	do
	:: atomic { 
		if
		:: true -> h = 0;
		:: true -> h = 1; 
		fi;
		if
		:: true -> m = 0; 
		:: true -> m = 1;
		fi; 
		if
		:: true -> p = 0; 
		:: true -> p = 1;
		fi; 
	}
	od;
}


ltl {
<>(h && m)
}
ltl {

}
