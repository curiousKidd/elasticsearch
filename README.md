# elasticsearch

Elasticsearch - 검색엔진, Data 저장소, Restful 검색엔진 
Logstash - 파이프라인, Data수집, 데이터 전송 
Kibana - Data 시각화, 프론트엔드


| ELK | RDBMS |
|---|---|
|index|database|
|type|table|
|document|row|
|field|column|
|mapping|schema|

# 2-1 인덱스 샤드 모니터링
## 인덱스와 샤드
- 프라이머리 샤드(Primary Shard)와 복제본(Replica)
- 인덱스의 settings 설정에서 샤드 갯수 지정
- _cat/shards API를 이용한 샤드 상태 조회

## 모니터링 도구를 이용한 클러스터 모니터링
- Kibana의 모니터링 도구 실행 및 확인
- _cluster/settings API를 이용한 모니터링 실행/중지
> https://esbook.kimjmin.net/03-cluster/3.2-index-and-shards

### 인덱스 생성
``` java
// Request
PUT /books
{
  "settings": {
    "number_of_shards": 5,
    "number_of_replicas": 1
  }
}'

// Response
{
  "acknowledged" : true,
  "shards_acknowledged" : true,
  "index" : "books"
}
```

- 한번 만들어진 index는 같은 이름으로 생성 불가능하다.
- number_of_shards는 index 생성 후 변경X
- number_of_replicas 는 중간에 변경 O

 ``` java
PUT books/_settings
{
    "number_of_replicas": 0
}
```

### 구성확인
``` java
// Request
GET _cat/nodes?v&h=ip,name,node.role

// Response
ip        name   node.role
127.0.0.1 node-1 cdfhilmrstw
```
- &h : header

### 모니터링 셋팅 끄기
``` java
// Request
GET _cluster/settings

// Response
{
  "persistent" : {
    "xpack" : {
      "monitoring" : {
        "collection" : {
          "enabled" : "true"
        }
      }
    }
  },
  "transient" : { }
}
```
  
``` java
// Request
PUT _cluster/settings
{
  "persistent" : {
    "xpack" : {
      "monitoring" : {
        "collection" : {
          "enabled" : "false"
        }
      }
    }
  }
}

// Response
{
  "acknowledged" : true,
  "persistent" : {
    "xpack" : {
      "monitoring" : {
        "collection" : {
          "enabled" : "false" // "true": 재실행, null 완전삭제
        }
      }
    }
  },
  "transient" : { }
}
```

# 2-2 도큐먼트 CRUDS : 입력, 조회, 수정, 삭제, 검색
### REST API로 도큐먼트 접근
- 입력|조회|삭제 : PUT|GET|DELETE {_index}/_doc/{_id}
- 업데이트 : POST {_index}/_update/{_id}
- 벌크 명령 : POST _bulk

### _search API로 도큐먼트 검색
- URI 검색 : ?q=...쿼리...
- 데이터 본문 검색 : {"query" : {...쿼리...}}
> https://esbook.kimjmin.net/04-data/4.1-rest-api

### bulk로 데이터 넣기
``` java
POST _bulk
{"index":{"_index":"test", "_id":"1"}}
{"field":"value one"}
{"index":{"_index":"test", "_id":"2"}}
{"field":"value two"}
{"delete":{"_index":"test", "_id":"2"}}
{"create":{"_index":"test", "_id":"3"}}
{"field":"value three"}
{"update":{"_index":"test", "_id":"1"}}
{"doc":{"field":"value two"}}
{"index":{"_index":"test", "_id":"2"}}
{"field":"value two"}
```

### index data 복사하기 (_reindex)

Elasticsearch에서 reindex를 사용하기 위해서는 두가지 사항을 알고 있어야 한다.
- _source 는 enabled 상태인 index만 reindex가 가능하다.
- reindex는 복사가 아니다. 즉 reindex를 하려는 index의 mapping정보도 똑같이 카피되는 것이 아니라, 데이터만 넣기 때문에 이때 디폴트 매핑으로 인덱스가 생성되기 때문에 반드시 reindex 할 곳에 mapping작업도 원본과 동일하게 해주어야 한다.

