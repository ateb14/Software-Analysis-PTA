package MyAnalysis;

import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.stmt.Stmt;

import java.util.Set;

public class MyPTA extends ProgramAnalysis<> {

    public MyPTA(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        return null;
    }
}
