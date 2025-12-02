[ Data Mapping ]
---
`Data Mapping - Introduction`

매핑(Mapping)은 Elasticsearch에서 데이터의 구조와 형식을 정의하는 중요한 개념이다. 매핑을 통해 각 필드의 데이터 타입, 분석기 설정, 인덱싱 옵션 등을 지정할 수 있다. 이를 통해 검색 성능을
최적화하고, 데이터의 일관성을 유지할 수 있다.

첫 번째는 동적 매핑(Dynamic Mapping)으로, Elasticsearch가 자동으로 필드와 데이터 타입을 추론하여 매핑을 생성하는 방식이다.
두 번째는 명시적 매핑(Explicit Mapping) 으로, 사용자가 직접 매핑을 정의하는 방식이다.

주로 사용되는 데이터 타입:

- `text` : 분석된 문자열 데이터를 저장하는 데 사용된다. 주로 전체 텍스트 검색에 적합하다.
- `keyword` : 분석되지 않은 문자열 데이터를 저장하는 데 사용된다. 주로 필터링, 정렬, 집계에 적합하다.
- `integer`, `float`, `date` 등 : 숫자 및 날짜 데이터를 저장하는 데 사용된다.

keyword 타입은 분석이 적용되지 않으며 키워드 자체로 저장된다. 따라서 정확한 일치 검색에 적합하다. 반면에 text 타입은 분석기를 통해 토큰화되어 저장되므로 전체 텍스트 검색에 유리하다.

---
`Dynamic Mapping`

동적 매핑은 Elasticsearch가 문서를 인덱싱할 때 자동으로 필드와 데이터 타입을 추론하여 매핑을 생성하는 기능이다. 예를 들어, 다음과 같은 문서를 인덱싱할 때:

```http request
POST /books/_doc
{
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald",
    "year": 1925,
    "genre": "Fiction",
    "rating": 4.9,
    "last_modified": "2024-06-01T12:00:00"
}
```

Elasticsearch는 각 필드의 값을 분석하여 적절한 데이터 타입을 자동으로 할당한다.
예를 들어, "title"과 "author" 필드는 `text` 타입으로, "year"는 `integer` 타입으로, "rating"은 `float` 타입으로, "last_modified"는 `date` 타입으로
매핑된다.

```http request
GET /my-index/_mapping
```

매핑 정보를 조회하여 동적 매핑이 어떻게 생성되었는지 확인할 수 있다.

```json
{
  "my-index": {
    "mappings": {
      "properties": {
        "title": {
          "type": "text"
        },
        "author": {
          "type": "text"
        },
        "year": {
          "type": "integer"
        },
        "genre": {
          "type": "text"
        },
        "rating": {
          "type": "float"
        },
        "last_modified": {
          "type": "date"
        }
      }
    }
  }
}
```

동적 매핑은 대체로 올바르게 매핑되지만, 가끔 잘못된 타입으로 매핑되어 문제를 야기하기도 한다.

```http request
POST /my-index/_doc
{
 "phone": 1234567890
}
```

위 예제에서 "phone" 필드는 숫자로 인식되어 `long` 타입으로 매핑된다. 그러나 전화번호는 일반적으로 문자열로 취급되어야 하므로, 이 경우에는 명시적 매핑을 사용하는 것이 좋다.

또한, 다음에 들어오는 문서가 타입 변환이 가능한 형태라면 문제를 일으키지 않으므로 주의가 필요하다.

```http request
POST /my-index/_doc
{
 "phone": "1234567890"
}
```

이미 `long` 타입으로 매핑된 "phone" 필드에 문자열이 들어오지만, 숫자로 변환이 가능한 형태이므로 저장이 가능하다.
**(하지만 _source 필드에는 문자열로 저장된다. 이는 검색 시 혼란을 야기할 수 있으므로 바람직한 상황은 아니다.)**

