package pascal.taie.analysis.pta.toolkit.scaler;

import pascal.taie.analysis.pta.toolkit.PointerAnalysisResultEx;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.Map;

import static java.util.function.Predicate.not;

/**
 * This class computes (estimates) the number of contexts for given method
 * when using corresponding context sensitivity variant.
 */
abstract class ContextComputer {

    final PointerAnalysisResultEx pta;

    /**
     * Map from a method to its context number.
     */
    final Map<JMethod, Integer> method2ctxNumber = Maps.newMap();

    ContextComputer(PointerAnalysisResultEx pta) {
        this.pta = pta;
        computeContext();
    }

    private void computeContext() {
        pta.getBase()
                .getCallGraph()
                .reachableMethods()
                .filter(not(JMethod::isStatic))
                .forEach(m -> method2ctxNumber.put(m, computeContextNumberOf(m)));
    }

    /**
     * @return the number of contexts of the given method.
     */
    int contextNumberOf(JMethod method) {
        return method2ctxNumber.get(method);
    }

    /**
     * @return name of the context sensitivity variant.
     */
    abstract String getVariantName();

    /**
     * Computes (estimates) the number of contexts for the given method
     * using the context sensitivity variant.
     */
    abstract int computeContextNumberOf(JMethod method);
}