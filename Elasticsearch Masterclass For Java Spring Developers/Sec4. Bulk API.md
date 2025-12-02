[ Bulk API ]
---

`Bulk API Introduction`

Elasticsearch의 Bulk API는 대량의 문서에 대해 일괄적으로 작업을 수행할 수 있는 강력한 기능을 제공한다. 이를 통해 여러 개의 인덱싱, 업데이트, 삭제 작업을 한 번의 요청으로 처리할 수 있어 성능
향상과 네트워크 오버헤드 감소에 큰 도움이 된다. 예시는 다음과 같다.

```http request
POST /_bulk
# bulk insert
POST /my-index/_bulk
{ "create" : {} }
{ "name" : "item1" }
{ "create" : {} }
{ "name" : "item2" }
{ "create" : {} }
{ "name" : "item3" }
```
요청 페이로드로는 전통적인 JSON 배열이 아닌, 각 작업을 나타내는 메타데이터 행과 실제 데이터를 번갈아 가며 나열하는 형식을 사용한다. 각 작업은 다음과 같은 형식으로 구성된다.
이를 **NDJSON(Newline Delimited JSON)** 이라고 부른다.

NDJSON은 각 줄이 독립적인 JSON 객체로 구성되어 있어, 한 줄씩 읽으면서 바로바로 파싱할 수 있으며, 대용량 데이터 처리에 적합하다. Bulk API는 이러한 NDJSON 형식을 활용하여 여러 작업을 효율적으로 처리한다.
스트림 기반으로 데이터를 처리할 수 있어 메모리 사용량을 최소화할 수 있다. 또한, 각 작업에 대한 결과를 개별적으로 확인할 수 있어, 실패한 작업만 재처리하는 등의 세밀한 제어가 가능하다.

---
`Bulk API Demo`

실제 Bulk API를 사용하여 여러 문서를 한 번에 인덱싱하는 예제를 살펴보자.

```http request
POST /my-index/_bulk
{ "create" : {} }
{ "name" : "item1" }
{ "create" : {} }
{ "name" : "item2" }
{ "create" : {} }
{ "name" : "item3" }
```
위 예제에서는 `my-index`라는 인덱스에 세 개의 문서를 한 번에 추가하는 요청을 보내고 있다. 각 문서는 `create` 작업을 통해 생성되며, 실제 데이터는 다음 줄에 JSON 형식으로 제공된다.
응답으로는 각 작업의 성공 여부와 생성된 문서의 ID 등이 포함된 결과가 반환된다. 이를 통해 어떤 작업이 성공했는지, 어떤 작업이 실패했는지를 쉽게 파악할 수 있다.
위 경우에는 아이디를 별도로 지정하지 않았으므로 생성의 결과에 따른 아이디는 Elasticsearch가 자동으로 생성한 아이디가 된다.

만약 특정 아이디로 문서를 생성하고 싶다면 다음과 같이 할 수 있다.

```http request
POST /my-index/_bulk
{ "create" : { "_id": "1" } }
{ "name" : "item1" }
{ "create" : { "_id": "2" } }
{ "name" : "item2" }
{ "create" : { "_id": "3" } }
{ "name" : "item3" }
```

삽입과 수정, 삭제를 혼합하여 한 번에 처리할 수도 있다.

```http request
POST /my-index/_bulk
{ "index" : { "_id": "1" } }
{ "name" : "updatedItem1" }
{ "delete" : { "_id": "2" } }
{ "create" : { "_id": "3" } }
{ "name" : "item3" }
```

---
`Granular Error Handling`
Bulk API는 각 작업에 대한 개별적인 결과를 반환하므로, 일부 작업이 실패하더라도 전체 요청이 실패하지 않는다. 이를 통해 실패한 작업만 재처리하거나 별도로 처리할 수 있다.
예를 들어, 다음과 같은 Bulk 요청이 있다고 가정해보자.

```http request
POST /my-index/_bulk
{ "create" : { "_id": "1" } }
{ "name" : "item1" }
{ "create" : { "_id": "1" } }  # Duplicate ID to trigger error
{ "name" : "item2" }
{ "create" : { "_id": "3" } }
{ "name" : "item3" }
```

위와 같은 Bulk 요청에서 두 번째 `create` 작업은 동일한 ID를 사용하여 오류가 발생할 것이다. 응답에서는 각 작업에 대한 성공 여부와 오류 메시지가 포함되어 있어, 실패한 작업만 별도로 처리할 수 있다.
반환값로는 다음과 같은 형식이 될 것이다.

```json
{
  "took": 30,
  "errors": true,
  "items": [
    {
      "create": {
        "_index": "my-index",
        "_id": "1",
        "status": 201
      }
    },
    {
      "create": {
        "_index": "my-index",
        "_id": "1",
        "status": 409,
        "error": {
          "type": "version_conflict_engine_exception",
          "reason": "[1]: version conflict, document already exists"
        }
      }
    },
    {
      "create": {
        "_index": "my-index",
        "_id": "3",
        "status": 201
      }
    }
  ]
}
```

--- 
`Optimistic Concurrency Control / Multiple Indices`

Bulk API는 낙관적 동시성 제어(Optimistic Concurrency Control)를 지원하여, 문서의 버전을 기반으로 충돌을 방지할 수 있다. 
이를 통해 여러 클라이언트가 동시에 동일한 문서를 수정할 때 발생할 수 있는 문제를 최소화할 수 있다.

