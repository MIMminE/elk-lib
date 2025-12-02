package nuts.study.elk.esapitest.sec01.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "reviews")
@Setting(shards = 2, replicas = 2)
@Getter
@Setter
public class Review {

    @Id
    private String id;
}
