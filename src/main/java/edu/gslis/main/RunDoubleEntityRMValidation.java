package edu.gslis.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.entities.docscoring.CachedFileLookupDocScorer;
import edu.gslis.entities.docscoring.DatabaseLookupDocScorer;
import edu.gslis.entities.docscoring.FileLookupDocScorer;
import edu.gslis.entities.docscoring.FileLookupQueryLikelihoodQueryScorer;
import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.evaluators.NDCGEvaluator;
import edu.gslis.evaluation.running.runners.DoubleEntityRMRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.readers.QueryProbabilityDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataInterpreter;
import edu.gslis.related_docs.DocumentClusterDataSource;
import edu.gslis.related_docs.RelatedDocs;
import edu.gslis.scoring.CachedDocScorer;
import edu.gslis.scoring.DirichletDocScorer;
import edu.gslis.scoring.DocScorer;
import edu.gslis.scoring.queryscoring.QueryLikelihoodQueryScorer;
import edu.gslis.scoring.queryscoring.QueryScorer;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.config.Configuration;
import edu.gslis.utils.config.SimpleConfiguration;
import edu.gslis.utils.data.interpreters.RelevanceModelDataInterpreter;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.DatabaseDataSource;
import edu.gslis.utils.data.sources.FileDataSource;

public class RunDoubleEntityRMValidation {
	
	public static void main(String[] args) throws InterruptedException {
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		IndexWrapperIndriImpl wikiIndex = new IndexWrapperIndriImpl(config.get("wiki-index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);
		String targetMetric = config.get("target-metric");
		
		IndexBackedCollectionStats cs = new IndexBackedCollectionStats();
		cs.setStatSource(index);
		
		FileDataSource ds = new DocumentClusterDataSource(new File(config.get("document-entities-file-self")));
		RelatedDocs selfClusters = (new DocumentClusterDataInterpreter()).build(ds);
		ds.read(new File(config.get("document-entities-file-wiki")));
		RelatedDocs wikiClusters = (new DocumentClusterDataInterpreter()).build(ds);

		Set<String> terms = new HashSet<String>();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			Iterator<String> featureIt = query.getFeatureVector().iterator();
			while (featureIt.hasNext()) {
				terms.add(featureIt.next());
			}
		}

		Evaluator evaluator = new MAPEvaluator(qrels);
		if (targetMetric.equalsIgnoreCase("ndcg")) {
			evaluator = new NDCGEvaluator(qrels);
		}
		
		Connection dbCon = DatabaseDataSource.getConnection(config.get("database"));
		
		DatabaseDataSource data = new DatabaseDataSource(dbCon, SearchResultsDataInterpreter.DATA_NAME);
		data.read();
		SearchResultsDataInterpreter dataInterpreter = new SearchResultsDataInterpreter(index);
		SearchHitsBatch initialHitsBatch = dataInterpreter.build(data);

		long seed = Long.parseLong(args[1]);

		DatabaseDataSource expansionDataSelf = new DatabaseDataSource(
				dbCon, "expansion_rms_self");
		DatabaseDataSource expansionQueryDataSelf = new DatabaseDataSource(
				dbCon, "expansion_probabilities_self");
		DatabaseDataSource expansionDataWiki = new DatabaseDataSource(
				dbCon, "expansion_rms_wiki");
		DatabaseDataSource expansionQueryDataWiki = new DatabaseDataSource(
				dbCon, "expansion_probabilities_wiki");
		
		// fbDocs scorer
		DocScorer docScorer = new CachedDocScorer(new DirichletDocScorer(0, cs));
		
		// fbDocs query scorer
		FileLookupDocScorer docScorerQueryProb = new CachedFileLookupDocScorer(config.get("document-probability-data-dir"));
		QueryScorer docQueryScorer = new FileLookupQueryLikelihoodQueryScorer(docScorerQueryProb);
		
		// self expansion docs scorer
		DocScorer expansionScorerSelf = new CachedDocScorer(new DatabaseLookupDocScorer(expansionDataSelf,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT")));
		
		// self expansion docs query scorer
		DocScorer expansionScorerQueryProbSelf = new CachedDocScorer(new DatabaseLookupDocScorer(expansionQueryDataSelf,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT")));
		QueryScorer expansionQueryScorerSelf = new QueryLikelihoodQueryScorer(expansionScorerQueryProbSelf);

		// wiki expansion docs scorer
		DocScorer expansionScorerWiki = new CachedDocScorer(new DatabaseLookupDocScorer(expansionDataWiki,
				new RelevanceModelDataInterpreter(RelevanceModelDataInterpreter.SCORE_FIELD,
						RelevanceModelDataInterpreter.TERM_FIELD,
						"ORIGINAL_DOCUMENT")));
		
		// wiki expansion docs query scorer
		DocScorer expansionScorerQueryProbWiki = new CachedDocScorer(new DatabaseLookupDocScorer(expansionQueryDataWiki,
				new QueryProbabilityDataInterpreter(RelevanceModelDataInterpreter.TERM_FIELD,
						RelevanceModelDataInterpreter.SCORE_FIELD,
						"QUERY", "DOCUMENT")));
		QueryScorer expansionQueryScorerWiki = new QueryLikelihoodQueryScorer(expansionScorerQueryProbWiki);

		DoubleEntityRMRunner runner = new DoubleEntityRMRunner(index, initialHitsBatch, stopper,
				docScorer, expansionScorerSelf, expansionScorerWiki,
				docQueryScorer, expansionQueryScorerSelf, expansionQueryScorerWiki,
				selfClusters, wikiClusters, index, wikiIndex);
		KFoldValidator validator = new KFoldValidator(runner, 10);
		
		SearchHitsBatch batchResults = validator.evaluate(seed, queries, evaluator);
		
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("rm3", outputWriter);
		
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
