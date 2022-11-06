package MyAnalysis;

import pascal.taie.World;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.JMethod;

import java.util.*;

class NewConstraint {
    String to;
    int allocId;

    NewConstraint(String to_, int allocId_) {
        to = to_;
        allocId = allocId_;
    }
}

public class Anderson {

    private World world;

    /**
     *  We use MySignature(String) here to index the points-to-sets, where each variable owns
     *  its own set that records the objects that the variable may point to
     */
    private Map<String, TreeSet<Integer> > pts = new HashMap<>();

    /**
     * Each "key"(String, or signature) represents a variable in the dataflow graph, and
     * the "value" associated with it is a set that contains the other variables which the
     * object-pointer-flow will propagate to, i.e. if we take the key as a Rvalue in some
     * statements, all the Lvalues corresponding with it are included in the set.
     */
    private Map<String, TreeSet<String> > graph = new HashMap<>();

    /**
     *  These are the necessary components for clone-based inter-procedural analysis
     */
    private Map<String, Integer> method_counter_map = new TreeMap<String ,Integer>();
    public final int clone_depth = 1;


    private void AddEdge(String from, String to){
        if(!graph.containsKey(from)){
            graph.put(from, new TreeSet<>());
        }
        graph.get(from).add(to);
    }
    /**
     *  Map the variables to allocIds given the New statements
     */
    private List<NewConstraint> newConstraintList = new ArrayList<NewConstraint>();

    private void AddNewConstraints(String to, int allocId){
        newConstraintList.add(new NewConstraint(to, allocId));
    }

    /**
     *  For queries(benchmark.internal.Benchmark.test)
     */
    TreeMap<Integer, String> queries = new TreeMap<>();

    /**
     * pointer-propagation job list
     */
    Map<String, TreeSet<Integer> > Jobs = new TreeMap<>();


    /**
     *  Solve the PTA
     * @param world_ ZA WARUDO!
     */
    public void Solve(World world_){
        world = world_;
        Initialize();
        Run();
        Answer();
    }

    /**
     * Answer the queries
     */
    private void Answer(){
        StringBuilder answer = new StringBuilder();
        for(Map.Entry<Integer, String> query : queries.entrySet()){
            answer.append(query.getKey()).append(":");
            for(Integer object : pts.get(query.getValue())){
                answer.append(" ").append(object);
            }
            answer.append("\n");
        }
        System.out.print(answer);
    }

    /**
     * Perform the PTA iteratively
     */
    private void Run(){

        while(!Jobs.isEmpty()){
            Propagate();
        }
    }

    /**
     * Propagate the pointer-flow by one hop and update the Jobs queue
     */
    private void Propagate(){
        Map<String, TreeSet<Integer> > new_job = new TreeMap<>();

        // Traverse all the jobs
        for(Map.Entry<String, TreeSet<Integer> > job : Jobs.entrySet()){
            String sig = job.getKey();
            TreeSet<Integer> new_objects = job.getValue();

            // Propagate to its Lvalues
            if(!graph.containsKey(sig)){
                // this signature flows to nothing!
                continue;
            }
            for(String flow_to : graph.get(sig)){
                if(!pts.containsKey(flow_to)){
                    pts.put(flow_to, new TreeSet<>());
                }
                for(Integer object : new_objects){
                    if(pts.get(flow_to).add(object)){
                        if(!new_job.containsKey(flow_to)){
                            new_job.put(flow_to, new TreeSet<>());
                        }
                        new_job.get(flow_to).add(object);
                    }
                }
            }
        }
        Jobs = new_job;
    }

    /**
     * Initialized everything
     */
    public void Initialize(){

        /* The entry method of the program */

        /* Initialize the constraints by method */
        InitConstraints(world.getMainMethod());
        //        for(NewConstraint newConstraint : newConstraintList){
//        System.out.println(newConstraint.to + " " + newConstraint.allocId);
//        }
//        for(AssignConstraint assignConstraint : assignConstraintList){
//        System.out.println(assignConstraint.to + " " + assignConstraint.from);
//        }
        InitPTS();
//        System.out.println("NewConstraints:");
//        for(Map.Entry<String, TreeSet<Integer>> entry: pts.entrySet()){
//            System.out.println(entry.getKey() + ":");
//            for(Integer alloc : entry.getValue()){
//                System.out.print(alloc + " ");
//            }
//            System.out.print("\n");
//        }
//        System.out.println("Queries:");
//        for(Map.Entry<Integer, String> entry : queries.entrySet()){
//            System.out.println(entry.getKey() + " " + entry.getValue());
//        }
//        System.out.println("Graph:");
//        for(Map.Entry<String, TreeSet<String> > entry : graph.entrySet()){
//            System.out.println(entry.getKey() + " flows to:");
//            for(String to : entry.getValue()){
//                System.out.print("    " + to + " ");
//            }
//            System.out.print("\n");
//        }
        InitJobs();

    }

