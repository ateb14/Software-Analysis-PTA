package MyAnalysis;

import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.Set;

public class MyPTA extends ProgramAnalysis<PTAResult> {
    public static final String ID = "my_pta";
    public MyPTA(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PTAResult analyze() {
        Anderson anderson = new Anderson();
        anderson.Solve(World.get());
        return null;
    }
}

