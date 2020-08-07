package edu.ucr.cs.ufarooq.visualization;

import edu.ucr.cs.ufarooq.providers.CallReturnEdgesInfo;
import edu.ucr.cs.ufarooq.providers.CallReturnEdgesProvider;
import heros.InterproceduralCFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.toolkits.ide.JimpleIFDSSolver;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.util.dot.*;

import java.util.*;

public class HeroBackwardsICFGDotVisualizer {
    private static final Logger logger = LoggerFactory.getLogger(HeroBackwardsICFGDotVisualizer.class);
    private final DotGraphAttribute headAttr;
    private final DotGraphAttribute tailAttr;
    private Collection<String> exitNodes, entryNodes;
    private DotGraph dotIcfg = new DotGraph("");
    private ArrayList<Unit> visited = new ArrayList<Unit>();
    private JimpleIFDSSolver<?, InterproceduralCFG<Unit, SootMethod>> solver;
    private ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
    String fileName;
    Unit startPoint;
    InterproceduralCFG<Unit, SootMethod> icfg;
    BackwardsInterproceduralCFG backwardICFG;


    public HeroBackwardsICFGDotVisualizer(JimpleIFDSSolver<?, InterproceduralCFG<Unit, SootMethod>> solver, String fileName, Unit startPoint, InterproceduralCFG<Unit, SootMethod> icfg, BackwardsInterproceduralCFG backwardICFG) {

        this.solver = solver;
        this.fileName = fileName;
        this.startPoint = startPoint;
        this.backwardICFG = backwardICFG;
        this.icfg = icfg;
        if (this.fileName == null || this.fileName == "") {
            System.out.println("Please provide a vaid filename");
        }
        if (this.startPoint == null) {
            System.out.println("startPoint is null!");
        }
        if (this.icfg == null) {
            System.out.println("ICFG is null!");
        }
        dotIcfg.setNodeShape(DotGraphConstants.NODE_SHAPE_BOX);

        headAttr = new DotGraphAttribute("fillcolor", "gray");
        tailAttr = new DotGraphAttribute("fillcolor", "lightgray");
        exitNodes = new ArrayList<>();
        entryNodes = new ArrayList<>();

    }

    /**
     * For the given file name, export the ICFG into DOT format. All parameters initialized through the constructor
     */

    public void exportToDot() {
        if (this.startPoint != null && this.icfg != null && this.fileName != null) {
            SootMethod method = icfg.getMethodOf(startPoint);
            entryNodes.add(getNodeLabel(method, startPoint));

            graphTraverse(this.startPoint, this.icfg);
            postProcessing();
            dotIcfg.plot(this.fileName);
            logger.debug("" + fileName + DotGraph.DOT_EXTENSION);
        } else {
            System.out.println("Parameters not properly initialized!");
        }

    }

    private void graphTraverse(Unit startPoint, InterproceduralCFG<Unit, SootMethod> icfg) {
        List<Unit> currentSuccessors = icfg.getSuccsOf(startPoint);

        if (currentSuccessors.size() == 0) {
            System.out.println("Traversal complete");
            return;
        } else {
            for (Unit succ : currentSuccessors) {
                System.out.println("Succesor: " + succ.toString());
                if (!visited.contains(succ)) {
                    SootMethod startMethod = icfg.getMethodOf(startPoint);
                    String succNode = getNodeLabel(startMethod, succ);
                    dotIcfg.drawEdge(getNodeLabel(startMethod, startPoint), succNode);
                    if (icfg.isExitStmt(succ))
                        exitNodes.add(succNode);
                    visited.add(succ);
                    graphTraverse(succ, icfg);
                    if (!methods.contains(startMethod))
                        AddCallAndReturnEdges(startMethod, icfg);
                } else {
                    SootMethod startMethod = icfg.getMethodOf(startPoint);
                    dotIcfg.drawEdge(getNodeLabel(startMethod, startPoint), getNodeLabel(startMethod, succ));
                }
            }
        }
    }

