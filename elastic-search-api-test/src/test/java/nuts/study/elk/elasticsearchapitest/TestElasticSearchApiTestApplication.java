package nuts.study.elk.elasticsearchapitest;

import org.springframework.boot.SpringApplication;

public class TestElasticSearchApiTestApplication {

    public static void main(String[] args) {
        SpringApplication.from(ElasticSearchApiTestApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
