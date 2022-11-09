package MyAnalysis;

import pascal.taie.World;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.JMethod;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

    private Set<Integer> all_allocIds = new TreeSet<>();

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
     *  These are the necessary components for clone-based inter-procedural analysis.
     */
    private Map<String, Integer> method_counter_map = new TreeMap<>();
    public final int clone_depth = 30;

    /**
     * This map is used to detect the cycles in the call graph.
     */
    private Map<String, Integer> single_path_visited_counter = new TreeMap<>();


    /**
     * Statement: LHS = RHS,
     * which means LHS set contains RHS set (RHS flows to LHS)
     * @param from The var/symbol in the RHS
     * @param to The var/symbol in the LHS
     */
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
     * object-propagation job list
     */
    Map<String, TreeSet<Integer> > Jobs = new TreeMap<>();


    /**
     *  Solve the PTA
     * @param world_ ZA WARUDO!
     */
    public void Solve(World world_){
        world = world_;
        try {
            Initialize();
            Run();
        } catch (Exception e){
            Answer(true);
            return;
        }
        Answer(false);
    }

    /**
     * Answer the queries
     * @param answer_all true then output all objects
     */
    private void Answer(Boolean answer_all){
        StringBuilder answer = new StringBuilder();
        if(!answer_all) {
            for (Map.Entry<Integer, String> query : queries.entrySet()) {
                answer.append(query.getKey()).append(":");
                if (!pts.containsKey(query.getValue())) {
                    answer.append("\n");
                    continue;
                }
                for (Integer object : pts.get(query.getValue())) {
                    answer.append(" ").append(object);
                }
                answer.append("\n");
            }
        } else {
            for (Map.Entry<Integer, String> query : queries.entrySet()) {
                answer.append(query.getKey()).append(":");
                for (Integer object : all_allocIds) {
                    answer.append(" ").append(object);
                }
                answer.append("\n");
            }
        }
        System.out.print(answer);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("result.txt"));
            out.write(answer.toString());
            out.close();
        } catch (IOException e) {
            System.out.print(answer);
        }
    }

    /**
     * Perform the PTA iteratively
     */
    private void Run() {

        while(!Jobs.isEmpty()){
            Propagate();
        }
    }

    /**
     * Propagate the pointer-flow by one hop and update the Jobs queue
     * @// TODO: 2022/11/8 Consider the objects
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
     * Initialize everything
     */
    public void Initialize(){

        /* The entry method of the program */

        /* Initialize the constraints by method */
        InitConstraints(world.getMainMethod());

        System.out.println("NewConstraints:");
        for(NewConstraint newConstraint: newConstraintList){
            System.out.println(newConstraint.allocId + " " + newConstraint.to);
        }
        System.out.println("Queries:");
        for(Map.Entry<Integer, String> entry : queries.entrySet()){
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        System.out.println("Graph:");
        for(Map.Entry<String, TreeSet<String> > entry : graph.entrySet()){
            System.out.println(entry.getKey() + " flows to:");
            for(String to : entry.getValue()){
                System.out.print("    " + to + " ");
            }
            System.out.print("\n");
        }
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

        /**
         * @// TODO: 2022/11/8 The function shouldn't return now, it has to propagate the objects in the cycle to this method
         */
        if(DetectCycle(method)){
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
                if (signature.equals("<benchmark.internal.Benchmark: void alloc(int)>") ||
                        signature.equals("<benchmark.internal.BenchmarkN: void alloc(int)>")
                ){
                    allocId =  Integer.parseInt(invoke_stmt.getInvokeExp().getArg(0).getConstValue().toString());
                    continue;
                } else if(signature.equals("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>") ||
                        signature.equals("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")
                ){
                    List<Var> args = invoke_stmt.getInvokeExp().getArgs();
                    queries.put(
                                Integer.parseInt(args.get(0).getConstValue().toString()), // allocId
                                GenMySignature(args.get(1), cur_clone_depth) // Var
                            );
                    continue;
                }

                // pass the arguments
                List<Var> args= invoke_stmt.getInvokeExp().getArgs();
                int arg_cnt = 0;
                for(Var arg : args){
                    AddEdge(
                            GenMySignature(arg, cur_clone_depth),
                            FetchFormalArgSignature(arg_cnt, invoke_stmt)
                    );
                    ++ arg_cnt;
                }

                // Recursively construct...
                // it may throw an exception if the resolving process fails
                InitConstraints(invoke_stmt.getInvokeExp().getMethodRef().resolve());


                // receive the return value
                // invoke -> var = InvokeExp
                if(invoke_stmt.getLValue() != null){
                    int ret_num = invoke_stmt.getInvokeExp().getMethodRef().resolve().getIR().getReturnVars().size();
                    for(int i=0;i<ret_num;++i) {
                        AddEdge(
                                FetchReturnSignature(i, invoke_stmt),
                                GenMySignature(invoke_stmt.getResult(), cur_clone_depth)
                        );
                    }
                }
            } else if (statement instanceof New new_stmt) {
                // New -> Var = NewExp; NewExp -> NewInstance | NewArray | NewMultiArray.
                // NewInstance -> new ClassType
                // For Tai-e, the Var is always temp$k?
                if(allocId != 0) {
                    AddNewConstraints(GenMySignature(new_stmt.getLValue(), cur_clone_depth), allocId); // map temp$k(heap var) to allocId
                    AddEdge(Integer.toString(allocId), GenMySignature(new_stmt.getLValue(),cur_clone_depth));
                    all_allocIds.add(allocId);
                    allocId = 0;
                }
            } else if (statement instanceof Copy copy_stmt){
                // Copy -> Var = Var;
                AddEdge(
                        GenMySignature(copy_stmt.getRValue(), cur_clone_depth),
                        GenMySignature(copy_stmt.getLValue(), cur_clone_depth)
                );
            } else if (statement instanceof LoadField lf_stmt){
                /**
                 * @// TODO: 2022/11/9 Complete unrolling once @xhz
                 * @// TODO: Implement unrolling many times @xhz
                 *
                 * LoadField -> Var = FieldAccess;
                 * FieldAccess -> InstanceFieldAccess | StaticFieldAccess
                 *
                 * (Field Unroll Once)
                 *
                 *
                 */
                Var lhsVar = lf_stmt.getLValue();
                FieldAccess rhsField = lf_stmt.getRValue();
                // Placeholder
                AddEdge(
                        GenMySignature(lhsVar, cur_clone_depth),
                        GenMySignature(rhsField, cur_clone_depth)
                );

            } else if (statement instanceof StoreField sf_stmt){
                /**
                 * @// TODO: Implement unrolling many times @xhz
                 *
                 * StoreField -> FieldAccess = Var;
                 *
                 * (Field Unroll Once)
                 * statement:
                 *  a.f = b;
                 * which means:
                 *  a.f contains b
                 *  a.f = b.f
                 *
                 * L value is a.f(FieldAccess), R value is b(Var)
                 */
                FieldAccess lhsField = sf_stmt.getLValue();
                Var rhsVar = sf_stmt.getRValue();
                /* a.f contains b */
                AddEdge(
                        GenMySignature(rhsVar, cur_clone_depth),
                        GenMySignature(lhsField, cur_clone_depth)
                );
                /* a.f = b.f, double edge */
                AddEdge(
                        GenMySignature(lhsField, cur_clone_depth),
                        GenSynthesizedFieldSignature(rhsVar, cur_clone_depth, lhsField)
                );
                AddEdge(
                        GenSynthesizedFieldSignature(rhsVar, cur_clone_depth, lhsField),
                        GenMySignature(lhsField, cur_clone_depth)
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
        Traceback(method);
    }

    /**
     * Initialize the points-to-sets given all the new-constraints
     * @deprecated
     */
    @Deprecated
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
            String allocId_str = Integer.toString(newConstraint.allocId);
            if(!Jobs.containsKey(allocId_str)){
                Jobs.put(allocId_str, new TreeSet<>());
            }
            Jobs.get(allocId_str).add(newConstraint.allocId);
        }
    }


    /**
     * Try to clone the given method before its clone-depth reaches a constant
     * @param method the method to be cloned
     * @return the number of clones of the given method after this function finishes
     * @// TODO: 2022/11/6 For now this function is only a counter
     */
    private int Clone(JMethod method){
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
     * Detect whether the DFS encounters a cycle.
     * @param method The vertex in the call graph
     * @return true if a cycle is detected, otherwise false
     */
    private boolean DetectCycle(JMethod method){
        String signature = method.getSignature();
        if(!single_path_visited_counter.containsKey(signature)){
            single_path_visited_counter.put(signature, 1);
            return false;
        } else if(single_path_visited_counter.get(signature) == 0) {
            single_path_visited_counter.put(signature, 1);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Perform the traceback in DFS
     * @param method The given vertex in the call graph
     */
    private void Traceback(JMethod method){
        String signature = method.getSignature();
        Integer visited_num = single_path_visited_counter.get(signature);
        single_path_visited_counter.put(signature, visited_num-1);
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
     * Generate a signature, given an expression and its clone-depth
     * @param exp The given Tai-e expression
     * @param depth The clone-depth of the method of the given expression
     * @return The generated signature
     */
    private String GenMySignature(Exp exp, int depth){
        String sig;
        if (exp instanceof Var var){
            String curMethodSig = depth + var.getMethod().getSignature();
            sig = curMethodSig + var.getName();
        } else if (exp instanceof InstanceFieldAccess iField){
            String curMethodSig = depth + iField.getBase().getMethod().getSignature();
            String baseVarName = iField.getBase().getName();
            String fieldSig = iField.getFieldRef().resolve().getSignature();
            sig = curMethodSig + baseVarName + fieldSig;
        } else if (exp instanceof StaticFieldAccess sField) {
            sig = sField.getFieldRef().resolve().getSignature();
        } else {
            sig = null;
        }
        //System.out.println(sig);
        return sig;
    }

    /**
     * Generate a synthesized signature of a field access, given a var, its clone-depth and a field.
     * Even if this var does not have this field, the fake signature will be generated.
     * @param var The given Tai-e Var
     * @param depth The clone-depth of the method of the given expression
     * @param field The wanted field
     * @return The synthesized signature of the field access
     */
    private String GenSynthesizedFieldSignature(Var var, int depth, FieldAccess field)
    {
        String curMethodSig = depth + var.getMethod().getSignature();
        String baseVarSig = curMethodSig + var.getName();
        String fieldSig = field.getFieldRef().resolve().getSignature();
        String sig = baseVarSig + fieldSig;
        return sig;
    }

    /**
     * Generate a signature of the return value of the given invoke statement
     * @param ret_cnt the number of the return value(always 0 ?)
     * @param invoke the invoke statement
     * @return the signature
     */
    private String FetchReturnSignature(int ret_cnt, Invoke invoke){
        int depth;
        String method_sig = invoke.getMethodRef().resolve().getSignature();
        if(!method_counter_map.containsKey(method_sig)){ // not cloned yet
            // normally this won't happen
            throw new RuntimeException();
        } else {
            depth = method_counter_map.get(method_sig);
        }
        return depth + invoke.getInvokeExp().getMethodRef().resolve().getSignature() +
                invoke.getMethodRef().resolve().getIR().getReturnVars().get(ret_cnt);
    }

    /**
     * Generate a signature of a formal argument of a method
     * @param arg_cnt the number of the formal argument
     * @param invoke the invoke statement
     * @return the signature
     */
    private String FetchFormalArgSignature(int arg_cnt, Invoke invoke){
        int depth;
        String method_sig = invoke.getMethodRef().resolve().getSignature();
        if(!method_counter_map.containsKey(method_sig)){ // not cloned yet
            depth = 1;
        } else{
            int counter = method_counter_map.get(method_sig);
            if(counter < clone_depth){
                depth = counter + 1;
            } else {
                depth = clone_depth;
            }
        }
        return depth + invoke.getInvokeExp().getMethodRef().resolve().getSignature() +
                invoke.getInvokeExp().getMethodRef().resolve().getIR().getParam(arg_cnt);
    }
}
