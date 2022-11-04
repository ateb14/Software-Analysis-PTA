package MyAnalysis;

import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Stmt;

import java.util.Set;

public class MyPTA extends ProgramAnalysis<PTAResult> {

    public MyPTA(AnalysisConfig config) {
        super(config);
    }

    @Override
    public PTAResult analyze() {
        return null;
    }
}
