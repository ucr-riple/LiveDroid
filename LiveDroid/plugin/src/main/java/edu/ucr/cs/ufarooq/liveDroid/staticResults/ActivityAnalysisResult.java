package edu.ucr.cs.ufarooq.liveDroid.staticResults;

import com.intellij.psi.PsiClass;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActivityAnalysisResult {
  private final Set<ViewResult> uiResults;
  private final Set<AccessPath> mayUseForEventListener;
  private final Set<AccessPath> mayModifyForEventListener;
  private Set<AccessPath> criticalData; // may update based on user selection
  private Set<AccessPath> userSelectedCriticalData;
  // private final Set<AccessPath> mayAllocForEventListener;
  private final Map<AccessPath, Set<AccessPath>> mayPointsToForEventListener;
  private final PsiClass activityName;
  private final Set<AccessPath> processedUsingPointsTo;

  public ActivityAnalysisResult(
      PsiClass activityName,
      Set<ViewResult> uiResults,
      Set<AccessPath> criticalData,
      Set<AccessPath> mayUseForEventListener,
      Set<AccessPath> mayModifyForEventListener, /*Set<AccessPath> mayAllocForEventListener,*/
      Map<AccessPath, Set<AccessPath>> mayPointsToForEventListener) {
    this.uiResults = uiResults;
    this.mayUseForEventListener = mayUseForEventListener;
    this.mayModifyForEventListener = mayModifyForEventListener;
    // this.mayAllocForEventListener = mayAllocForEventListener;
    this.mayPointsToForEventListener = mayPointsToForEventListener;
    this.activityName = activityName;
    this.criticalData = criticalData;
    this.userSelectedCriticalData = new HashSet<>();
    Set<AccessPath> temp = new HashSet<>();
    mayPointsToForEventListener.forEach(
        (k, v) -> {
          System.out.println(k.getKey() + ":" + v);
          temp.addAll(v);
        });

    this.processedUsingPointsTo = temp;
  }

  public Set<ViewResult> getUiResults() {
    return uiResults;
  }

  public Set<AccessPath> getMayUseForEventListener() {
    return mayUseForEventListener;
  }

  public Set<AccessPath> getCriticalData() {
    return criticalData;
  }

  public void setCriticalData(Set<AccessPath> criticalData) {
    this.criticalData = criticalData;
  }

  public void addUserSelection(AccessPath accessPath) {
    this.userSelectedCriticalData.add(accessPath);
  }

  public Set<AccessPath> getUserSelectedCriticalData() {
    return userSelectedCriticalData;
  }

  public Set<AccessPath> getMayModifyForEventListener() {
    return mayModifyForEventListener;
  }

  //    public Set<AccessPath> getMayAllocForEventListener() {
  //        return mayAllocForEventListener;
  //    }

  public Map<AccessPath, Set<AccessPath>> getMayPointsToForEventListener() {
    return mayPointsToForEventListener;
  }

  public PsiClass getActivityName() {
    return activityName;
  }

  public Set<AccessPath> getProcessedUsingPointsTo() {
    return processedUsingPointsTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ActivityAnalysisResult that = (ActivityAnalysisResult) o;

    if (uiResults != null ? !uiResults.equals(that.uiResults) : that.uiResults != null)
      return false;
    if (criticalData != null ? !criticalData.equals(that.criticalData) : that.criticalData != null)
      return false;
    if (mayUseForEventListener != null
        ? !mayUseForEventListener.equals(that.mayUseForEventListener)
        : that.mayUseForEventListener != null) return false;
    if (mayModifyForEventListener != null
        ? !mayModifyForEventListener.equals(that.mayModifyForEventListener)
        : that.mayModifyForEventListener != null) return false;
    //        if (mayAllocForEventListener != null ?
    // !mayAllocForEventListener.equals(that.mayAllocForEventListener) :
    // that.mayAllocForEventListener != null)
    //            return false;
    if (mayPointsToForEventListener != null
        ? !mayPointsToForEventListener.equals(that.mayPointsToForEventListener)
        : that.mayPointsToForEventListener != null) return false;
    return activityName != null
        ? activityName.equals(that.activityName)
        : that.activityName == null;
  }

  @Override
  public int hashCode() {
    int result = uiResults != null ? uiResults.hashCode() : 0;
    result = 31 * result + (mayUseForEventListener != null ? mayUseForEventListener.hashCode() : 0);
    result = 31 * result + (criticalData != null ? criticalData.hashCode() : 0);
    result =
        31 * result
            + (mayModifyForEventListener != null ? mayModifyForEventListener.hashCode() : 0);
    // result = 31 * result + (mayAllocForEventListener != null ?
    // mayAllocForEventListener.hashCode() : 0);
    result =
        31 * result
            + (mayPointsToForEventListener != null ? mayPointsToForEventListener.hashCode() : 0);
    result = 31 * result + (activityName != null ? activityName.hashCode() : 0);
    return result;
  }
}
