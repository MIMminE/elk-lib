[ Analyzer ]
---
`Analyzer Introduction`

데이터는 크게 구조화된 데이터와 비구조화된 데이터로 나눌 수 있다.
구조화된 데이터는 데이터베이스의 테이블처럼 미리 정의된 스키마를 따르고,
비구조화된 데이터는 텍스트 문서, 이메일 본문, 블로그 글 등 자유 형식의 텍스트에 해당한다.

관계형 데이터베이스는 구조화된 데이터를 저장하고, 미리 정의된 쿼리로 효율적으로 조회가 가능하다는 장점이 있지만,
대규모 자유 텍스트에서 정보를 검색하는 데는 한계가 있다. LIKE 연산자를 사용한 부분 문자열 검색은 성능이 떨어지고,
복잡한 텍스트 분석 기능이 부족하기 때문이다.

비구조화된 데이터에서 효과적으로 검색하려면 텍스트를 분석하고 색인화하는 과정이 필요하다.
Elasticsearch는 이러한 텍스트 분석을 위해 **Analyzer**라는 개념을 도입하였다.
Analyzer는 텍스트를 토큰(token)이라는 작은 단위로 분해하고, 불용어(stop words)를 제거하며,
필요에 따라 어간 추출(stemming) 등의 처리를 수행한다. 이 과정을 통해 텍스트 데이터가 검색에 적합한 형태로 변환된다.

앞으로 이러한 과정에 대해 구체적으로 살펴보는 시간을 갖도록 하겠다.

--- 
`Analyzer Components`

Analyzer 세 가지 구성 요소를 가지고 있다.

1. `Character Filter` : 텍스트에 대한 전처리를 수행한다. 예를 들어, HTML 태그를 제거하거나 특수 문자를 처리하는 작업을 수행할 수 있다.
2. `Tokenizer` : 텍스트를 토큰이라는 작은 단위로 분해하는 역할을 한다. 일반적으로 공백이나 구두점 등을 기준으로 텍스트를 나눈다.
3. `Token Filter` : 토큰에 대한 후처리를 수행한다. 예를 들어, 불용어 제거, 어간 추출, 소문자 변환 등의 작업을 수행할 수 있다.

최종적으로 새로운 문서를 추가할 때, 위 세 가지 구성 요소가 순차적으로 적용되어 텍스트가 분석되고 토큰으로 변환된다.
이렇게 생성된 토큰들은 **역색인(inverted index)** 에 저장되어 검색 시에 활용된다.

Analyzer는 기본적으로 **내장된 표준 분석기(standard analyzer)** 를 사용하지만, 사용자 정의 분석기를 만들어 특정 요구사항에 맞게 텍스트 분석을 커스터마이징할 수도 있다.
이때, 한 개 이상의 Tokenizer 는 필수 구성 요소이며, Character Filter 와 Token Filter 는 선택적으로 포함될 수 있다.

---
`Analzyer Clarification`

예시로 Json 문서가 다음과 같이 Elasticsearch에 추가된다고 가정해보자.

doc1 의 아이디를 가진다고 하자.

```json 
{
  "title": "The Quick Brown Foxes!",
  "content": "Jumped over the lazy dogs."
}
```

두 개의 필드가 있으며 Analyzer는 각 필드에 대해 분석을 수행하여 토큰을 생성한다.
'title' 필드에 대한 분석의 결과로 Quick, Brown, Foxes 라는 토큰이 생성되었다고 한다면, 'title'의 역색인에는 다음과 같은 매핑이 생성될 것이다.

```text
"Quick"  -> [ doc1 ]
"Brown"  -> [ doc1 ]
"Foxes"  -> [ doc1 ]
```

각 필드는 별도로 분석되기에 'content' 필드에 대한 분석의 결과로 Jumped, lazy, dogs 라는 토큰도 생성될 것이다.

중요한 점은 Analyzer는 소스 문서를 변경하거나 수정하지 않는다는 것이다.
즉, 원본 문서는 그대로 유지되며, 분석을 통해 생성된 토큰들이 역색인에 저장되어 검색에 활용된다.

- 문서가 저장될 때, 각 텍스트 필드는 독립적으로 분석되어 토큰화되고 역색인에 저장된다.
- 역색인이란, 각 토큰이 어떤 문서에 포함되어 있는지를 매핑하는 데이터 구조이다. 키워드를 통해 빠르게 문서를 찾을 수 있도록 도와준다.
- 원본 문서는 변경되지 않으며, 분석된 토큰들이 별도로 저장되어 검색에 활용된다.

