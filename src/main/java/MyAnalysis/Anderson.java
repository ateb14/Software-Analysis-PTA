package MyAnalysis;

import pascal.taie.World;
import pascal.taie.language.classes.*;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.*;

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
    private static final boolean DEBUG = false;

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
    public final int clone_depth = 999;

    /**
     * This map is used to detect the cycles in the call graph.
     */
    private Map<String, Integer> single_path_visited_counter = new TreeMap<>();

    /**
     * Stores all the fields that appear in the program.
     * If two fields have different signatures, they are definitely not the same field.
     * If two fields have the same field signature, they have the same field name, type and base class type.
     * So, for all JField with the same signature, we only have to store one instance.
     * Why don't I use a set of Strings instead? JField contains more information,
     * such as whether it is an Instance field or a Static field, which should be treated differently.
     */
    private Map<String, JField> all_existing_fields = new TreeMap<>();



    private void PrintPTS()
    {
        System.out.println("Pointer Sets: ");
        for(String sig: pts.keySet())
        {
            System.out.println("Symbol Signature: "+sig);
            System.out.print("[");
            for(Integer i: pts.get(sig))
            {
                System.out.print(i+", ");
            }
            System.out.println("]");
        }
    }

    /**
     * Statement: LHS = RHS,
     * which means LHS set contains RHS set (RHS flows to LHS)
     * @param from The var/symbol in the RHS
     * @param to The var/symbol in the LHS
     */
    private void AddEdge(String from, String to){
        if(Objects.equals(from, to)) return; // In case the two symbols are the same thing.
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
     * Record all the classes that are declared
     */
    Set<String> class_set = new TreeSet<>();


    /**
     *  Solve the PTA
     * @param world_ ZA WARUDO!
     */
    public void Solve(World world_){
        world = world_;
        Initialize();
        try {
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
        if(DEBUG)
        {
            PrintPTS();
            for(String sig: method_counter_map.keySet())
            {
                System.out.println(sig+": clone cnt "+method_counter_map.get(sig));
            }
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

        /* Record every field. */
        for(JClass jClass: world.getClassHierarchy().allClasses().toList())
        {
            String class_name = jClass.getName();
            if(class_name.contains("com.") ||
                    class_name.contains("sun.") ||
                    class_name.contains("javax.") ||
                    class_name.contains("java.") ||
                    class_name.contains("org") ||
                    class_name.contains("jdk")
            ){
                continue;
            }
            class_set.add(class_name);
//            if(jClass.toString().contains("<java") || jClass.toString().contains("<com")) continue;
            for(JField jField: jClass.getDeclaredFields())
            {
                /* Only fields defined in the benchmark program are desired. */
//                if(!jField.toString().contains("<benchmark")) continue;
                boolean isLib = false;
                for(CharSequence str: new CharSequence[]{"<java", "<com", "<sun", "<jdk"})
                {
                    if(jField.toString().contains(str)) {isLib = true; break;}
                }
                if(isLib) continue;
                all_existing_fields.put(jField.getSignature(), jField);
            }
        }

        /* Initialize the constraints by method */
        InitConstraints(world.getMainMethod());

        if(DEBUG)
        {
            System.out.println("NewConstraints:");
            for (NewConstraint newConstraint : newConstraintList) {
                System.out.println(newConstraint.allocId + " " + newConstraint.to);
            }
            System.out.println("Queries:");
            for (Map.Entry<Integer, String> entry : queries.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
            System.out.println("Graph:");
            for (Map.Entry<String, TreeSet<String>> entry : graph.entrySet()) {
                System.out.println(entry.getKey() + " flows to:");
                for (String to : entry.getValue()) {
                    System.out.println("    " + to);
                }
                System.out.print("\n");
            }
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

        if(DetectCycle(method)){
            return;
        }

        /* Try to clone the methods */
        int cur_clone_depth = Clone(method);

        /* Traverse all the statements */
        List<Stmt> statements = method.getIR().getStmts();
        int allocId = 0;
//        for(Stmt statement : statements) {
//            System.out.println(statement);
//        }
//        System.out.println("###########");
        for(Stmt statement : statements){
            if (statement instanceof Invoke invoke_stmt){
                JMethod newMethod = invoke_stmt.getMethodRef().resolve();
                if (isLibrary(newMethod)) continue; // Important!!!!!!!!!!!!!!!!!!!!!!!!!
                String signature = invoke_stmt.getMethodRef().toString();
                if(DEBUG) System.out.println("The invoked method is: "+signature);

                // These statements may throw exceptions if the argument is not a constant, handle them?
                if (signature.contains("<benchmark.internal.Benchmark: void alloc(int)>") ||
                        signature.contains("<benchmark.internal.BenchmarkN: void alloc(int)>")
                ){
                    allocId =  Integer.parseInt(invoke_stmt.getInvokeExp().getArg(0).getConstValue().toString());
                    continue;
                } else if(signature.contains("<benchmark.internal.Benchmark: void test(int,java.lang.Object)>") ||
                        signature.contains("<benchmark.internal.BenchmarkN: void test(int,java.lang.Object)>")
                ){
                    List<Var> args = invoke_stmt.getInvokeExp().getArgs();
                    queries.put(
                                Integer.parseInt(args.get(0).getConstValue().toString()), // allocId
                                GenMySignature(args.get(1), cur_clone_depth) // Var
                            );
                    continue;
                }

                // deal with the remaining invoke statements
                DealWithInvokeStatement(invoke_stmt, cur_clone_depth);

            } else if (statement instanceof New new_stmt) {
                if(allocId == 0) continue;
                /*
                 New -> Var = NewExp; NewExp -> NewInstance | NewArray | NewMultiArray.
                 NewInstance -> new ClassType
                 For Tai-e, the Var is always temp$k?
                 If we know the clone cnt of the init() function,
                 we could know the signature of %this symbol in the init() function.
                */

                AddNewConstraints(GenMySignature(new_stmt.getLValue(), cur_clone_depth), allocId); // map temp$k(heap var) to allocId
                AddEdge(Integer.toString(allocId), GenMySignature(new_stmt.getLValue(),cur_clone_depth));
                all_allocIds.add(allocId);
                allocId = 0; // No need? There may be many new() statements in an init() function.
            } else if (statement instanceof Cast cast_stmt){
                Var lhsVar = cast_stmt.getLValue();
                CastExp castExp = cast_stmt.getRValue();
                Var rhsVar = castExp.getValue();
                String lhsSig = GenMySignature(lhsVar, cur_clone_depth);
                String rhsSig = GenMySignature(rhsVar, cur_clone_depth);
                AddEdge(rhsSig, lhsSig);
                for(JField field: getAllFields(new Var[]{lhsVar, rhsVar}))
                {
                    AddEdge(
                            GenSynthesizedFieldSignature(rhsSig, field),
                            GenSynthesizedFieldSignature(lhsSig, field)
                    );
                    AddEdge(
                            GenSynthesizedFieldSignature(lhsSig, field),
                            GenSynthesizedFieldSignature(rhsSig, field)
                    );
                }
            } else if (statement instanceof Copy copy_stmt){
                /**
                 * Copy -> Var = Var;
                 *
                 * (Field Unroll Once)
                 * statement:
                 *  a = b;
                 * which means:
                 *  a contains b
                 *  a.f = b.f
                 */
                Var lhsVar = copy_stmt.getLValue(), rhsVar = copy_stmt.getRValue();
                String lhsSig = GenMySignature(lhsVar, cur_clone_depth);
                String rhsSig = GenMySignature(rhsVar, cur_clone_depth);
                /* a contains b */
                AddEdge(
                        rhsSig,
                        lhsSig
                );
                /* a.f = b.f for each f in b's fields. */
                for(JField field: getAllFields(new Var[]{lhsVar, rhsVar}))
                {
                    AddEdge(
                            GenSynthesizedFieldSignature(rhsSig, field),
                            GenSynthesizedFieldSignature(lhsSig, field)
                    );
                    AddEdge(
                            GenSynthesizedFieldSignature(lhsSig, field),
                            GenSynthesizedFieldSignature(rhsSig, field)
                    );
                    // Hopefully right!
                }
            } else if (statement instanceof LoadField lf_stmt){
                /**
                 * @// TODO: Implement unrolling many times @xhz
                 *
                 * LoadField -> Var = FieldAccess;
                 * FieldAccess -> InstanceFieldAccess | StaticFieldAccess
                 *
                 * (Field Unroll Once)
                 * statement:
                 *  a = b.f;
                 * which means:
                 *  a contains b.f
                 *  a.f = b.f
                 *
                 * L value is a(Var), R value is b.f(FieldAccess)
                 */
                Var lhsVar = lf_stmt.getLValue();
                String lhsSig = GenMySignature(lhsVar, cur_clone_depth);
                FieldAccess rhsField = lf_stmt.getRValue();

                /* a contains b.f */
                AddEdge(
                        GenMySignature(rhsField, cur_clone_depth),
                        lhsSig
                );
                /* a.f = b.f, double edge */
                AddEdge(
                        GenSynthesizedFieldSignature(lhsSig, rhsField.getFieldRef().resolve()),
                        GenMySignature(rhsField, cur_clone_depth)
                );
                AddEdge(
                        GenMySignature(rhsField, cur_clone_depth),
                        GenSynthesizedFieldSignature(lhsSig, rhsField.getFieldRef().resolve())
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
                String rhsSig = GenMySignature(rhsVar, cur_clone_depth);
                /* a.f contains b */
                AddEdge(
                        rhsSig,
                        GenMySignature(lhsField, cur_clone_depth)
                );
                /* a.f = b.f, double edge */
                AddEdge(
                        GenMySignature(lhsField, cur_clone_depth),
                        GenSynthesizedFieldSignature(rhsSig, lhsField.getFieldRef().resolve())
                );
                AddEdge(
                        GenSynthesizedFieldSignature(rhsSig, lhsField.getFieldRef().resolve()),
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
     * Deal with an invoke statement
     * @param invoke_stmt an arbitrary invoke statement
     * @param cur_clone_depth as it shows
     */
    private void DealWithInvokeStatement(Invoke invoke_stmt, int cur_clone_depth){
        if(DEBUG) System.out.println("Dealing with statement "+invoke_stmt.toString());
        Var resultVar = invoke_stmt.getLValue(); // May be null if return Var not received, but OK!
        InvokeExp invokeExp = invoke_stmt.getInvokeExp();
        Var base = null;

        if(invokeExp instanceof InvokeInstanceExp instExp)
        {
            base = instExp.getBase();
        }
        else // instance of InvokeStatic
        {
//            DealWithNonAbstractInvokeStatement(
//                invoke_stmt.getInvokeExp().getMethodRef().resolve(),
//                    null,
//                    invoke_stmt.getInvokeExp().getArgs(),
//                    resultVar,
//                    cur_clone_depth
//            );
        }

        JMethod invokedMethod = invokeExp.getMethodRef().resolve();
        if(DEBUG) System.out.println("Finding overriding methods of method "+invokedMethod.getSignature());
        JClass declaringClass = invokeExp.getMethodRef().getDeclaringClass();

        /* Deal with the method itself. In case all the subclasses have no overriding. */
        if(!invokedMethod.isAbstract()) DealWithNonAbstractInvokeStatement(
                invokedMethod, base, invokeExp.getArgs(), resultVar, cur_clone_depth);
        /**
         * Should consider all the subclasses' methods, for:
         * - not only may the invokedMethod be abstract,
         * - but also may the invokedMethod be overridden by subclasses' methods.
         */
        for(JClass subClass: world.getClassHierarchy().getAllSubclassesOf(declaringClass))
        {
            if(invokedMethod.getSignature().contains("void <init>")) break; // Brutally skip init functions.
            if(subClass.toString()==declaringClass.toString()) continue;
            // Better not deal with declaringClass twice!
            /* Using SubSignature, I can find overriding methods. */
            JMethod subMethod = subClass.getDeclaredMethod(invokedMethod.getSubsignature());
            if(subMethod==null) continue; // The subClass did not override this method.
            if(subMethod.isAbstract()) continue; // Skip abstract methods.
            if(DEBUG) System.out.println("    finds: "+subMethod.getSignature());
            DealWithNonAbstractInvokeStatement(subMethod, base, invokeExp.getArgs(), resultVar, cur_clone_depth);
        }
        if(DEBUG) System.out.println("Finding results end. ");
    }

    /**
     *  Deal with the CERTAIN invoke statements, i.e. it's not abstract
     * @param cur_clone_depth as it shows
     */
    private void DealWithNonAbstractInvokeStatement(
            JMethod method,
            Var base,
            List<Var> args ,
            Var resultVar,
            int cur_clone_depth
    ){
        assert !method.isAbstract();
        if(DEBUG)
        {
            System.out.println("Dealing with invoked method: ");
            System.out.println("    Sig: "+method.getSignature());
            System.out.println("    base instance: "+GenMySignature(base, cur_clone_depth));
            System.out.println("    result stored in: "+GenMySignature(resultVar, cur_clone_depth));
            System.out.println("    isStatic: "+method.isStatic());
        }

        if(!method.isStatic()) {
            /**
             * If this function belongs to a Var, then %this should be connected with this Var.
             * (Important!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!)
             */
            Var this_ = method.getIR().getThis();

            String baseSig = GenMySignature(base, cur_clone_depth);
            String thisSig = GenMySignature(this_, getNewInvokeCloneID(method));

            AddEdge(
                    baseSig,
                    thisSig
            );
//                    AddEdge(
//                            GenMySignature(this_, cur_clone_depth),
//                            GenMySignature(base, cur_clone_depth)
//                    );
            for (JField field : getAllFields(new Var[]{base, this_})) {
                AddEdge(
                        GenSynthesizedFieldSignature(baseSig, field),
                        GenSynthesizedFieldSignature(thisSig, field)
                );
                AddEdge(
                        GenSynthesizedFieldSignature(thisSig, field),
                        GenSynthesizedFieldSignature(baseSig, field)
                );
                // Hopefully right!
            }
        }
        int arg_cnt = 0;
        for(Var arg : args){
            /**
             * Passed args are treated the same as copy statements:
             *  formalArg = arg;
             * which means:
             *  formalArg contains arg
             *  formalArg.f = arg.f, for each field f.
             */
            Var formalArg = method.getIR().getParam(arg_cnt);
            String formalArgSig = FetchFormalArgSignature(arg_cnt, method); // Inside the function!
            String argSig = GenMySignature(arg, cur_clone_depth);
            if(DEBUG) System.out.println("Arg: "+argSig);
            if(DEBUG) System.out.println("Formal Arg: "+formalArgSig);
            AddEdge(
                    argSig,
                    formalArgSig
            );
            /* a.f = b.f for each f in b's fields. */
            for(JField field: getAllFields(new Var[]{arg, formalArg}))
            {
                AddEdge(
                        GenSynthesizedFieldSignature(argSig, field),
                        GenSynthesizedFieldSignature(formalArgSig, field)
                );
                AddEdge(
                        GenSynthesizedFieldSignature(formalArgSig, field),
                        GenSynthesizedFieldSignature(argSig, field)
                );
                // Hopefully right!
            }
            ++ arg_cnt;
        }

        // Recursively construct...
        // it may throw an exception if the resolving process fails
        InitConstraints(method);


        // receive the return value
        // invoke -> var = InvokeExp
        if(resultVar!=null){
            int ret_num = method.getIR().getReturnVars().size();
            for(int i=0;i<ret_num;++i) {
                /**
                 * Return values are treated the same as copy statements:
                 *  result = retVal;
                 * which means
                 *  result contains retVal
                 *  result.f = retVal.f, for each field f.
                 */
                Var returnVar = method.getIR().getReturnVars().get(i); // Callee!
                //Var resultVar = invoke_stmt.getResult(); // Caller!
                String resultSig = GenMySignature(resultVar, cur_clone_depth); // Caller!
                String returnSig = FetchReturnSignature(i, method); // Callee!
                if(DEBUG) System.out.println("Returned Var: "+returnSig);
                if(DEBUG) System.out.println("Result Var: "+resultSig);
                AddEdge(
                        returnSig,
                        resultSig
                );
                /* a.f = b.f for each f in b's fields. */
                for(JField field: getAllFields(new Var[]{returnVar, resultVar}))
                {
                    AddEdge(
                            GenSynthesizedFieldSignature(returnSig, field),
                            GenSynthesizedFieldSignature(resultSig, field)
                    );
                    AddEdge(
                            GenSynthesizedFieldSignature(resultSig, field),
                            GenSynthesizedFieldSignature(returnSig, field)
                    );
                    // Hopefully right!
                }
            }
        }
    }

    /**
     * Utility function for getting all fields of two variables.
     *
     * @param vars Target Tai-e Vars
     * @return A Set containing the FieldAccess of each field of {@code var}.
     */
    private Set<JField> getAllFields(Var[] vars) {
        Set<JField> ans = new HashSet<>();
//        for(StoreField sf_stmt: var.getStoreFields())
//        {
//            ans.add(sf_stmt.getFieldAccess());
//        }
//        for(LoadField lf_stmt: var.getLoadFields())
//        {
//            ans.add(lf_stmt.getFieldAccess());
//        }
        for(JField field: all_existing_fields.values())
        {
            ans.add(field);
            // @TODO: Add class filter to filter some impossible fields.
        }
        if(DEBUG)
        {
            for (Var var : vars) {
                System.out.println(GenMySignature(var, 114514) + "   and");
            }
            System.out.println("    may have " + ans.size() + " active fields: ");
            for (JField field : ans) {
                System.out.println("        " + field.getSignature());
            }
        }
        return ans;
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
        if(sig.contains("<java.") || sig.contains("<com")){
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
            sig = curMethodSig + var.getName() + "(type=" + var.getType() + ")";
        } else if (exp instanceof InstanceFieldAccess iField){
            String curMethodSig = depth + iField.getBase().getMethod().getSignature();
            String baseVarName = iField.getBase().getName();
            String fieldSig = iField.getFieldRef().resolve().getSignature();
            sig = curMethodSig + baseVarName + "(type=" + iField.getBase().getType() + ")" + fieldSig;
        } else if (exp instanceof StaticFieldAccess sField) {
            sig = sField.getFieldRef().resolve().getSignature();
        } else {
            sig = null;
        }
        //System.out.println(sig);
        return sig;
    }

    /**
     * Generate a synthesized signature of a field access,
     * given a variable's existing signature and a field.
     * Even if this var does not have this field, the fake signature will be generated.
     * @param varSig The existing signature for the variable
     * @param field The wanted field
     * @return The synthesized signature of the field access
     */
    private String GenSynthesizedFieldSignature(String varSig, JField field)
    {
        String fieldSig = field.getSignature();
        if(field.isStatic())
        {
            return fieldSig;
        }
        else // Instance field
        {
            return varSig + fieldSig;
        }
    }

    /**
     * Gen
     * @param method the given method
     * @return the future clone id of the upcoming method
     */
    private int getNewInvokeCloneID(JMethod method)
    {
        int depth;
        String method_sig = method.getSignature();
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
        return depth;
    }

    /**
     * Generate a signature of the return value of a given method
     * @param ret_cnt the number of the return value
     * @param method the given method
     * @return the signature
     */
    private String FetchReturnSignature(int ret_cnt, JMethod method){
        int depth;
        String method_sig = method.getSignature();
        if(!method_counter_map.containsKey(method_sig)){ // not cloned yet
            // normally this won't happen
            throw new RuntimeException();
        } else {
            depth = method_counter_map.get(method_sig);
        }
        Var retVar = method.getIR().getReturnVars().get(ret_cnt);
//        return depth + invoke.getInvokeExp().getMethodRef().resolve().getSignature() +
//                invoke.getMethodRef().resolve().getIR().getReturnVars().get(ret_cnt);
        return GenMySignature(retVar, depth);
    }

    /**
     * Generate a signature of a formal argument of a method
     * @param arg_cnt the number of the formal argument
     * @param method the given method
     * @return the signature
     */
    private String FetchFormalArgSignature(int arg_cnt, JMethod method){
        int depth;
        String method_sig = method.getSignature();
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
        Var argVar = method.getIR().getParam(arg_cnt);
//        return depth + invoke.getInvokeExp().getMethodRef().resolve().getSignature() +
//                invoke.getInvokeExp().getMethodRef().resolve().getIR().getParam(arg_cnt) +
//                "(type=" + argVar.getType() + ")";
        return GenMySignature(argVar, depth);
    }
}
