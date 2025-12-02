package nuts.study.elk.esapitest.sec01.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "customers")
@Setting(settingPath = "sec01/index-setting.json")
@Mapping(mappingPath = "sec01/index-mapping.json")
@Getter
@Setter
public class Customer {

    @Id
    private String id;
    private String name;
    private Integer age;

}