--- 
`Character Filters`

Elasticsearch는 여러 가지 내장 Character Filter를 제공하며, _analyzer API를 통해 Character Filter의 동작을 테스트할 수 있다.
예를 들어, HTML 태그를 제거하는 `html_strip` Character Filter를 사용해보자.

```http request
GET /_analyze
{
  "char_filter": ["html_strip"],
  "text": "<b>The Quick Brown Foxes!</b>"
}
```

위의 html 구조 형태의 텍스트를 본문에 담아 _analyze API에 전달하였다.

```text
{
  "tokens": [
    {
      "token": "The Quick Brown Foxes!",
      "start_offset": 3,
      "end_offset": 29,
      "type": "word",
      "position": 0
    }
  ]
}
```

현재는 토큰이 하나만 반환되었다. 이는 Character Filter만 적용되었기 때문이다.
html_strip Character Filter가 HTML 태그를 제거하였고, 이후에 Tokenizer가 적용되지 않았기에 전체 텍스트가 하나의 토큰으로 반환되었다.

다음으로 **mapping Character Filter** 를 사용해보자. mapping Character Filter는 특정 문자열을 다른 문자열로 매핑하는 역할을 한다.
예를 들어, "quick"을 "fast"로 매핑하는 Character Filter를 적용해보자.

```http request
GET /_analyze
{
  "char_filter": [
    {
      "type": "mapping",
      "mappings": ["quick => fast"]
    }
  ],
  "text": "The quick brown foxes"
}
```

다음으로 **pattern_replace Character Filter** 를 사용해보자. 이 필터는 정규 표현식을 사용하여 텍스트의 특정 패턴을 다른 문자열로 대체하는 역할을 한다.

```http request
GET /_analyze
{
  "text": "At $100, the product is quite expensive.",
  "char_filter": [
    {
      "type": "pattern_replace",
      "pattern": "\\$(\\d+)",
      "replacement": "$1 dollars"
    }
  ]
}
```

여러 필터를 조합하여 사용할 수도 있다.

```http request
GET /_analyze
{
  "text": "<p>The quick brown foxes jump at $100!</p>",
  "char_filter": [
    "html_strip",
    {
      "type": "mapping",
      "mappings": ["quick => fast"]
    },
    {
      "type": "pattern_replace",
      "pattern": "\\$(\\d+)",
      "replacement": "$1 dollars"
    }
  ]
}
```

       
--- 
`Tokenizers`

Tokenizer는 텍스트를 토큰이라는 작은 단위로 분해하는 역할을 한다.
Elasticsearch는 여러 가지 내장 Tokenizer를 제공하며, _analyze API를 통해 Tokenizer의 동작을 테스트할 수 있다.
예를 들어, **standard tokenizer** 를 사용해보자.

```http request
GET /_analyze
{
  "text": "This is a sample text to see how tokens are generated.",
  "tokenizer": "standard"
}
```

결과로는 다음과 같은 토큰들이 생성된다.

```text
{
  "tokens": [
    { "token": "This", ... },
    { "token": "is", ... },
    { "token": "a", ... },
    { "token": "sample", ... },
    { "token": "text", ... },
    { "token": "to", ... },
    { "token": "see", ... },
    { "token": "how", ... },
    { "token": "tokens", ... },
    { "are", ... },
    { "generated", ... }
  ]
}
``` 

standard tokenizer는 공백과 구두점을 기준으로 텍스트를 분해하여 토큰을 생성한다.
이 상태에서 이전에 학습한 character filter를 추가로 적용할 수도 있다. 만약 html_strip character filter를 추가로 적용한다면 다음과 같이 된다.

```http request
GET /_analyze
{
  "text": "<b>This is a sample text to see how tokens are generated.</b>",
  "char_filter": ["html_strip"],
  "tokenizer": "standard"
}
```

결과는 이전과 동일한 토큰들이 생성된다. html_strip character filter가 HTML 태그를 제거하였고, standard tokenizer가 텍스트를 분해하여 토큰을 생성하였다.

다음으로 적용할 tokenizer는 uax_url_email tokenizer 이다. 이 토크나이저는 URL과 이메일 주소를 인식하여 하나의 토큰으로 처리한다.

```http request
GET /_analyze
{
  "text": "Reach out to Support at support@domain.com or send mail to 123,Non Main Street, Atlanta, for assistance!",
  "tokenizer": "uax_url_email"
}
```

이렇게 하면 이메일 주소와 URL이 하나의 토큰으로 처리된다.

