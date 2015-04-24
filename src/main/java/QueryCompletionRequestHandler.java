import org.apache.lucene.index.*;
import org.apache.lucene.search.SearcherFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;

import java.io.IOException;
import java.util.*;

public class QueryCompletionRequestHandler extends RequestHandlerBase {


	@Override public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse)
			throws Exception {


		int count = 0;
		Collection<String> fields = solrQueryRequest.getSearcher().getFieldNames();
		NamedList<String> results = new NamedList<String>();
		SolrParams params = solrQueryRequest.getParams();
		String q = params.get(CommonParams.Q);
		String[] twoPartString = getStringParts(q);

		for (String field : fields){
			if ((field.length() > twoPartString[1].length()) && (field.substring(0, twoPartString[1].length()).equals(twoPartString[1]))){
				results.add("" + count++, twoPartString[0] + " " + field + ":()");
			}

		}
		solrQueryResponse.add("auto_complete", results);
		//solrQueryResponse.add("fields", getFieldsInNamedList(fields));
	}

	private String[] getStringParts(String q) {
		String[] tokens = q.split(" ");

		String baseString = "";
		for (int i = 0; i < tokens.length-1; i++){
			baseString += " " + tokens[i];
		}
		return new String[]{baseString, tokens[tokens.length-1]};
	}

	@Override public String getDescription() {
		return "Testing count";
	}

	private NamedList<Integer> getFieldsInNamedList(Collection<String> fields){
		NamedList<Integer> result_fields = new NamedList<Integer>();

		for (String field : fields){
			result_fields.add(field, 1);
		}
		return result_fields;
	}
}
