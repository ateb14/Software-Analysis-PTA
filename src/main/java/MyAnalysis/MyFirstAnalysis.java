package MyAnalysis;
import pascal.taie.World;
import pascal.taie.analysis.*;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class MyFirstAnalysis extends ProgramAnalysis<Set<Stmt>> {
    public static final String ID = "MyFirstTest";

    public MyFirstAnalysis(AnalysisConfig config){
        super(config);
    }

    @Override
    public Set<Stmt> analyze(){
        System.out.println("");
//        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
//        JMethod method = cfg.getMethod();
//        System.out.println("Here is the signature:\n");
//        System.out.println(method.getSubsignature().toString());
//        System.out.println("Here are the vars:\n");
//        List<Var> vars = ir.getVars();
//        for(Var var : vars){
//            if(var.getName().contains("temp$")){
//                continue;
//            }
//            System.out.println(var.getName());
//            System.out.println(var.getType());
//        }
//        System.out.flush();
        World world = World.get();
        JMethod main_method = world.getMainMethod();
        System.out.println("Here is the signature of the main method:");
        System.out.println(main_method.getSubsignature().toString());


        System.out.println("Now we're going through the statements of the main method:");
        IR main_ir = main_method.getIR();

        for(Stmt statement : main_ir.getStmts()){

            // call statements
            // System.out.println(statement);
            if(statement instanceof Invoke invoke) {
                MethodRef m_ref = invoke.getMethodRef();
                System.out.println("Invoke:\n    " + m_ref.toString());
                if (invoke.getLValue() != null) {
                    System.out.println("    Lvalue name:" + invoke.getLValue().getName());
                }
                System.out.println("    Args:");
                for(Var arg : invoke.getInvokeExp().getArgs()){
                    System.out.println("        " + arg.getType() + " " + arg.getName());
                }
            } else if (statement instanceof New new_stmt) {
                System.out.println("New:\n    " + new_stmt);
            } else if (statement instanceof AssignLiteral asl_stmt) {
                System.out.println("Assign Literal:\n    " + asl_stmt);
                System.out.println("    L:" + asl_stmt.getLValue());
                System.out.println("    R:" + asl_stmt.getRValue());
            } else if (statement instanceof Binary b_stmt) {
                System.out.println("Assign Binary:\n    " + b_stmt);
            } else if (statement instanceof StoreField sf_stmt){
                System.out.println("Store field:\n    " + sf_stmt);
                System.out.println("    L:" + sf_stmt.getLValue().toString());
                System.out.println("    R:" + sf_stmt.getRValue().toString());
            } else if (statement instanceof LoadField lf_stmt){
                System.out.println("Load field:\n    " + lf_stmt);
                System.out.println("    L:" + lf_stmt.getLValue().toString());
                System.out.println("    R:" + lf_stmt.getRValue().toString());
            }
        }

        System.out.println("###############\n");
        return null;
    }
}