```text
    ...
    {
      "token": "support@domain.com",
      "start_offset": 24,
      "end_offset": 42,
      "type": "<EMAIL>",
      "position": 5
    },
```

이외에도 whitespace tokenizer, keyword tokenizer, pattern tokenizer 등 다양한 토크나이저가 제공된다.

---
`Token Filters`

Token Filter는 토큰에 대한 후처리를 수행하는 역할을 한다.
Elasticsearch는 여러 가지 내장 Token Filter를 제공하며, _analyze API를 통해 Token Filter의 동작을 테스트할 수 있다.
예를 들어, **uppercase token filter** 를 사용해보자.

```http request
GET /_analyze
{
  "text": "This is a sample text to see how tokens are generated.",
  "tokenizer": "standard",
  "filter": [
    "uppercase"
  ]
}
```

결과로는 다음과 같은 토큰들이 생성된다.

```text
{
  "tokens": [
    { "token": "THIS", ... },
    { "token": "IS", ... },
    { "token": "A", ... },
    { "token": "SAMPLE", ... },
    { "token": "TEXT", ... },
    { "token": "TO", ... },
    { "token": "SEE", ... },
    { "token": "HOW", ... },
    { "token": "TOKENS", ... },
    { "token": "ARE", ... },
    { "token": "GENERATED", ... }
  ]
}
```

uppercase token filter가 각 토큰을 대문자로 변환하였다.

다음으로 length token filter 를 사용해보자. 이 필터는 토큰의 길이에 따라 필터링을 수행한다.

```http request
GET /_analyze
{
  "text": "This is a sample text to see how tokens are generated.",
  "tokenizer": "standard",
  "filter": [
    {
      "type": "length",
      "min": 4,
      "max": 6
    }
  ]
}
```

결과로는 길이가 4에서 6 사이인 토큰만 필터링되어 생성한다.

```text
{
  "tokens": [
    { "token": "This", ... },
    { "token": "sample", ... },
    { "token": "text", ... },
    { "token": "tokens", ... }
  ]
}
```

이외에도 stop token filter, stemmer token filter, synonym token filter 등 다양한 토큰 필터가 제공된다.

---
`Synonym Filter`

Synonym Token Filter는 토큰의 동의어를 처리하는 역할을 한다.
예를 들어, "quick"이라는 단어의 동의어로 "fast"와 "speedy"를 정의할 수 있다.

```http request
GET /_analyze
{
  "text": "The quick brown fox jumps over the lazy dog.",
  "tokenizer": "standard",
  "filter": [
    {
      "type": "synonym",
      "synonyms": [
        "quick, fast, speedy"
      ]
    }
  ]
}
```

이렇게 하면 fast 와 speedy 라는 토큰도 함께 생성된다. 이런식으로 하는 것은 동의어를 확장하는 것이고,
동의어를 치환하는 것도 가능하다. 예를 들어, "jumps"를 "leaps"로 치환할 수 있다.

```http request
GET /_analyze
{
  "text": "The quick brown fox jumps over the lazy dog.",
  "tokenizer": "standard",
  "filter": [
    {
      "type": "synonym",
      "synonyms": [
        "jumps => leaps"
      ]
    }
  ]
}
```

동의어 치환이란 특정 단어를 다른 단어로 대체하는 것을 의미한다. 토큰 자체가 변경되는 것이므로 jumps 토큰은 더 이상 존재하지 않고, 대신 leaps 토큰이 생성된다.

주의할 점은 동의어 사전의 품질이 검색 품질에 직접적인 영향을 미친다는 것이다. 도메인에 맞는 동의어 리스트를 지속적으로 관리해야 한다.

---
`Stop Words Filter`

Stop Words Filter는 자주 등장하지만 검색에 큰 의미가 없는 단어(불용어)들을 제거하는 역할을 한다.
예를 들어, "the", "is", "at", "which", "on" 등의 단어들이 불용어에 해당한다.

```http request
GET /_analyze
{
  "text": "The quick brown fox jumps over the lazy dog.",
  "tokenizer": "standard",
  "filter": ["stop"]
}
```

일반적으로 사용되는 불용어 토큰들이 제거된다.

도메인에 따라 일반적인 불용어라고 해도 의미가 있을 수 있으며, 커스텀 불용어 목록을 정의하여 사용하는 것도 가능하다.

```http request
GET /_analyze
{
  "text": "The quick brown fox jumps over the lazy dog.",
  "tokenizer": "standard",
  "filter": [
    {
      "type": "stop",
      "stopwords": ["the", "over", "jumps"]
    }
  ]
}
``` 

---
`Stemming Filter`