``` java
POST _reindex
{
    "source": {
        "index": "가져올 인덱스명"
    },
    "dest": {
        "index": "저장할 인덱스명"
    }
}
```

remote 된 곳이 인덱스도 가져올 수 있다.
``` java
POST _reindex
{
    "source": {
        "remote": {
            "host": "http://remotehost:9200",
            "username": "user",
            "password": "pass"
        },
        "index": "old_index"
    },
    "dest": {
        "index": "new_index"
    }
}
```

`username`, `password` 는 옵션이다. 만약 https 설정이 안되어 있다면 필요없다. 그런데 원격서버에서 인덱스를 가져와서 reindex를 하려면 원격서버의 elasticsearch에 `elasticsearch.yml` 에 다음과 같이 접근가능한 elasticsearch 주소를 기재해야 한다.
> reindex.remote.whitelist: "otherhost:9200, another:9200, 127.0.10.*:9200, localhost:*"

원한다면 쿼리를 주어서 가져올 수도 있다.
``` java
POST _reindex
{
    "source": {
        "remote": {
            "host": "https://remotehost:9200",
            "username": "user",
            "password": "pass"
        },
        "index": "old_index",
        "query": {
            "match": {
                "field_name": "field_value"
            }
        }
    },
    "dest": {
        "index": "new_index"
    }
}
```

우리가 같은 index에 대한 reindex를 여러번 할때마다 document version 필드가 업데이트가 된다. 
기존꺼를 지우고 다시 인서트된다는 의미이다. (insert)

매번 모든데이터를 지웠다가 다시 입력해야할 필요는 없다.
index 데이터중에서 update한 데이터만을 입력하고 싶은 경우가 있을 것이다.
그럴 경우에는 옵션을 추가로 설정해 주어야한다

#### version_type (default = external)
``` java
// request
POST /_reindex
{
  "source": {
    "index": "twitter"
  },
  "dest": {
    "index": "new_twitter",
    "version_type": "external"
  }
}

// version_type=’external’ 로 처리한다면 아래와 같은 버전 충돌 메시지가 나타난다.

// response
{
  "took": 2,
  "timed_out": false,
  "total": 8,
  "updated": 0,
  "created": 0,
  "deleted": 0,
  "batches": 1,
  "version_conflicts": 8,
  "noops": 0,
  "retries": {
    "bulk": 0,
    "search": 0
  },
  "throttled_millis": 0,
  "requests_per_second": -1,
  "throttled_until_millis": 0,
  "failures": [
    {
      "index": "new_twitter",
      "type": "_doc",
      "id": "5",
      "cause": {
        "type": "version_conflict_engine_exception",
        "reason": "[_doc][5]: version conflict, current version [7] is higher or equal to the one provided [1]",
        "index_uuid": "D8jYKopoQjS4bVTZH7MqlQ",
        "shard": "1",
        "index": "new_twitter"
      },
      "status": 409
    },
    .....중략......
```

old_index에 document가 추가/수정되면 `version_type=’external’` 처리하면 이미 등록된 id는 버전충돌이 되서 업데이트가 되지 않고 **새로 추가/수정된 document**만 입력되는 것이다. 만약 저 에러나는 메시지가 보고 싫고 그냥 수정이나 추가된 document만 제대로 반영되기만 하면 된다면 아래와 같이 **옵션(conflicts)** 을 추가하면 된다.

``` java
// request
POST /_reindex
{
  "conflicts": "proceed", 
  "source": {
    "index": "twitter"
  },
  "dest": {
    "index": "new_twitter",
    "version_type": "external"
  }
}

// response
{
  "took" : 9,
  "timed_out" : false,
  "total" : 9,
  "updated" : 0,
  "created" : 1,
  "deleted" : 0,
  "batches" : 1,
  "version_conflicts" : 8,
  "noops" : 0,
  "retries" : {
    "bulk" : 0,
    "search" : 0
  },
  "throttled_millis" : 0,
  "requests_per_second" : -1.0,
  "throttled_until_millis" : 0,
  "failures" : [ ]
}
```

