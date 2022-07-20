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

###인덱스를 조회하는것
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
