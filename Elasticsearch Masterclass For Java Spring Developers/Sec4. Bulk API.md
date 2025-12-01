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

