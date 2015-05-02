import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;

import java.io.IOException;
import java.util.*;

public class QueryCompletionRequestHandler extends RequestHandlerBase {


	@Override public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse)
			throws Exception {

		Collection<String> fieldNames = solrQueryRequest.getSearcher().getFieldNames();
		IndexSchema index = solrQueryRequest.getSearcher().getSchema();
		Map<String, SchemaField> schemaFields = index.getFields();

		List<String> fields = getStoredFields(fieldNames, schemaFields);

		SolrParams params = solrQueryRequest.getParams();

		ArrayList<SimpleOrderedMap<String>> results = new ArrayList<SimpleOrderedMap<String>>();
		String q = params.get(CommonParams.Q);
		if (linkWords(q)){
			results.add(wrappInMap(q + "AND "));
			results.add(wrappInMap(q + "OR "));
			results.add(wrappInMap(q + "NOT "));
		}
		else if (isInFieldAutoComplete(q)){
			results = inFieldAutoCompletion(q);
		}
		else {
			String[] twoPartString = getStringParts(q);
			results = addQuerySuggestionsToRequest(fields, index, twoPartString);
		}
		solrQueryResponse.add("auto_complete", results);
	}

	private boolean linkWords(String q){
		if (q.endsWith(" ")){
			if (q.endsWith("AND ") || q.endsWith("OR ") || q.endsWith("NOT ")){
				return false;
			}
			return true;
		}
		return false;
	}

	private List<String> getStoredFields(Collection<String> fieldNames, Map<String, SchemaField> schemaFields) {
		List<String> fields = new ArrayList<String>(fieldNames.size());
		for(String fieldName : fieldNames){
			if (schemaFields.get(fieldName).stored()){
				fields.add(fieldName);
			}
		}
		return fields;
	}

	private ArrayList<SimpleOrderedMap<String>> inFieldAutoCompletion(String q) throws SolrServerException, IOException {
		ArrayList<SimpleOrderedMap<String>> results = new ArrayList<SimpleOrderedMap<String>>();
		if (q.endsWith(")")){
			q = q.substring(0, q.length()-1);
		}
		if (!q.endsWith("(")) {
			String field = q.substring(0, q.lastIndexOf(':'));
			field = field.substring(field.lastIndexOf(' ') + 1);
			String fieldSearch = q.substring(q.lastIndexOf('(') + 1);
			q = q.substring(0, q.lastIndexOf('('));
			String[] twoPartString = getStringParts(fieldSearch);
			QueryResponse suggested = getSuggestions(field, twoPartString[1]);

			List<SpellCheckResponse.Suggestion> suggestions = suggested.getSpellCheckResponse().getSuggestions();
			for (SpellCheckResponse.Suggestion suggestion : suggestions) {
				for (String alternative : suggestion.getAlternatives()) {
					results.add(wrappInMap(q + "(" + (twoPartString[0] + " " + alternative).trim() + " "));
				}
			}
		}
		return results;
	}

	private QueryResponse getSuggestions(String field, String s) throws SolrServerException, IOException {
		SolrClient solr = new HttpSolrClient("http://localhost:8983/solr/Test");
		SolrQuery parameters = new SolrQuery();
		parameters.set("qt", "/suggest_" + field);
		parameters.set("q", s);
		return solr.query(parameters);
	}

	private ArrayList<SimpleOrderedMap<String>> addQuerySuggestionsToRequest(Collection<String> fields, IndexSchema index, String[] twoPartString) {
		ArrayList<SimpleOrderedMap<String>> results = new ArrayList<SimpleOrderedMap<String>>();
		for (String field : fields){
			if ((field.length() >= twoPartString[1].length()) && (field.substring(0, twoPartString[1].length()).equals(twoPartString[1]))){
				results.add(wrappInMap(twoPartString[0] + " " + field + ":("));
				addRangeIfRangeField(index, twoPartString, results, field);
			}
		}
		return results;
	}

	private void addRangeIfRangeField(IndexSchema index, String[] twoPartString, ArrayList<SimpleOrderedMap<String>> results,
			String field) {
		if(index.getFieldType(field).getTypeName().equals("int") || index.getFieldType(field).getTypeName().equals("date")){
			results.add(wrappInMap(twoPartString[0] + " " + field + ":[ TO ]"));
		}
	}

	@Override public String getDescription() {
		return "Testing count";
	}

	private String[] getStringParts(String q) {
		String[] tokens = q.split(" ");

		String baseString = "";
		for (int i = 0; i < tokens.length-1; i++){
			baseString += " " + tokens[i];
		}
		return new String[]{baseString, tokens[tokens.length-1]};
	}

	private SimpleOrderedMap<String> wrappInMap(String term){
		SimpleOrderedMap<String> map = new SimpleOrderedMap<String>();
		map.add("term", term.trim());
		return map;
	}

	private boolean isInFieldAutoComplete(String q){
		if (q.endsWith(")") || q.lastIndexOf('(') > q.lastIndexOf(')')){
			return true;
		}
		return false;
	}
}