실무에서는 개발 단계에서는 동적 매핑으로 시작할 수 있지만, 운영 환경에서는 명시적 매핑을 정의하여 사용하는 것이 좋다.

--- 
`Explicit Mapping`

명시적 매핑은 사용자가 직접 매핑을 정의하는 방식으로, 데이터의 구조와 형식을 명확하게 지정할 수 있다.
인덱스를 생성할 때 매핑을 함께 정의한다. 예를 들어, 다음과 같이 "books" 인덱스를 생성하면서 매핑을 정의할 수 있다.

```http request
PUT /books
{
  "mappings": {
    "properties": {
      "title": { "type": "text" },
      "author": { "type": "keyword" },
      "year": { "type": "integer" },
      "genre": { "type": "keyword" },
      "email": { "type": "keyword" },
      "rating": { "type": "float" },
      "last_modified": { "type": "date" }
    }
  }
}
```

위 예제에서는 "author"와 "genre" 필드를 `keyword` 타입으로 명시적으로 지정하여, 정확한 일치 검색과 필터링에 적합하도록 설정하였다.
명시적 매핑을 사용하면 데이터의 일관성을 유지하고, 검색 성능을 최적화할 수 있다. 특히, 전화번호와 같이 특정 형식이 필요한 필드에 대해 올바른 타입을 지정하는 것이 중요하다.

date 타입은 내부적으로 long 타입으로 관리하지만 BKD 트리 구조로 인덱싱되어 범위 검색에 최적화되어 있다.
**BKD(Block K-Dimensional Tree)** 트리는 다차원 공간에서 효율적인 범위 쿼리를 지원하는 데이터 구조이다. 날짜는 특정 범위 내에서 검색하는 경우가 많기 때문에, BKD 트리를 사용하여 날짜
필드를
인덱싱하면 범위 검색 성능이 크게 향상된다.

한번 만들어진 인덱스에는 동적으로 새로운 필드를 추가할 수 있지만, 기존 필드의 데이터 타입이나 매핑 설정은 변경할 수 없다.
하지만, 운영 환경에서는 의도치 않은 동적 필드 추가를 방지하기 위해 매핑을 엄격하게 설정하는 것이 좋다.

--- 
`Field Possible Values`

Elasticsearch 에서는 인덱스의 필드를 명시적으로 매핑하였더라도, 문서를 색인할 때마다 그 값을 꼭 넣어야 하는 것은 아니다.
모든 필드는 선택적이며, 이는 Not Null를 강제할 수 없다는 의미이다. 또한, Integer 타입 필드에 숫자로 된 배열이나 빈 배열, null 값도 허용된다.
즉, 필드에 정의된 타입들의 집합 중 하나의 값이 들어올 수 있다는 의미이다.

```http request
PUT /customers
{
  "mappings": {
    "properties": {
      "name": {
        "type": "text"
      },
      "score": {
        "type": "integer"
      }
    }
  }
}
```

위와 같이 매핑된 인덱스에 다음과 같은 문서들을 색인할 수 있다.

```http request
POST /customers/_bulk
{ "create": {} }
{ "name": "sam", "score": 15 }
{ "create": {} }
{ "name": "mike", "score": null }
{ "create": {} }
{ "name": "jake", "score": [] }
{ "create": {} }
{ "name": "john", "score": [10, 15, 20] }
{ "create": {} }
{ "name": "nancy" }
```

---
`Using Custom Analyzer`

기본적으로 Elasticsearch는 `standard` 를 사용하지만, 특정 필드에 대한 맞춤형 분석기를 정의할 수도 있다.
즉, 전체적으로 기본 분석기를 사용하되, 특정 필드에 대해서만 별도의 분석기를 적용할 수 있다.

