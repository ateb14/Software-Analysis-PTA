/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.config;

import pascal.taie.analysis.graph.callgraph.CallGraphBuilder;
import pascal.taie.util.collection.Lists;
import pascal.taie.util.graph.Graph;
import pascal.taie.util.graph.SCC;
import pascal.taie.util.graph.SimpleGraph;
import pascal.taie.util.graph.TopoSorter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.Sets.newSet;

/**
 * Makes analysis plan based on given plan configs and analysis configs.
 */
public class AnalysisPlanner {

    private final ConfigManager manager;

    public AnalysisPlanner(ConfigManager manager) {
        this.manager = manager;
    }

    /**
     * This method makes a plan by converting given list of PlanConfig
     * to AnalysisConfig. It will be used when analysis plan is specified
     * by configuration file.
     *
     * @return the analysis plan consists of a list of analysis config.
     * @throws ConfigException if the given planConfigs are invalid.
     */
    public List<AnalysisConfig> makePlan(List<PlanConfig> planConfigs,
                                         boolean reachableScope) {
        List<AnalysisConfig> plan = covertConfigs(planConfigs);
        validatePlan(plan, reachableScope);
        return plan;
    }

    /**
     * Converts a list of PlanConfigs to the list of corresponding AnalysisConfigs.
     */
    private List<AnalysisConfig> covertConfigs(List<PlanConfig> planConfigs) {
        return planConfigs.stream()
                .map(pc -> manager.getConfig(pc.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if the given analysis plan is valid.
     *
     * @param plan           the given analysis plan
     * @param reachableScope whether the analysis scope is set to reachable
     * @throws ConfigException if the given plan is invalid
     */
    private void validatePlan(List<AnalysisConfig> plan, boolean reachableScope) {
        // check if all required analyses are placed in front of
        // their requiring analyses
        for (int i = 0; i < plan.size(); ++i) {
            AnalysisConfig config = plan.get(i);
            for (AnalysisConfig required : manager.getRequiredConfigs(config)) {
                int rindex = plan.indexOf(required);
                if (rindex == -1) {
                    // required analysis is missing
                    throw new ConfigException(String.format(
                            "'%s' is required by '%s' but missing in analysis plan",
                            required, config));
                } else if (rindex >= i) {
                    // invalid analysis order: required analysis runs
                    // after current analysis
                    throw new ConfigException(String.format(
                            "'%s' is required by '%s' but it runs after '%s'",
                            required, config, config));
                }
            }
        }
        if (reachableScope) { // analysis scope is set to reachable
            // check if given analyses include call graph builder
            AnalysisConfig cg = Lists.findFirst(plan, AnalysisPlanner::isCG);
            if (cg == null) {
                throw new ConfigException(String.format("Scope is reachable" +
                                " but call graph builder (%s) is not given in plan",
                        CallGraphBuilder.ID));
            }
            // check if call graph builder is executed as early as possible
            Set<AnalysisConfig> cgRequired = manager.getAllRequiredConfigs(cg);
            for (AnalysisConfig config : plan) {
                if (config.equals(cg)) {
                    break;
                }
                if (!cgRequired.contains(config)) {
                    throw new ConfigException(String.format(
                            "Scope is reachable, thus '%s' " +
                                    "should be placed after call graph builder (%s)",
                            config, CallGraphBuilder.ID));
                }
            }
        }
    }

    private static boolean isCG(AnalysisConfig config) {
        return config.getId().equals(CallGraphBuilder.ID);
    }

    /**
     * This method makes an analysis plan based on given plan configs,
     * and it will automatically add required analyses (which are not in
     * the given plan) to the resulting plan.
     * It will be used when analysis plan is specified by command line options.
     *
     * @return the analysis plan consisting of a list of analysis config.
     * @throws ConfigException if the specified planConfigs is invalid.
     */
    public List<AnalysisConfig> expandPlan(List<PlanConfig> planConfigs,
                                           boolean reachableScope) {
        List<AnalysisConfig> configs = covertConfigs(planConfigs);
        if (reachableScope) { // complete call graph builder
            AnalysisConfig cg = Lists.findFirst(configs, AnalysisPlanner::isCG);
            if (cg == null) {
                // if analysis scope is reachable and call graph builder is
                // not given, then we automatically add it
                configs.add(manager.getConfig(CallGraphBuilder.ID));
            }
        }
        Graph<AnalysisConfig> graph = buildRequireGraph(configs);
        validateRequireGraph(graph);
        List<AnalysisConfig> plan = new TopoSorter<>(graph, configs).get();
        return reachableScope ? shiftCG(plan) : plan;
    }

    /**
     * Shifts call graph builder (cg) in given plan to ensure that
     * it will run before all the analyses that it does not require.
     */
    private List<AnalysisConfig> shiftCG(List<AnalysisConfig> plan) {
        AnalysisConfig cg = Lists.findFirst(plan, AnalysisPlanner::isCG);
        Set<AnalysisConfig> required = manager.getAllRequiredConfigs(cg);
        List<AnalysisConfig> notRequired = new ArrayList<>();
        // obtain the analyses that run before cg but not required by cg
        for (AnalysisConfig c : plan) {
            if (c.equals(cg)) {
                break;
            }
            if (!required.contains(c)) {
                notRequired.add(c);
            }
        }
        List<AnalysisConfig> result = new ArrayList<>(plan.size());
        // add analyses that are required by cg
        for (AnalysisConfig c : plan) {
            if (required.contains(c)) {
                result.add(c);
            }
            if (c.equals(cg)) { // found cg, break
                break;
            }
        }
        result.add(cg); // add cg
        // add analyses that are not required by cg but placed before cg
        // in the original plan
        result.addAll(notRequired);
        // add remaining analyses
        for (int i = plan.indexOf(cg) + 1; i < plan.size(); ++i) {
            result.add(plan.get(i));
        }
        return result;
    }

    /**
     * Builds a require graph for AnalysisConfigs.
     * This method traverses relevant AnalysisConfigs starting from the ones
     * specified by given configs. During the traversal, if it finds that
     * analysis A1 is required by A2, then it adds an edge A1 -> A2 and
     * nodes A1 and A2 to the resulting graph.
     * <p>
     * The resulting graph contains the given analyses (planConfigs) and
     * all their (directly and indirectly) required analyses.
     */
    private Graph<AnalysisConfig> buildRequireGraph(List<AnalysisConfig> configs) {
        SimpleGraph<AnalysisConfig> graph = new SimpleGraph<>();
        Set<AnalysisConfig> visited = newSet();
        Queue<AnalysisConfig> workList = new ArrayDeque<>(configs);
        while (!workList.isEmpty()) {
            AnalysisConfig config = workList.poll();
            graph.addNode(config);
            visited.add(config);
            manager.getRequiredConfigs(config).forEach(required -> {
                graph.addEdge(required, config);
                if (!visited.contains(required)) {
                    workList.add(required);
                }
            });
        }
        return graph;
    }

    /**
     * Checks if the given require graph is valid.
     *
     * @throws ConfigException if the given plan is invalid
     */
    private void validateRequireGraph(Graph<AnalysisConfig> graph) {
        // Check if the require graph is self-contained, i.e., every required
        // analysis is included in the graph
        graph.forEach(config -> {
            List<AnalysisConfig> missing = manager.getRequiredConfigs(config)
                    .stream()
                    .filter(Predicate.not(graph::hasNode))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                throw new ConfigException("Invalid analysis plan: " +
                        missing + " are missing");
            }
        });
        // Check if the require graph contains cycles
        SCC<AnalysisConfig> scc = new SCC<>(graph);
        if (!scc.getTrueComponents().isEmpty()) {
            throw new ConfigException("Invalid analysis plan: " +
                    scc.getTrueComponents() + " are mutually dependent");
        }
    }
}
