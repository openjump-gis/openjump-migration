package com.vividsolutions.jump.workbench.ui.plugin.analysis;

import java.util.*;
import com.vividsolutions.jump.task.*;


import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jump.feature.*;

/**
 * Exceutes a spatial query with a given mask FeatureCollection, source FeatureCollection,
 * and predicate.
 * Ensures result does not contain duplicates.
 *
 * @author Martin Davis
 * @version 1.2
 */
public class SpatialJoinExecuter
{
  private FeatureCollection srcAFC;
  private FeatureCollection srcBFC;

  private FeatureCollection queryFC;

  private boolean isExceptionThrown = false;

  private Geometry geoms[] = new Geometry[2];
  private Set resultSet = new HashSet();

  public SpatialJoinExecuter(FeatureCollection srcAFC, FeatureCollection srcBFC)
  {
    this.srcAFC = srcAFC;
    this.srcBFC = srcBFC;
  }

  /**
   * Gets the feature collection to query.
   * A spatial index may be created if this would improve performance.
   *
   * @param func
   * @return
   */
  private void createQueryFeatureCollection(GeometryPredicate pred)
  {
    boolean buildIndex = false;
    if (srcAFC.size() > 10) buildIndex = true;
    if (srcBFC.size() > 100) buildIndex = true;
    if (pred instanceof GeometryPredicate.DisjointPredicate) buildIndex = false;

    if (buildIndex) {
      queryFC = new IndexedFeatureCollection(srcBFC);
    }
    else {
      queryFC = srcBFC;
    }
  }

  private Iterator query(GeometryPredicate pred, double[] params, Geometry gMask)
  {
    Envelope queryEnv = gMask.getEnvelopeInternal();
    // special hack for withinDistance
    if (pred instanceof GeometryPredicate.WithinDistancePredicate) {
      queryEnv.expandBy(params[0]);
    }

    boolean useQuery = true;
    if (pred instanceof GeometryPredicate.DisjointPredicate) useQuery = false;

    Iterator queryIt = null;
    if (useQuery) {
      Collection queryResult = queryFC.query(queryEnv);
      queryIt = queryResult.iterator();
    }
    else {
      queryIt = queryFC.iterator();
    }
    return queryIt;
  }

  public boolean isExceptionThrown() { return isExceptionThrown; }

  private FeatureSchema createResultSchema()
  {
	FeatureSchema resultFS = new FeatureSchema();
	resultFS.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
	copyAttributesToSchema(srcAFC.getFeatureSchema(), "A_", resultFS);
	copyAttributesToSchema(srcBFC.getFeatureSchema(), "B_", resultFS);
	return resultFS;
  }
  
  private void copyAttributesToSchema(FeatureSchema srcFS, 
		  String prefix, 
		  FeatureSchema resultFS)
  {
	  for (int i = 0; i < srcFS.getAttributeCount(); i++) {
		  if (srcFS.getAttributeType(i) != AttributeType.GEOMETRY) {
			  resultFS.addAttribute(prefix + srcFS.getAttributeName(i),
					  srcFS.getAttributeType(i));
		  }
	  }
  }
  
  public FeatureCollection getResultFC()
  {
    return new FeatureDataset(createResultSchema());
  }

  private boolean isInResult(Feature f)
  {
    return resultSet.contains(f);
  }


  /**
   * Computes geomSrc.func(geomMask)
   *
   * @param monitor
   * @param func
   * @param params
   * @param resultFC
   */
  public void execute(TaskMonitor monitor,
                                     GeometryPredicate func,
                                     double[] params,
                                     FeatureCollection resultFC
                                     )
  {
    createQueryFeatureCollection(func);

    int total = srcAFC.size();
    int count = 0;
    for (Iterator iMask = srcAFC.iterator(); iMask.hasNext(); ) {

      monitor.report(count++, total, "features");
      if (monitor.isCancelRequested()) return;

      Feature fMask = (Feature) iMask.next();
      Geometry gMask = fMask.getGeometry();

      Iterator queryIt = query(func, params, gMask);
      for (; queryIt.hasNext(); ) {
        Feature fSrc = (Feature) queryIt.next();

        // optimization - if feature already in result no need to re-test
        if (isInResult(fSrc))
          continue;

        Geometry gSrc = fSrc.getGeometry();
        geoms[0] = gSrc;
        geoms[1] = gMask;
        boolean isInResult = isTrue(func, gSrc, gMask, params);

        if (isInResult) {
          addToResult(fSrc, fMask, resultFC);
        }
      }
    }
  }

  private void addToResult(Feature fA, Feature fB, FeatureCollection resultFC)
  {
	Feature fResult = new BasicFeature(resultFC.getFeatureSchema());
	// for now just use A's geometry - in future make a geometry pair
	fResult.setGeometry(fA.getGeometry());
//	copyAttributesToFeature(fA, "A_", fResult);
//	copyAttributesToFeature(fB, "B_", fResult);
//  Ed Deen: Switched the above such that the right prefixes are used 
//		     for the right features attribute names.	
	copyAttributesToFeature(fA, "B_", fResult);
	copyAttributesToFeature(fB, "A_", fResult);
    resultFC.add(fResult);
  }

  private void copyAttributesToFeature(Feature fSrc, 
		  String prefix, 
		  Feature fResult)
  {
	  FeatureSchema srcFS = fSrc.getSchema();
	  for (int i = 0; i < srcFS.getAttributeCount(); i++) {
		  if (srcFS.getAttributeType(i) != AttributeType.GEOMETRY) {
			  fResult.setAttribute(prefix + srcFS.getAttributeName(i),
					  fSrc.getAttribute(i));
		  }
	  }
  }
  private boolean isTrue(GeometryPredicate func, Geometry geom0, Geometry geom1, double[] params)
  {
    try {
      return func.isTrue(geom0, geom1, params);
    }
    catch (RuntimeException ex) {
      // simply eat exceptions and report them by returning null
      isExceptionThrown = true;
    }
    return false;

  }

}