```http request
PUT /blogs
{
 "settings": {
   "analysis": {
    "analyzer": {
      "html-strip-stem-standard": {
        "type": "custom",
        "char_filter": [
           "html_strip"
        ],
        "tokenizer": "standard",
        "filter": [
          "lowercase",
          "stemmer"
        ]
      }
    }
   }
 },
 "mappings": {
   "properties": {
       "title": {
         "type": "text"
       },
       "body": {
         "type": "text",
         "analyzer": "html-strip-stem-standard"
       }
   }
 }
}
```

위 예제에서는 "body" 필드에 대해 HTML 태그를 제거하는 `html_strip` 문자 필터와 어간 추출을 수행하는 `stemmer` 토큰 필터를 포함한
맞춤형 분석기를 정의하였다. 이를 통해 "body" 필드에 저장되는 텍스트는 HTML 태그가 제거되고, 소문자로 변환된 후 어간 추출이 적용된다.

추가적으로 만약 위 예제에서 "title" 필드에도 동일한 분석기를 적용하고 싶다면, "title" 필드에도 `analyzer` 속성을 추가하여 지정할 수 있다.

---
`Skipping A Field`

엘라스틱 서치는 필드 타입에 따라 모든 필드를 인덱싱하려고 시도한다. 하지만 때로는 검색에 사용하지 않을 필드에 대해 인덱싱을 건너뛰고 싶을 수 있다.
인덱싱에 포함되지 않은 필드는 검색이나 집계에 사용할 수 없지만, `_source` 필드에는 여전히 저장된다.
이는 불필요한 인덱싱 오버헤드를 줄이고 저장 공간을 절약하는 데 도움이 된다.

실무적으로 필드를 인덱싱에서 제외하는 방법은 두 가지 정도가 있다.

1. 애플리케이션 측에서 필터링을 거쳐 문서를 색인할 때 제외할 필드를 제거하는 방법
2. 엘라스틱 서치 매핑 설정에서 비활성화하는 방법

2번 방법은 enabled 속성을 false 로 설정하는 것이다.

```http request
PUT /blogs
{
 "mappings": {
   "properties": {
       "title": {
         "type": "text"
       },
       "body": {
         "type": "object", // object 필드로 지정하는 이유는 아무것도 하지 않음을 명시하기 위함
         "enabled": false
       }
   }
 }
}
```

위 예제에서 "body" 필드는 `enabled` 속성을 `false` 로 설정하여 인덱싱에서 제외되었다. 따라서 "body" 필드의 내용은 검색이나 집계에 사용할 수 없지만,
`_source` 필드에는 여전히 저장된다.

1번 방법은 애플리케이션에서 문서를 색인하기 전에 제외할 필드를 제거하는 것이다.
이렇게 하면 `_source` 필드에도 해당 필드가 저장되지 않는다.

---

`1-To-1 Parent Child Mapping`

단순한 평면 JSON이 아닌, 내부에 중첩된 객체 구조를 가지는 문서를 다루어야 할 때가 있다. 사실 실무에서는 이런 경우가 매우 흔하다.
예를 들어 영화(movie) 문서가 있고 그 안에 감독(director) 문서가 있는 구조일때, 다음과 같이 매핑을 정의할 수 있다.

```http request
PUT /movies
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text"
      },
      "yearReleased": {
        "type": "integer"
      },
      "director": {
        "properties": {
            "name": {
                "type": "text"
            },
            "country": {
                "type": "keyword"
            }
        }
      }
    }
  }
}
```
위 예제에서 "director" 필드는 중첩된 객체로 정의되어 있으며, "name"과 "country" 필드를 포함하고 있다.
이렇게 하면 영화 문서 내에서 감독 정보를 함께 저장할 수 있다. 
중첩된 객체는 별도의 인덱스로 관리되지 않고, 상위 문서의 일부로 저장된다. 따라서 검색 시에도 상위 문서와 함께 검색된다.

---
`1-To-Many Parent Child Mapping`

때로는 1 대 다 관계를 표현해야 할 때가 있다. 영화(movie) 문서가 있고, 그 안에 여러 배우(actor) 문서가 있는 구조일 때 다음과 같이 매핑을 정의할 수 있다.