#### op_type: create
**op_type의 값은 create 밖에 없다.**
``` java
POST /_reindex
{
  "source": {
    "index": "twitter"
  },
  "dest": {
    "index": "new_twitter",
    "op_type": "create"
  }
}
```
`op_type: create` 는 reindex를 한 후에 다시 reindex를 하더라도, 새롭게 추가된 데이터만 추가된다.
** 수정된 데이터는 반영되지 않는다 **
이것도 역시 수행시 같은 document가 있다면 version conflict 에러가 나고 그 메시지를 피하기 위해서 conflicts: proceed 를 사용하면 된다.

아래와 같이 reindex를 한 후에 원본에 doucment을 업데이트해보자.
``` java
PUT /twitter/_doc/9
{
  "counter":6,
  "tag": "8899aaaa2222"
}

// request
GET /new_twitter/_doc/9

// response
"_index" : "new_twitter",
  "_type" : "_doc",
  "_id" : "9",
  "_version" : 1,
  "found" : true,
  "_source" : {
    "counter" : 6,
    "tag" : "8899aaaa"
  }
  
```
**새로 추가된 document만 reindex시 반영된다**. 
실제 운영시 version_type, op_type를 용도에 맞게 적절하게 사용하면 원하는 결과를 얻을 수 있을 것이다.

기억해야할 마지막 부분은 reindex는 scroll batch로 돌아간다는 것이다. 
즉 default size=1000 개로 나누어서 가져온다. 이 사이즈는 아래와 같이 조정이 가능하다.
``` java
POST /_reindex
{
  "source": {
    "index": "twitter",
    "size": 100
  },
  "dest": {
    "index": "new_twitter"
  }
}
```

### 인덱스를 조회하는것
``` java
GET test/_doc/1
```

``` java
{
  "_index" : "test",
  "_type" : "_doc",
  "_id" : "1",
  "_version" : 2,
  "_seq_no" : 4,
  "_primary_term" : 1,
  "found" : true,
  "_source" : {
    "field" : "value two"
  }
}
```
### two라는 값이 어느 인덱스이 있는지 확인
``` java
GET test/_search?q=two

GET test/_search?q=field:two
// 이렇게 직접 field명을 지정할 수도 있다.

GET test/_search?q=field:two AND field:value
// AND 조건 넣기
```

``` java
{
  "took" : 4,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 2,
      "relation" : "eq"
    },
    "max_score" : 0.53899646,
    "hits" : [
      {
        "_index" : "test",
        "_type" : "_doc",
        "_id" : "1",
        "_score" : 0.53899646,
        "_source" : {
          "field" : "value two"
        }
      },
      {
        "_index" : "test",
        "_type" : "_doc",
        "_id" : "2",
        "_score" : 0.53899646,
        "_source" : {
          "field" : "value two"
        }
      }
    ]
  }
}
```

하지만, 위처럼 조회하는것보단 Body에 데이터를 담아 조회한다.

# 2-3-1 Query DSL : 풀텍스트
### 풀텍스트(Full Text) 쿼리
- match
- match_phrase
- query_string

### Relevancy (연관성, 정확도)
- TF / IDF
> https://esbook.kimjmin.net/05-search

