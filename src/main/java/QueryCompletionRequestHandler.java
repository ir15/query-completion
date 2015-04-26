import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;

import java.lang.reflect.Array;
import java.util.*;

public class QueryCompletionRequestHandler extends RequestHandlerBase {


	@Override public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse)
			throws Exception {

		Collection<String> fields = solrQueryRequest.getSearcher().getFieldNames();
		IndexSchema index = solrQueryRequest.getSearcher().getSchema();
		SolrParams params = solrQueryRequest.getParams();
		NamedList<String> results;
		String q = params.get(CommonParams.Q);
		if (q.endsWith(" ")){
			results = new NamedList<String>();
			results.add("0", q + "AND ");
			results.add("1", q + "OR ");
		}
		else {
			String[] twoPartString = getStringParts(q);

			results = addQuerySuggestionsToRequest(fields, index, twoPartString);
			solrQueryResponse.add("auto_complete", results);
			//solrQueryResponse.add("fields", getFieldsInNamedList(fields));
		}
	}

	private NamedList<String> addQuerySuggestionsToRequest(Collection<String> fields, IndexSchema index, String[] twoPartString) {
		int count = 0;
		NamedList<String> results = new NamedList<String>();
		for (String field : fields){
			if ((field.length() > twoPartString[1].length()) && (field.substring(0, twoPartString[1].length()).equals(twoPartString[1]))){
				results.add("" + count++, twoPartString[0] + " " + field + ":()");
				count = addRangeIfRangeField(index, twoPartString, count, results, field);
			}
		}
		return results;
	}

	private int addRangeIfRangeField(IndexSchema index, String[] twoPartString, int count, NamedList<String> results,
			String field) {
		if(index.getFieldType(field).getTypeName().equals("int") || index.getFieldType(field).getTypeName().equals("date")){
			results.add("" + count++, twoPartString[0] + " " + field + ":[ TO ]");
		}
		return count;
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

	private NamedList<Integer> getFieldsInNamedList(Collection<String> fields){
		NamedList<Integer> result_fields = new NamedList<Integer>();

		for (String field : fields){
			result_fields.add(field, 1);
		}
		return result_fields;
	}
}