```http request
PUT /movies
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text"
      },
      "yearReleased": {
        "type": "integer"
      },
      "actors": {
        "type": "nested", // important
        "properties": {
            "name": {
                "type": "text"
            },
            "role": {
                "type": "text"
            }
        }
      }
    }
  }
}
```
위 예제에서 "actors" 필드는 `nested` 타입으로 정의되어 있으며, 각 배우의 "name"과 "role" 필드를 포함하고 있다.
`nested` 타입은 중첩된 객체 배열을 표현하는 데 사용되며, 각 객체는 독립적으로 인덱싱되고 검색될 수 있다.
이를 통해 영화 문서 내에서 여러 배우 정보를 함께 저장할 수 있다. 
`nested` 타입을 사용하면 검색 시에도 각 배우 객체를 독립적으로 검색할 수 있다.

```http request
POST /movies/_bulk
{ "create": {} }
{ "title": "Inception", "actors": [ { "name": "Leonardo DiCaprio", "role": "Cobb" }, { "name": "Joseph Gordon-Levitt", "role": "Arthur" } ], "yearReleased": 2010 }
{ "create": {} }
{ "title": "The Dark Knight", "actors": [ { "name": "Christian Bale", "role": "Bruce Wayne / Batman" }, { "name": "Heath Ledger", "role": "Joker" } ], "yearReleased": 2008 }
{ "create": {} }
{ "title": "Interstellar", "actors": [ { "name": "Matthew McConaughey", "role": "Cooper" }, { "name": "Anne Hathaway", "role": "Brand" } ], "yearReleased": 2014 }
```

위 예제에서는 세 개의 영화 문서를 한 번에 추가하는 요청을 보내고 있다. 각 영화 문서에는 여러 배우 객체가 포함되어 있다.
위 예제서 단순히 q=joker 로 검색하면 "The Dark Knight" 문서가 검색되지 않는다.
이는 "joker" 라는 단어가 "role" 필드에 포함되어 있지만, "role" 필드가 `nested` 타입으로 정의되어 있기 때문이다.
`nested` 타입은 독립적인 객체로 인덱싱되므로, 단순한 쿼리로는 검색되지 않는다. 이는 추후에 구체적으로 다룬다.

---
`Summary`

데이터 매핑은 '문서가 어떤 필드를 포함하고, 각 필드의 데이터 타입이 무엇인지'를 정의하는 과정이다. 
크게 동적 매핑과 명시적 매핑으로 나눌 수 있으며, 실무에서는 운영 환경에서 명시적 매핑을 사용하는 것이 좋다.
데이터 타입에 따라 검색 성능과 저장 방식이 달라지므로, 각 필드에 적절한 타입을 지정하는 것이 중요하다.

- `text` : 텍스트가 들어 있고, 부분 텍스트로 검색하고 싶을 떄 사용한다. 분석기로 토큰화되어 액색인에 저장된다.
- `keyword` : 텍스트가 들어 있고, 정확히 일치하는 값으로 검색하고 싶을 때 사용한다. 분석되지 않고 키워드 자체로 저장된다.
- `date` : 날짜가 들어 있을 때 사용한다. 내부적으로 long 타입으로 관리되지만 BKD 트리 구조로 인덱싱되어 범위 검색에 최적화되어 있다.
- `integer`, `float` 등 : 숫자가 들어 있을 때 사용한다.

필드의 값은 선택적이며, null 값이나 빈 배열, 해당 타입의 배열도 허용한다.

내부 객체 구조를 표현할 때는 1-대-1 관계는 중첩 객체로, 1-대-다 관계는 `nested` 타입으로 정의할 수 있다.
1-대-1 관계는 상위 문서의 일부로 저장되며, 1-대-다 관계는 독립적인 객체로 인덱싱되므로 특별한 검색 방법이 필요하다.