``` java
POST my_index/_bulk
{"index":{"_id":1}}
{"message":"The quick brown fox"}
{"index":{"_id":2}}
{"message":"The quick brown fox jumps over the lazy dog"}
{"index":{"_id":3}}
{"message":"The quick brown fox jumps over the quick dog"}
{"index":{"_id":4}}
{"message":"Brown fox brown dog"}
{"index":{"_id":5}}
{"message":"Lazy jumping dog"}

GET my_index/_doc/1

GET my_index/_search
GET my_index/_search
{
  "query": {
    "match": {
      "message": "dog"
    }
  }
}

// quick OR dog
GET my_index/_search
{
  "query": {
    "match": {
      "message": "quick dog"
    }
  }
}

// quick AND doc 따로든 같이든 있는것
GET my_index/_search
{
  "query": {
    "match": {
      "message": {
        "query": "quick dog",
        "operator": "and"
      }
    }
  }
}

// "quick dog" 이 구문을 가지고오고싶은경우
GET my_index/_search
{
  "query": {
    "match_phrase": {
      "message": "quick dog"
    }
  }
}


// quick dog 사이에 허용하는 단여 갯수 
// quick dog, quick fast dog
GET my_index/_search
{
  "query": {
    "match_phrase": {
      "message": {
        "query": "quick dog",
        "slop": 1
      }
    }
  }
}
```

## 5.3 정확도 - Relevancy
### 사용자가 입력한 검색어와 가장 연관성 있는지를 계산하여 score 점수 체크하는 방법
- TF (Term Frequency)
- IDF (Inverse Document Frequency)
- Field Length
> https://esbook.kimjmin.net/05-search/5.3-relevancy

# Query DSL : Bool, Range
### 복합 (Bool) 쿼리
> https://esbook.kimjmin.net/05-search/5.2-bool
- must : 쿼리가 참인 도큐먼트들을 검색합니다.
- must_not : 쿼리가 거짓인 도큐먼트들을 검색합니다.
- should : 검색 결과 중 이 쿼리에 해당하는 **도큐먼트의 점수를 높입니다. **
  - 여기에 들어간 값은 있어도되고 없어도 되는데, 있으면 score을 높인다.
- filter : 쿼리가 참인 도큐먼트를 검색하지만 스코어를 계산하지 않습니다. must 보다 검색 속도가 빠르고 캐싱이 가능합니다.
  - 여기에 있는 값은 있어야하지만, 점수에는 영향 X
  
### 쿼리에 따른 검색 스코어의 변화

### 범위(Range) 쿼리
- 숫자값 / 날짜의 범위 검색

### 복합(Bool)쿼리 예제
``` java
GET my_index/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "message": {
              "query": "fox" // 필수데이터, 있으면 점수 높임
            }
          }
        },
        {
          "match": {
            "message": "quick" // 필수데이터, 있으면 점수 높임
          }
        }
      ],
      "should": [
        {
          "match": {
            "message": {
              "query": "lazy" // lazy 없어도됨, 있으면 점수 높임
            }
          }
        }
      ],
      "filter": [
        {
          "match" :{
            "message":"lazy" // lazy 무조건 있어야함, 점수 영향X
          }
        }
      ]
    }
  }
}
```

IMAGE

``` java
GET phones/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 400,
        "lte": 800
      }
    }
  }
}

// range 여러개 하고싶을땐 bool must 사용
GET phones/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "range": {
            "price": {
              "gte": 400,
              "lte": 700
            }
          }
        },
        {
          "range": {
            "date": {
              "gte": "2010-01-01",
              "lte": "2015-01-01"
            }
          }
        }
      ]
    }
  }
}
```

# 2-4 데이터 색인과 텍스트 분석

#### 역 인덱스 (Inverted index)

#### 텍스트 분석 과정 (Text Analysis)

- Tokenizer
- Token Filter
- _analyze API 를 이용한 분석 테스트

#### 사용자 정의 analyzer

#### 텍스트 필드에 analyzer 적용

#### _termvectors API를 이용한 term 확인

> https://esbook.kimjmin.net/06-text-analysis/6.1-indexing-data

Elasticsearch는 문자열 필드가 저장될 때 데이터에서 검색어 토큰을 저장하기 위해 여러 단계의 처리 과정을 거칩니다. 이 전체 과정을 **텍스트 분석(Text Analysis)** 이라고 하고 이 과정을 처리하는 기능을 **애널라이저(Analyzer)** 라고 합니다.

#### 1. 캐릭터 필터

