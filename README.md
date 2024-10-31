Change pom.xml and update project.baseDir
It should address the SpecBCFuzz project. For example, lib is a subfolder of the SpecBCFuzz project.

After that, you must set the Main.java (src/main/).

You should edit:
* solverBin_version (File instance).
* solver_version (SATSolver instance).

Certify that solver_version is an element of the list solvers.

After that, you need to provide some parameters to run SpecBCFuzz.

For example:

-log=/home/user/outcomes -lib=/home/user/lib  -id=1 -run=1 -id=10 -sattimeout=60 -experiment=searchbased:SemSyn-format:gore -popSize=50 -maxNumOfInd=600 -specs=/home/user/specs/atm -os=mac
 
* log represents the folder for the outcomes;
* lib represents the folder for lib. You may reference a copy of /lib present in the SpecBCFuzz repository;
* id indicates a single number or name for the execution;
* run is the number of executions of the differential search fuzzing;
* sattimeout is the timeout for each LTL solver;
* experiment indicates the type of experiment. For instance, the format of inputs;
* popSize indicates the population size for the genetic algorithm;
* maxNumOfInd indicates the maximum number of individuals created in the genetic algorithm;
* specs is the path for the specification and boundary conditions;
* os refers to the operating systems;

Additional parameters and options are available in src/main/Main.java. Feel free to test them or add new ones.

You can find specifications and boundary conditions (divergences or conflicts in the specification) in the following repository: https://github.com/SpecBCFuzz/repo
