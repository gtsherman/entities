package edu.gslis.docscoring.support;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gslis.readers.ModelReader;
import edu.gslis.readers.TSVReader;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.utils.NameToFileNameConverter;

public class CategoryProbability {

	private String modelDir;
	private String docEntitiesDir;
	private SearchHit doc;
	
	/**
	 * Constructor method.
	 * @param modelRootDir	The root directory containing the category models.
	 * @param entitiesRootDir	The root directory containing the per-document entities.
	 */
	public CategoryProbability(String modelRootDir, String entitiesRootDir) {
		this.setModelRootDir(modelRootDir);
		this.setDocEntitiesRootDir(entitiesRootDir);
	}
	
	public void setDoc(SearchHit doc) {
		this.doc = doc;
	}
	
	public void setModelRootDir(String dir) {
		this.modelDir = dir;
	}
	
	public void setDocEntitiesRootDir(String dir) {
		this.docEntitiesDir = dir;
	}
	
	public double getProbability(String term) {
		Set<String> entities = this.getDocEntities();
		NameToFileNameConverter nc = new NameToFileNameConverter(this.doc.getDocno());
		ModelReader reader = new ModelReader();
		
		double prob = 1.0;
		for (String entity : entities) {
			try {
				File modelFile = new File(this.modelDir+"/"+nc.getFirstChar()+"/"+nc.getSecondChar()+"/"+entity);
				Map<String, Double> termScores = reader.readFile(modelFile);

				if (termScores.containsKey(term)) {
					prob *= termScores.get(term);
				}
				prob *= 0.001;
			} catch (Exception e) {
				System.err.println(e.getStackTrace());
			}
		}
		return prob;
	}
	
	private Set<String> getDocEntities() {
		int entityIndex = 6;

		Set<String> entities = new HashSet<String>();
		try {
			File entityFile = new File(this.docEntitiesDir+"/"+this.doc.getDocno());
			if (entityFile.exists()) {
				TSVReader reader = new TSVReader();
				List<List<String>> lines = reader.readFile(entityFile);
				for (List<String> line : lines) {
					String entity = line.get(entityIndex);
					entities.add(entity);
				}
			}
		} catch (Exception e) {
			System.err.println("Error gathering entities from file: "+this.doc.getDocno());
		}
		return entities;
	}
}