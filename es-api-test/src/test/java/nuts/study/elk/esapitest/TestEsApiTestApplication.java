package nuts.study.elk.esapitest;

import org.springframework.boot.SpringApplication;

public class TestEsApiTestApplication {

    public static void main(String[] args) {
        SpringApplication.from(EsApiTestApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
