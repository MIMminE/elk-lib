[ Elasticsearch Core Concepts ]
---
`Terminologies`

이번 섹션의 목표는 Elasticsearch의 인덱스를 가지고 직접 CRUD 작업을 수행하는 것이다.
그리고 내부적으로 어떻게 동작하는지도 살펴볼 것이다. 들어가기에 앞서 몇 가지 용어를 정리하고 간다.

- `index` : 관계형 데이터베이스의 테이블이라고 생각하면 된다. MongoDB에서는 컬렉션이라고 부르는 개념과 같다. 관련된 문서의 집합을 의미한다.
- `document` : 테이블의 한 레코드, 즉 한 행(row)를 의힌다. 행에는 여러 컬럼이 있듯이 문서에는 여러 필드가 있다. JSON 객체로 표현된다. 여러 document가 모여 index를 구성한다.
- `field` : document 내의 속성, 즉 컬럼을 의미한다. 예를 들어 name, age, address 등이 필드가 될 수 있다.

---
`Index - Create / Delete`

이번에는 직접 인덱스 API를 직접 사용해보겠다.
이를 위해 현재 어떤 인덱스들이 있는지 확인해보겠다.

```http request
GET _cat/indices
```

indices는 인덱스의 복수형으로 현재 클러스터에 존재하는 모든 인덱스를 보여준다. 만약 특정 패턴의 인덱스를 검색하고 싶다면 와일드카드를 사용할 수 있다.

```http request
GET _cat/indices/logs-*
```

이제 새로운 인덱스를 만들어본다. 상품 정보를 저장하고 싶다고 할 때, product라는 인덱스를 만들고 싶다면 아래와 같이 사용하면 된다.

```http request
PUT /product
```

acknowledged가 true로 반환되면 인덱스가 성공적으로 생성된 것이다. shards 와 관련된 내용은 추후에 다룬다.
(추가적으로 Elasticsearch 의 REST API 규칙은 REST 규칙을 엄격히 따르지 않는다고 한다. 인덱스를 생성하는 메서드가 POST가 아닌 PUT인 점이 그 예이다.)

위와 같이 인덱스를 생성하고 나서 GET _cluster/health API를 호출하면 yellow 상태가 된다.
이는 단일 노드이기 때문에 복제본이 할당되지 않아서 발생하는 현상이다.
만약 단일 노드가 다운되면 해당 인덱스는 검색 요청 등을 처리할 수 없기 때문에 경고하는 것이다. 현재는 학습용 로컬 환경이므로 큰 문제는 없다.

인덱스를 삭제하는 방법도 알아보겠다.

```http request
DELETE /product
```

acknowledged가 true로 반환되면 인덱스가 성공적으로 삭제된 것이다. 이후 다시 클러스터 상태를 확인해보면 다시 green 상태로 돌아온다.

---
`[CRUD] - Adding Documents`
앞으로 몇 강에서는 인덱스에 문서를 추가하고, 수정하고 삭제하고 조회하는 방법을 다룬다.
먼저 문서를 추가하는 방법을 알아보겠다. 인덱스 이름 뒤에 _doc 이라는 예약어를 사용하여 문서를 추가할 수 있다.

```http request
POST /books/_doc
{
  "title": "To Kill a Mockingbird",
  "author": "Harper Lee",
  "year": 1960,
  "genre": "Fiction",
  "rating": 4.9
}

POST /books/_doc
{
  "title": "1984",
  "author": "George Orwell",
  "year": 1949,
  "genre": "Dystopian",
  "rating": 4.8
}
```

저장할 객체를 JSON 형식으로 요청 본문에 담아 보내면 그대로 저장된다.
잠깐 스키마 정의에 대해 살펴본다.

객체에 어떤 필드가 있고, 각 필드에 어떤 데이터 타입이 있는지 미리 정의하지 않아도 된다.
Elasticsearch는 문서를 저장할 때 자동으로 필드와 데이터 타입을 추론하여 매핑(mapping)을 생성한다.
이 기능을 동적 매핑(dynamic mapping)이라고 부른다.

하지만 매핑을 직접해주는 것도 있으며 이를 추천하는 편이다. 이는 추후에 자세히 다룬다.
(Kibana Dev Tools 에서는 헤더(application/json)를 자동으로 추가해주기 때문에 별도로 지정할 필요가 없다.
하지만 다른 도구를 사용할 때는 헤더를 명시적으로 지정해주어야 한다.)

문서를 추가할 때 ID를 명시적으로 지정하지 않으면 Elasticsearch가 자동으로 고유한 ID를 생성하여 할당한다.
응답에서 "_id" 필드에 생성된 ID가 포함되어 있다. 직접 ID를 지정하고 싶다면 다음과 같이 할 수 있다.

```http request
PUT /books/_doc/1
...
```

---
`Source Metadata Fields`

관계형 데이터베이스에는 테이블에 직접 데이터가 저장되지만, Elasticsearch에서는 문서가 인덱스에 저장될 때 몇 가지 메타데이터 필드가 자동으로 추가된다.
밑줄이 붙은 추가 필드들은 메타데이터 필드이다.

- `_index` : 문서가 저장된 인덱스의 이름을 나타낸다.
- `_type` : 문서의 유형을 나타낸다. 현재는 단일 유형만 지원되므로 항상 "_doc"이다.
- `_id` : 문서의 고유 식별자이다.
- `_version` : 문서의 버전 번호를 나타낸다. 문서가 수정될 때마다 증가한다.
- `_score` : 검색 결과에서 문서의 관련성을 나타내는 점수이다. 검색 쿼리 기능에서만 사용된다.
- `_source` : 실제로 저장된 문서의 내용을 포함한다. 사용자가 추가한 필드들이 여기에 포함된다.

---
`[CRUD] - Querying Documents`

