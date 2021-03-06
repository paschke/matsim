/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballSample.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.johannes.studies.mcmc;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.socnetgen.sna.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.analysis.*;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.*;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.Population2SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.EstimatedDegree;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SimplePiEstimator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.WSMStatsFactory;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.WaveSizeTask;
import org.matsim.contrib.socnetgen.sna.snowball.sim.Sampler;
import org.matsim.contrib.socnetgen.sna.snowball.sim.SamplerListener;
import org.matsim.contrib.socnetgen.sna.snowball.sim.SnowballSampler;
import org.matsim.contrib.socnetgen.sna.snowball.spatial.SpatialSampledGraphProjectionBuilder;
import org.matsim.contrib.socnetgen.sna.snowball.spatial.analysis.ObservedAccessibility;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class SnowballSample {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialSparseGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/ergm/raw/graph.graphml");
//		SpatialSparseGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/ergm/small/graph.graphml");
		
		Set<SimpleFeature> features = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/Kanton.shp");
		Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry();
		
		SnowballSampler<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> sampler = new SnowballSampler<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>();
		
		sampler.setBuilder(new SpatialSampledGraphProjectionBuilder<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>());
		sampler.setSeedGenerator(new SeedGenerator(geometry));
		sampler.setResponseGenerator(new RandomPartition<SpatialSparseVertex>(0.2));
		sampler.setListener(new SampleSizeListener());
		
		sampler.run(graph);
		
		Set<Point> choiceSet = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.10.xml");
		for(SpatialVertex v : graph2.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
		
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		
		array.addAnalyzerTask(new TopologyAnalyzerTask(), "topo");
		
		AnalyzerTaskComposite spatialTask = new AnalyzerTaskComposite();
		spatialTask.addTask(new EdgeLengthTask());
		EdgeLength.getInstance().setIgnoreZero(true);
		

//		AcceptanceProbabilityTask accTask = new AcceptanceProbabilityTask(choiceSet);
//		accTask.setModule(new ObservedAcceptanceProbability());
//		spatialTask.addTask(accTask);
		
		Accessibility access = new ObservedAccessibility(new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator()));
		access.setTargets(choiceSet);
		CachedAccessibility cachedAccess = new CachedAccessibility(access);
//		
		spatialTask.addTask(new DegreeAccessibilityTask(cachedAccess));
		spatialTask.addTask(new EdgeLengthAccessibilityTask(cachedAccess));
		spatialTask.addTask(new TransitivityAccessibilityTask(cachedAccess));
		
		AcceptancePropaCategoryTask t = new AcceptancePropaCategoryTask(cachedAccess);
//		t.setBoundary(boundary);
		t.setDestinations(choiceSet);
		spatialTask.addTask(t);
		
		array.addAnalyzerTask(spatialTask, "spatial");
		
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		DegreeTask kTask = new DegreeTask();
		SimplePiEstimator estim = new SimplePiEstimator(graph.getVertices().size());
		kTask.setModule(new EstimatedDegree(estim, new WSMStatsFactory()));
		composite.addTask(kTask);
		composite.addTask(new WaveSizeTask());
		array.addAnalyzerTask(composite, "snowball");
		

//		estim.update(sampler.getSampledGraph());
//		GraphAnalyzer.analyze(sampler.getSampledGraph(), array, "/Users/jillenberger/Work/socialnets/mcmc/output/snowball/");
		GraphAnalyzer.analyze(sampler.getSampledGraph(), array, "/Users/jillenberger/Work/socialnets/mcmc/ergm/");
//		GraphAnalyzer.analyze(sampler.getSampledGraph(), array, "/Users/jillenberger/Work/socialnets/mcmc/naive/");
//		GraphAnalyzer.analyze(graph, spatialTask, "/Users/jillenberger/Work/socialnets/mcmc/ergm/small/");
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setDrawEdges(false);
//		writer.write((SpatialGraph) sampler.getSampledGraph(), "/Users/jillenberger/Work/socialnets/mcmc/ergm/snowball/graph.kmz");
//		writer.write((SpatialGraph) sampler.getSampledGraph(), "/Users/jillenberger/Work/socialnets/mcmc/naive/snowball/graph.kmz");
	}

	private static class SeedGenerator implements VertexFilter<SpatialSparseVertex> {

		private Geometry geometry;
		
		public SeedGenerator(Geometry geometry) {
			this.geometry = geometry;
		}
		
		@Override
		public Set<SpatialSparseVertex> apply(Set<SpatialSparseVertex> vertices) {
			Set<SpatialSparseVertex> zrh = new HashSet<SpatialSparseVertex>();
			
			for(SpatialSparseVertex vertex : vertices) {
				if(geometry.contains(vertex.getPoint())) {
					zrh.add(vertex);
				}
			}
			
			VertexFilter<SpatialSparseVertex> filter = new FixedSizeRandomPartition<SpatialSparseVertex>(40);
			
			return filter.apply(zrh);
		}
		
	}
	
	private static class SampleSizeListener implements SamplerListener {

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
//			if(sampler.getSampledGraph().getVertices().size() > 7000)
			if(sampler.getSampledGraph().getVertices().size() > 12363)
//			if(sampler.getNumSampledVertices() > 700)
				return false;
			else
				return true;
		}

		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
		}
		
	}
}
