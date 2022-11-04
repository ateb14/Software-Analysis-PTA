package MyAnalysis;
import pascal.taie.World;
import pascal.taie.analysis.*;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AnalysisException;

import java.util.List;
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

        ClassHierarchy class_hierarchy = world.getClassHierarchy();
        Stream<JClass> classes = class_hierarchy.allClasses();
        for(JClass class_ : classes){
        }

        return null;
    }
}