이번에는 문서를 조회하는 방법에 대해 알아본다. 간단한 조회에 대한 것을 진행 후 복잡한 검색은 추후에 다룬다.
먼저 ID로 문서를 조회하는 방법이다.

```http request
GET /books/_doc/1
```

```json
{
  "_index": "books",
  "_id": "1",
  "_version": 1,
  "_seq_no": 0,
  "_primary_term": 1,
  "found": true,
  "_source": {
    "title": "To Kill a Mockingbird",
    "author": "Harper Lee",
    "year": 1960,
    "genre": "Fiction",
    "rating": 4.9
  }
}
```

응답에서 "_source" 필드에 실제로 저장된 문서의 내용이 포함되어 있다. 만약 존재하지 않는 ID로 요청하면 "found": false가 반환된다.

인덱스에 있는 모든 문서를 조회하고 싶다면, SQL의 select * from books처럼 다음과 같이 쿼리를 작성할 수 있다.

```http request
GET /books/_search  
```

```json
{
  "took": 1,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 2,
      "relation": "eq"
    },
    "max_score": 1,
    "hits": [
      {
        "_index": "books",
        "_id": "1",
        "_score": 1,
        "_source": {
          "title": "To Kill a Mockingbird",
          "author": "Harper Lee",
          "year": 1960,
          "genre": "Fiction",
          "rating": 4.9
        }
      },
      {
        "_index": "books",
        "_id": "2",
        "_score": 1,
        "_source": {
          "title": "1984",
          "author": "George Orwell",
          "year": 1949,
          "genre": "Dystopian",
          "rating": 4.8
        }
      }
    ]
  }
}
```

hits 배열에 모든 문서가 포함되어 있다.

다음으로 'Lee' 라는 저자의 책을 조회하는 방법이다.

```http request
GET /books/_search?q=Lee
```

이렇게 하면 특정 필드를 명시적으로 지정하지 않아도 해당 값이 포함된 모든 문서가 검색되며, 명확하게 필드를 지정할 수 있다.

```http request
GET /books/_search?q=author:Lee
```

엘라스틱 서치는 검색 엔진이기 때문에 복잡한 쿼리도 충분히 가능하며 이를 위해서는 내부 동작 원리와 매핑, 데이터 타입 등을 먼저 이해하는 것이 중요하다.

---
`[CRUD] - Updating Documents`

문서를 수정하는 방법에 대해 알아본다. PUT 또는 POST 메서드를 사용하여 문서를 교체할 수 있다.

```http request 
PUT /books/_doc/1
{
  "title": "To Kill a Mockingbird - Updated Edition",
  "author": "Harper Lee",
  "year": 1960,
  "genre": "Fiction",
  "rating": 5.0
}
```

이 방법은 전체 문서를 교체하기 때문에, 수정하지 않은 필드도 모두 포함시켜야 한다.
만약 일부 필드만을 수정할 목적으로 이 메서드를 사용하면 전달하지 않은 필드는 삭제된다.
만약 변경하고자 하는 ID가 존재하지 않으면 새로운 문서가 생성된다. 그렇기에 UPSERT 처럼 동작한다고 볼 수 있다.

--- 

`[ CRUD ] - Patch`

문서의 일부 필드만을 수정하고 싶다면 Update API를 사용하면 된다.

```http request
POST /books/_update/1
{
  "doc": {
    "rating": 4.95
  }
}
```
만약 기존에 없던 필드를 추가하면, 해당 필드가 문서에 추가된다.
또한, 해당하지 않는 ID로 요청을 보내면 예외가 발생하며, 없을 경우 문서를 추가하고 싶다면 별도의 옵션을 사용해야 한다.
```http request
POST /books/_update/3
{
  "doc": {
    "title": "Brave New World",
    "author": "Aldous Huxley",
    "year": 1932,
    "genre": "Dystopian",
    "rating": 4.7
  },
  "doc_as_upsert": true
}
```

--- 
`[ CRUD ] - Scripted Update`

스크립트 기능을 사용하여 문서를 동적으로 수정할 수도 있다.
```http request
POST /books/_update/1
{
  "script": {
    "source": "ctx._source.price += params.value",
    "params": {
      "value": 5000
    }
  }
}
```
ctx 는 미리 정의된 context 객체로, 현재 문서를 의미하며. _source 필드를 통해 문서의 필드에 접근할 수 있다.
위 예제에서는 price 필드의 값을 params 객체에 전달된 value 값만큼 증가시키는 스크립트를 사용하였다.
스크립트는 Painless 라는 도메인 특화 언어(DSL)를 사용하며, 복잡한 로직도 구현할 수 있다

아래는 조건문을 사용하여 필드를 수정하는 예제이다.
```http request

POST /books/_update/1
{
  "script": {
    "source": """
      if (ctx._source.rating < params.threshold) {
        ctx._source.status = 'Needs Review';
      } else {
        ctx._source.status = 'Approved';
      }
    """,
    "params": {
      "threshold": 4.8
    }
  }
}
```
별도의 조회가 없이 한 번의 요청으로 값을 수정할 수 있어 네트워크 오버헤드를 줄일 수 있다.
하지만 서버마다 스크립트를 해석하고 실행해야 하므로 CPU, 메모스 등의 리소스를 더 많이 사용한다는 점을 유의해야 한다.

---
`[ CRUD ] - Delete`

문서를 삭제하는 방법에 대해 알아본다. DELETE 메서드를 사용하여 특정 ID의 문서를 삭제할 수 있다.

```http request
DELETE /books/_doc/1
```
응답에서 "result": "deleted"가 반환되면 문서가 성공적으로 삭제된 것이다.
만약 존재하지 않는 ID로 삭제 요청을 보내면 "result": "not_found"가 반환된다.