텍스트 데이터가 입력되면 **가장 먼저** 필요에 따라 **전체 문장에서 특정 문자를 대치하거나 제거**하는데 이 과정을 담당

#### 2. 토크나이저

다음으로는 문장에 속한 단어들을 **텀 단위**로 하나씩 **분리 해 내는 처리 과정**을 거치는데 이 과정을 담당하는 기능입니다.
토크나이저는 **반드시 1개**만 적용이 가능합니다.
다음은 **`whitespace`** 토크나이저를 이용해서 공백을 기준으로 텀 들을 분리 한 결과입니다.

#### 3. 토큰 필터

분리된 **텀** 들을 **하나씩 가공**하는 과정을 거칩니다.

> 1. 여기서는 먼저 **`lowercase`** 토큰 필터를 이용해서 **대문자를 모두 소문자로** 바꿔줍니다.
>    이렇게 하면 대소문자 구별 없이 검색이 가능하게 됩니다. 대소문자가 일치하게 되어 같은 텀이 된 토큰들은 모두 하나로 병합이 됩니다.

https://github.com/slrslrr2/elasticsearch/blob/main/image-20220717174219888-8047345.png)

> 1. **`stop`** 옵션을 통하여 **불용어를 제거**시킵니다
>
> 텀 중에는 검색어로서의 가치가 없는 단어들이 있는데 이런 단어를 **불용어(stopword)** 라고 합니다. 보통 **a, an, are, at, be, but, by, do, for, i, no, the, to …** 등의 단어들은 불용어로 간주되어 검색어 토큰에서 제외됩니다. `stop`토큰 필터를 적용하면 우리가 만드는 역 인덱스에서 **the**가 제거됩니다.

`snowball`을 통해 형태소 분석 과정을 거쳐서 문법상 변형된 단어를 일반적으로 검색에 쓰이는 기본 형태로 변환해줍니다.

[

**`snowball`**을 통해 필요에 따라서는 **동의어**를 추가 해 주기도 합니다.

### 사용자 정의 analyzer

예제 1

```
PUT my_index2
{
  "mappings": {
    "properties": {
      "message": {
        "type": "text",
        "analyzer": "snowball" // mappings 정의
      }
    }
  }
}

// my_index에 id 에 값 생성
PUT my_index2/_doc/1
{
  "message": "The quick brown fox jumps over the lazy dog" 
}

GET my_index2/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "message": "jump"
          }
          
        }
      ]
    }
  }
}
```



------

# 2-5 노리(nori) 한글 형태소 분석기

#### Token Filter 추가 설명

#### Nori 설치

- Elasticsearch-plugin install analysis-nori
- _analyze API 를 이용한 한글 분석 테스트

#### Nori 설정

- user_dictionary_ruls를 이용한 사용자 사전 입력
- decompound_mode : 합성어 저장 설정
- Stoptags : Part Of Speech 품사 태그 지정
- nori_readingform : 한자어 번역

------

> https://esbook.kimjmin.net/06-text-analysis/6.7-stemming/6.7.2-nori

------

| 구분1                     | 구분2          | game/gameserver | subject | content |
| ------------------------- | -------------- | --------------- | ------- | ------- |
| **char_filter**           | html_strip     | -               | -       | O       |
|                           | mapping        | -               | -       | △       |
|                           | 정규식         | -               | -       | -       |
| **tokenizer**             | standard       |                 |         |         |
|                           | Letter         | -               | -       | -       |
|                           | Whitespace     | -               | -       | -       |
|                           | UAX URL Email  | -               | -       | -       |
|                           | Pattern        | -               | -       | -       |
|                           | path_hierarchy | -               | -       | -       |
| **filter** (token_filter) | Lowercase      | O               | O       | O       |
|                           | uppercase      | -               | -       | -       |
|                           | synonyms_path  | O               | -       | -       |
|                           | **ngram**      | -               | O?      | O?      |
|                           | **Edge NGram** | O               | -       | -       |
|                           | unique         | -               | O       | O       |
