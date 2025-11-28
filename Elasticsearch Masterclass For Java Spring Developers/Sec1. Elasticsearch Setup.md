[ Elasticsearch Setup ]

---
`Elasticsearch Setup Using Docker Compose`

엘라스틱서치는 검색 엔진이자 NoSQL 데이터베이스이다. 기본적으로 9200번 포트를 사용한다.
사용자는 포트를 사용행 CRUD 작업이나 검색 요청 등을 진행할 수 있다. 모든 기능은 REST API로 제공된다.
포스트맨이나 CURL 등을 사용하여 상호작용할 수 잇지만 이번 강의에서는 Kibana의 Dev Tools 콘솔을 사용한다.
단, Kibana를 사용하기는 하지만 이번 강의가 Kibana 사용법을 다루는 것은 아니다.
Kibana는 데이터 시각화 등 다양한 기능이 있지만 이번 강의에서는 다루지 않는다.

엘라스틱 서치는 9200번 포트로 REST API를 제공하고, 9300번 포트로 여러 노드 간 통신을 한다. 이 기능은 고가용성과 확장성을 위한 것이다.
이 부분은 강의 후반부에 다룰 예정이다.

Kibana는 5601번 포트를 사용하고 웹 UI를 제공한다. Elasticsearch와 Kibana를 Docker 이미지를 통해 사용할 것이다.

Elasticsearch는 여러 노드가 모여 클러스터를 구성할 수 있으며 수평 확장도 가능하다.
지금은 클러스터 없이 단일 노드만 사용하도록 설정하였으며, 보안과 클러스터 관련 내용도 추후에 다룬다.

Kibana 의 경우는 Elasticsearch와 통신해야 하므로 환경 변수를 통해 관련 정보를 제공해주어야 한다.
Docker Compose 파일에 두 개의 도커 이미지를 사용하므로 Kibana에 주어지는 환경 변수도 IP 주소가 아닌 도커 서비스 이름을 사용해야 한다.
이는 Docker Network 내에서 도커 서비스 이름이 호스트 이름으로 작동하기 때문이다.

Elasticsearch 와 Kibana 이미지가 모두 구동되었다면 localhost:9200 으로 엘라스틱서치에 접속할 수 있고,
localhost:5601 로 Kibana 웹 UI에 접속할 수 있다.

--- 
``Kibana Dev Tools``

이번 강의에서는 Kibana Dev Tools 콘솔을 살펴본다.
Kibana Dev Tools 콘솔은 엘라스틱서치와 상호작용할 수 있는 편리한 도구이다.
Kibana 를 구동할때 Elasticsearch 정보를 전달했기에 Rest 요청에서 호스트와 포트를 지정할 필요가 없다.
Elasticsearch가 단일 노드이기에 클러스터라고 보기 어렵지만 Dev Tools 콘솔에서는 클러스터 단위로 작업을 진행한다.

가장 먼저 테스트할 것은 '_cluster/health' API 이다.

```http request
GET _cluster/health
```

```json
{
  "cluster_name": "docker-cluster",
  "status": "yellow",
  "timed_out": false,
  "number_of_nodes": 1,
  "number_of_data_nodes": 1,
  "active_primary_shards": 27,
  "active_shards": 27,
  "relocating_shards": 0,
  "initializing_shards": 0,
  "unassigned_shards": 2,
  "delayed_unassigned_shards": 0,
  "number_of_pending_tasks": 0,
  "number_of_in_flight_fetch": 0,
  "task_max_waiting_in_queue_millis": 0,
  "active_shards_percent_as_number": 93.10344827586206
}
```

이 API는 클러스터의 상태를 확인하는 API이다. 이 요청을 통해 현재 Kibana가 연결된 Elasticsearch 클러스터의 상태를 확인할 수 있다.
응답에서 "status" 필드는 클러스터의 상태를 나타낸다.
"green"은 모든 것이 정상임을 의미하고, "yellow"는 일부 복제본이 할당되지 않았음을 의미하며, "red"는 심각한 문제가 있음을 나타낸다.

다음은 '_nodes' API 이다.
```http request
GET _nodes
```
```json
{
  "_nodes": {
    "total": 1,
    "successful": 1,
    "failed": 0
  },
  "cluster_name": "docker-cluster",
  "nodes": {
    "Db0eOmO2Tb6dphPhYW_g_w": {
      "name": "059ddab81793",
      "transport_address": "172.28.0.2:9300"
      ...
    }
  }
}
```

이 API는 클러스터 내의 모든 노드에 대한 정보를 반환한다. 노드의 IP, 역할, JVM 인자 등을 확인할 수 있다.

다음은 '_cat' API 이다.
```http request
GET _cat/health?v=true&pretty
```
이 API는 클러스터의 상태를 간단한 표 형식으로 반환한다. 'v=true'는 헤더를 포함하도록 지정하고, 'pretty'는 응답을 읽기 쉽게 포맷팅한다.