```http request
POST /my-index/_bulk
{ "create" : { "_id": 1 } }
{ "name" : "item1" }
{ "create" : { "_id": 2 } }
{ "name" : "item2" }
{ "create" : { "_id": 3 } }
{ "name" : "item3" }


POST /my-index/_bulk
{ "update" : { "_id": 2, "if_seq_no": "1", "if_primary_term": "1" } }
{ "doc": { "name" : "item2-updated" }}
{ "update" : { "_id": 3, "if_seq_no": "3", "if_primary_term": "1" } }
{ "doc": { "name" : "item3-updated" }}
```

벌크 연산에서도 순차적으로 적용되므로 OCC가 적용된다는 것을 알 수 있다.

주의할 점은 seq_no의 변화이다. seq_no은 프라이머리 샤드 단위 전역 시퀀스로 인덱스와 프라이머리 샤드 기준으로 관리된다. 
즉, 동일한 프라이머리 샤드와 인덱스 내에서는 여러 문서의 변화에 따라 seq_no가 증가하며, 다른 프라이머리 샤드나 인덱스에서는 별도로 관리된다.
따라서 Bulk API를 사용할 때는 동일한 인덱스와 프라이머리 샤드 내에서 작업을 수행하는 경우에만 OCC를 적용할 수 있다.
**(샤드를 넘어 하나의 전역 OCC는 제공되지 않는다)**

--- 

`Reindex API`

이전 섹션에서 인덱스는 한 번 생성되면 변경할 수 없다고 언급했다. 하지만, 인덱스의 매핑이나 설정을 변경해야 하는 경우가 발생할 수 있다.
이럴 때는 Reindex API를 사용하여 기존 인덱스의 데이터를 새로운 인덱스로 복사할 수 있다. Reindex API는 대량의 데이터를 효율적으로 복사할 수 있도록 설계되었다.
참고로 Reindex API를 쓰더라도 기존 데이터는 삭제되지 않으며, 새로운 인덱스가 생기고 복사되면서 샤드 구성도 새로 만들어지는 것이라는 점을 명심하자.

```http request
POST /_reindex
{
  "source": {
    "index": "old-index"
  },
  "dest": { 
    "index": "new-index"
  }
}
```
위 예제에서는 `old-index`라는 기존 인덱스의 데이터를 `new-index`라는 새로운 인덱스로 복사하는 요청을 보내고 있다.
이를 위해서는 새로운 인덱스를 미리 생성해두어야 한다. 새로운 인덱스의 샤드 구성이 기존 인덱스와 다르더라도 그에 맞춰 데이터들이 분산되어 복사된다.

필요에 따라 소스 인덱스에서 특정 조건에 맞는 문서만 복사하거나, 복사 과정에서 문서를 변환하는 등의 작업도 가능하다.
```http request
POST /_reindex
{
  "source": {
    "index": "old-index",
    "query": {
      "term": { "status": "active" }
    }
  },
  "dest": { 
    "index": "new-index"
  },
  "script": {
    "source": "ctx._source.new_field = 'new_value'"
  }
}
```
위 예제에서는 `old-index`에서 `status` 필드가 `active`인 문서만을 `new-index`로 복사하며, 복사 과정에서 각 문서에 `new_field`라는 새로운 필드를 추가하는 스크립트를 적용하고 있다.
Reindex API는 대량의 데이터를 효율적으로 복사하고 변환할 수 있는 강력한 도구로, 인덱스 구조 변경이나 데이터 마이그레이션 시 유용하게 활용될 수 있다.

특정 필드만 복사하고 싶다면 `_source` 필드를 활용할 수도 있다. 아래 예제는 name과 age 필드만 복사하는 예제이다.

```http request
POST /_reindex
{
  "source": {
    "index": "old-index",
    "_source": ["name", "age"]
  },
  "dest": { 
    "index": "new-index"
  }
}
```

---
`Summary`

Bulk API는 대량의 문서를 추가하거나 업데이트할 때, 한번의 요청으로 이를 처리할 수 있게 해준다.
액션 라인 + 문서 라인 형식의 NDJSON 포맷을 사용하며, 각 작업에 대한 개별적인 결과를 반환하여 세밀한 오류 처리가 가능하다.
낙관적 동시성 제어(Optimistic Concurrency Control)를 지원하여, 여러 클라이언트가 동시에 동일한 문서를 수정할 때 발생할 수 있는 문제를 최소화할 수 있다.

Reindex API는 기존 인덱스의 데이터를 새로운 인덱스로 복사할 수 있는 기능을 제공하며, 복사 과정에서 필터링이나 변환 작업도 가능하다.
이를 통해 인덱스 구조 변경이나 데이터 마이그레이션 시 유용하게 활용될 수 있다.
한번 만들어진 샤드 구성은 변경할 수 없으므로 새로운 샤드 구성이 필요할 때 Reindex API를 사용하여 데이터를 새로운 인덱스로 복사하는 것이 중요하다.

실무에서는 주로 초기 데이터 적재 및 마이그레이션, 테스트 또는 QA 환경에서 직접 사용되는 경우가 많다. 
또한 개발자가 직접 사용하지 않더라도 Logstash, Beats, Kafka Connect 등에서 내부적으로 Bulk API를 활용하여 대량의 데이터를 효율적으로 엘라스틱서치에 적재한다.

다만, 유저 액션 기반의 실시간 데이터 처리에는 적합하지 않으므로 어플리케이션의 특성에 따라 적절히 활용하는 것이 중요하다.
**(벌크 처리는 어느정도 데이터를 쌓은 이후에 일괄적으로 처리하는 방식이기 때문이다.)**