    private void AddCallAndReturnEdges(SootMethod sootMethod, InterproceduralCFG<Unit, SootMethod> icfg) {
        Set<Unit> calls = icfg.getCallsFromWithin(sootMethod);
        for (Unit call : calls) {
            CallReturnEdgesInfo callReturnEdgeInfo = CallReturnEdgesProvider.query(call);
            String color = generateColor();
            DotGraphAttribute edgeColor = getEdgeAttr("color", color);
            if (callReturnEdgeInfo != null) {
                Collection<Unit> startPoints = backwardICFG.getStartPointsOf(callReturnEdgeInfo.getCalleeMethod());
                Collection<Unit> startPointsICFG = icfg.getStartPointsOf(callReturnEdgeInfo.getCalleeMethod());
                for (Unit startPt : startPoints) {
                    String entryNode = getNodeLabel(callReturnEdgeInfo.getCalleeMethod(), startPt);
                    DotGraphEdge callEdge = dotIcfg.drawEdge(getNodeLabel(sootMethod, call), entryNode);
                    callEdge.setStyle(DotGraphConstants.EDGE_STYLE_DOTTED);
                    //callEdge.setAttribute(edgeColor);
                    callEdge.setLabel("Call:" + sootMethod.getName() + "->" + callReturnEdgeInfo.getCalleeMethod().getName());
                    entryNodes.add(entryNode);
                    visited.add(startPt);
                    //graphTraverse(startPt, backwardICFG);
                }
                for (Unit startPt : startPointsICFG) {
                    graphTraverse(startPt, icfg);
                }
                SootMethod returnSiteMethod = icfg.getMethodOf(callReturnEdgeInfo.getReturnSite());
                SootMethod exitMethod = icfg.getMethodOf(callReturnEdgeInfo.getExitStmt());
                String exitNode = getNodeLabel(exitMethod, callReturnEdgeInfo.getExitStmt());
                DotGraphEdge returnEdge = dotIcfg.drawEdge(exitNode, getNodeLabel(returnSiteMethod, callReturnEdgeInfo.getReturnSite()));
                returnEdge.setStyle(DotGraphConstants.EDGE_STYLE_DOTTED);
                //returnEdge.setAttribute(edgeColor);
                returnEdge.setLabel("Return:" + exitMethod.getName() + "->" + returnSiteMethod.getName());

                exitNodes.add(exitNode);
                visited.add(callReturnEdgeInfo.getExitStmt());
                visited.add(callReturnEdgeInfo.getReturnSite());
                //graphTraverse(callReturnEdgeInfo.getReturnSite(), icfg);


            }
        }
        methods.add(sootMethod);
    }

    public void postProcessing() {
        setStyle(entryNodes, dotIcfg, DotGraphConstants.NODE_STYLE_FILLED, headAttr);
        setStyle(exitNodes, dotIcfg, DotGraphConstants.NODE_STYLE_FILLED, tailAttr);
    }

    private String getNodeLabel(SootMethod method, Unit unit) {
        Set<?> resultAt = solver.ifdsResultsAt(unit);
        return method.getDeclaringClass().getShortName() + ":" + method.getName() + "<>" + unit.toString() + "\n" + resultAt.toString();
    }

    private Collection<Unit> getReturnStmt(SootMethod sootMethod) {
        Collection<Unit> returns = new ArrayList<>();
        Iterator<Unit> it = sootMethod.retrieveActiveBody().getUnits().snapshotIterator();
        while (it.hasNext()) {
            Unit u = it.next();
            if (u instanceof ReturnStmt) {
                ReturnStmt retStmt = (ReturnStmt) u;
                returns.add(u);
            }

        }
        return returns;
    }

    private void setStyle(Collection<String> objects, DotGraph canvas, String style,
                          DotGraphAttribute attrib) {
        // Fill the entry and exit nodes.
        for (String object : objects) {
            DotGraphNode objectNode = canvas.getNode(object);
            objectNode.setStyle(style);
            objectNode.setAttribute(attrib);
        }
    }

    public DotGraphAttribute getEdgeAttr(String id, String value) {
        DotGraphAttribute edgeAttr = new DotGraphAttribute(id, value);
        return edgeAttr;
    }

    private static String generateColor() {
        Random r = new Random(Calendar.getInstance().getTimeInMillis());

        final char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] s = new char[7];
        int n = r.nextInt(0x1000000);

        s[0] = '#';
        for (int i = 1; i < 7; i++) {
            s[i] = hex[n & 0xf];
            n >>= 4;
        }
        return new String(s);
    }
}