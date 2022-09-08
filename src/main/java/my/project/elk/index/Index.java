package my.project.elk.index;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class Index {
    private final RestClient restClient;

    public String getIndex() throws IOException {
        String indexName = "";
        Request request = null;

        request = new Request("GET", "/_cat/indices/Index*?s=index:desc&h=index,health");

        Response response = restClient.performRequest(request);

        InputStream inputStream = response.getEntity().getContent();
        List<String> indexes = new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.toList());

        if (indexes.size() > 1) {
            String[] nowIndex = indexes.get(0).split(" ");
            String[] oldIndex = indexes.get(1).split(" ");
            indexName = "green".equals(nowIndex[1]) ? nowIndex[0] : oldIndex[0];
        }

        return indexName;
    }
}