Stemming Filter는 단어의 어간(stem)을 추출하는 역할을 한다. 예를 들어, "running", "runner", "ran" 등의 단어들은 모두 "run"이라는 어간을 공유한다.
단수/복수를 통일하거나, 동사 형태를 기본형으로 변환하는 데 유용하다.

```http request
GET /_analyze
{
  "text": "running runner ran easily fairies",
  "tokenizer": "standard",
  "filter": [
    "stemmer"
  ]
}
```

결과로는 다음과 같은 토큰들이 생성된다.

```text
{
  "tokens": [
    { "token": "run", ... },
    { "token": "runner", ... },
    { "token": "ran", ... },
    { "token": "easili", ... },
    { "token": "fairi", ... }
  ]
}
``` 

Analyzer는 검색 시에도 동일하게 적용될 수 있으므로 검색어에 대해서도 어간 추출을 통해 일관된 검색이 가능하다.

---
`Analyzers`

Elasticsearch는 몇 가지 기본 분석기를 제공하며 대부분의 경우에 적합하게 사용이 가능하다.

- `standard analyzer` : 기본 분석기로, 표준 토크나이저와 소문자 변환 필터를 사용한다.
- `simple analyzer` : 공백 토크나이저와 소문자 변환 필터를 사용한다.
- `whitespace analyzer` : 공백 토크나이저만 사용하며, 토큰 필터는 적용하지 않는다.
- `stop analyzer` : 공백 토크나이저와 소문자 변환 필터, 불용어 필터를 사용한다.
- `keyword analyzer` : 키워드 토크나이저만 사용하며, 토큰 필터는 적용하지 않는다.

_analyze API에 별도의 분석기를 지정하지 않으면 standard analyzer가 기본으로 사용된다.

```http request
GET /_analyze
{
  "text": "The quick brown fox jumps over the lazy dog.",
  "analyzer": "standard" // 생략 가능
}
``` 

결과로는 standard analyzer가 적용된 토큰들이 생성된다.

---
`Custom Analyzer`

기본 제공되는 분석기가 모든 요구사항을 충족하지 못할 수 있다.
이럴 때는 사용자 정의 분석기를 만들어 특정 요구사항에 맞게 텍스트 분석을 커스터마이징할 수 있다.
사용자 정의 분석기는 Character Filter, Tokenizer, Token Filter를 조합하여 구성된다.

인덱스를 생성할 때 사용자 정의 분석기를 설정할 수 있다.

```http request
PUT /my_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "custom_analyzer": {
          "type": "custom",
          "char_filter": ["html_strip"],
          "tokenizer": "standard",
            "filter": ["lowercase", "stop", "stemmer"]
        }
      }
    }
  }
}
```

이렇게 생성된 사용자 정의 분석기는 my_index 인덱스에 저장되는 문서에 대해 적용된다.
인덱스 생성 후 _analyze API를 통해 동작을 테스트할 수도 있다.

```http request
GET /my_index/_analyze
{
  "analyzer": "custom_analyzer",
  "text": "<b>The quick brown foxes!</b>"
}   
```

---
`Summary`

이번 섹션에서는 Elasticsearch의 Analyzer에 대해 살펴보았다.
Analyzer는 인덱싱과 검색 시 텍스트를 분석하는 구성 요소이며, 텍스트를 효율적으로 검색 가능한 토큰으로 분해한다.
Analyzer는 Character Filter, Tokenizer, Token Filter로 구성되며, 최종 토큰은 역색인(inverted index)에 저장된다.

Character Filter는 텍스트에 대한 전처리를 수행하며 커스텀 구성에 있어서 선택적으로 포함될 수 있다.
Tokenizer는 텍스트를 토큰 단위로 분해하는 역할을 하며, 커스텀 구성에 있어서 하나 이상 반드시 포함되어야 한다.
Token Filter는 토큰에 대한 후처리를 수행하며 커스텀 구성에 있어서 선택적으로 포함될 수 있다.

Elasticsearch는 여러 가지 내장 분석기를 제공하며, 대부분의 경우에 적합하게 사용이 가능하다.

- **Character Filter** : html_strip, mapping, pattern_replace 등
- **Tokenizer** : standard, uax_url_email, whitespace, keyword, pattern 등
- **Token Filter** : uppercase, length, stop, stemmer, synonym 등

사용자 정의 분석기를 만들어 특정 요구사항에 맞게 텍스트 분석을 커스터마이징할 수도 있다. 이는 인덱스 생성 시 설정할 수 있으며 인덱스에 종속적이다.