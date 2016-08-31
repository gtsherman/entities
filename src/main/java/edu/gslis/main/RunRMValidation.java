package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.SearchHitsBatch;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.validators.RMValidator;
import edu.gslis.patches.FormattedOutputTrecEval;
import edu.gslis.patches.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queryrunning.RMRunner;
import edu.gslis.readers.RelevanceModelReader;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RunRMValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		String rmsDir = config.get("rms-dir");
		
		RelevanceModelReader rmReader = new RelevanceModelReader();
		rmReader.setBasePath(rmsDir);
		
		RMRunner runner = new RMRunner(index, rmReader, stopper);

		//List<Double> maps = new ArrayList<Double>();
		//Map<Thread, Runnable> threads = new HashMap<Thread, Runnable>();
		//for (int i = 0; i < 10; i++) {
			RMValidator validator = new RMValidator(runner);
			validator.setQueries(queries);
			validator.setQrels(qrels);
		/*	validator.setEvaluator(new MAPEvaluator());
			
			System.err.println("Running thread "+i);
			Thread t = new Thread(validator);
			t.start();
			threads.put(t, validator);
		}
		
		int i = 0;
		for (Thread t : threads.keySet()) {
			t.join();
			double map = ((SecondOrderRMValidator)threads.get(t)).getMap();
			System.err.println("Map for thread "+(i++)+": "+map);
			maps.add(map);
		}

		double sum = 0.0;
		for (double map : maps) {
			sum += map;
		}
		double map = sum /= maps.size();
		System.out.println("Average MAP: "+map);*/

		SearchHitsBatch batchResults = validator.evaluate(new MAPEvaluator());
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("entities", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