    /**
     * Record the initialized state of the pointer flow recursively method by method
     * @param method the entry method
     */
    public void InitConstraints(JMethod method){
        /* Exit Status */
        if (isLibrary(method)) {
            return;
        }

        /* Try to clone the methods */
        int cur_clone_depth = Clone(method);

        /* Traverse all the statements */
        List<Stmt> statements = method.getIR().getStmts();
        int allocId = 0;
        for(Stmt statement : statements){
            if (statement instanceof Invoke invoke_stmt){
                String signature = invoke_stmt.getMethodRef().toString();

                // These statements may throw exceptions if the argument is not a constant, handle them?
                if (signature.equals("<benchmark.internal.Benchmark: void alloc(int)>")){
                    allocId =  Integer.parseInt(invoke_stmt.getInvokeExp().getArg(0).getConstValue().toString());
                    continue;
                } else if(signature.equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>")){
                    List<Var> args = invoke_stmt.getInvokeExp().getArgs();
                    queries.put(
                                Integer.parseInt(args.get(0).getConstValue().toString()), // allocId
                                GenMySignature(args.get(1), cur_clone_depth) // Var
                            );
                    continue;
                }

                // Recursively construct...
                // it may throw an exception if the resolving process fails
                InitConstraints(invoke_stmt.getInvokeExp().getMethodRef().resolve());
            } else if (statement instanceof New new_stmt) {
                // New -> Var = NewExp; NewExp -> NewInstance | NewArray | NewMultiArray.
                // NewInstance -> new ClassType
                // For Tai-e, the Var is always temp$k?
                if(allocId != 0) {
                    AddNewConstraints(GenMySignature(new_stmt.getLValue(), cur_clone_depth), allocId); // map temp$k(heap var) to allocId
                    allocId = 0;
                }
            } else if (statement instanceof Copy copy_stmt){
                // Copy -> Var = Var;
                AddEdge(
                        GenMySignature(copy_stmt.getRValue(), cur_clone_depth),
                        GenMySignature(copy_stmt.getLValue(), cur_clone_depth)
                );
            } else if (statement instanceof LoadField lf_stmt){
                // LoadField -> Var = FieldAccess;
                // FieldAccess -> InstanceFieldAccess | StaticFieldAccess
                AddEdge(
                        GenMySignature(lf_stmt.getRValue(), cur_clone_depth),
                        GenMySignature(lf_stmt.getLValue(), cur_clone_depth)
                );
            } else if (statement instanceof StoreField sf_stmt){
                // StoreField -> FieldAccess = Var;
                AddEdge(
                        GenMySignature(sf_stmt.getRValue(), cur_clone_depth),
                        GenMySignature(sf_stmt.getLValue(), cur_clone_depth)
                );
            } else if (statement instanceof StoreArray sa_stmt){
                // StoreArray -> ArrayAccess = Var;
                AddEdge(
                        GenMySignature(sa_stmt.getRValue(), cur_clone_depth),
                        GenMySignature(sa_stmt.getArrayAccess().getBase(), cur_clone_depth)
                );
            } else if (statement instanceof LoadArray la_stmt){
                // LoadArray -> Var = ArrayAccess;
                AddEdge(
                        GenMySignature(la_stmt.getArrayAccess().getBase(), cur_clone_depth),
                        GenMySignature(la_stmt.getLValue(), cur_clone_depth)
                );
            }
        }
    }

    /**
     * Initialize the points-to-sets given all the new-constraints
     */
    public void InitPTS(){
        for(NewConstraint newConstraint : newConstraintList){
            if(!pts.containsKey(newConstraint.to)){
                pts.put(newConstraint.to, new TreeSet<>());
            }
            pts.get(newConstraint.to).add(newConstraint.allocId);
        }
    }

    /**
     * Initialize the Jobs list
     */
    public void InitJobs(){
        for(NewConstraint newConstraint : newConstraintList){
            if(!Jobs.containsKey(newConstraint.to)){
                Jobs.put(newConstraint.to, new TreeSet<>());
            }
            Jobs.get(newConstraint.to).add(newConstraint.allocId);
        }
    }


    /**
     * Try to clone the given method before its clone-depth reaches a constant
     * @param method the method to be cloned
     * @return the number of clones of the given method after this function finishes
     * @// TODO: 2022/11/6 For now this function is only a counter
     */
    public int Clone(JMethod method){
        String signature = method.getSignature();
        if (!method_counter_map.containsKey(signature)) {
            method_counter_map.put(signature, 1);
            return 1;
        } else {
            Integer counter = method_counter_map.get(signature);
            if(counter < clone_depth){
                counter += 1;
                method_counter_map.put(signature, counter);
            }
            return counter;
        }

    }

    /**
     *  to filtrate some methods
     * @param method the method to check
     * @return true if it's an embedded method of Java's library
     */
    public boolean isLibrary(JMethod method){
        String sig = method.getSignature();
        if(sig.contains("<java.")){
            return true;
        }
        return false;
    }

    /**
     * Generate a signature given an expression and its clone-depth
     * @param exp The given Tai-e expression
     * @param depth The clone-depth of the method of the given expression
     * @return The generated signature
     */
    public String GenMySignature(Exp exp, int depth){
        String sig;
        if (exp instanceof Var var){
            //System.out.println("var");
            sig = depth + var.getMethod().getSignature() + var.getName();
        } else if (exp instanceof FieldAccess fa_exp){
            //System.out.println("field");
            sig = depth + fa_exp.getFieldRef().resolve().getSignature();
        } else {
            sig = null;
        }
        //System.out.println(sig);
        return sig;
    }
}
