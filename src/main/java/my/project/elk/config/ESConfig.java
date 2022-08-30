package my.project.elk.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ESConfig {

    @Value("${elastic.user.username}")
    private static String userName;

    @Value("${elastic.user.password}")
    private static String password;

    @Value("${elastic.host}")
    private static String host;

    @Value("${elastic.port}")
    private static int port;


    @Bean
    public RestClient restClient() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));

        HttpHost httpHost = new HttpHost(host, port, "https");

        RestClientBuilder restBuilder = RestClient.builder(httpHost).setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

        restBuilder.setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(10000));

        return restBuilder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = restClient();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }

}
