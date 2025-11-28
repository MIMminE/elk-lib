[ Clustering / Sharding / Replication ]

`Elasticsearch Cluster`

지금까지는 단일 노드로 구성된 Elasticsearch를 사용하였다. 하지만 실제 운영 환경에서는 가용성(availability)과 확장성(scalability)을 위해 여러 노드가 모여 클러스터(cluster)를
구성한다.
이번 섹션에서는 Docker 컨테이너를 활용해 여러 개의 Elasticsearch 컨테이너를 Docker compose로 구동하여 클러스터를 구성하는 방법을 알아본다.
실제 운용 환경에서는 직접 구축하기보다는 매니지드 서비스를 사용하는 것을 추천한다. 데이터 백업, 보안 설정, 모니터링 등 다양한 기능을 제공하기 때문이다.

각 Elasticsearch 노드들은 네트워크로 연결하면 모든 노드가 자신들이 클러스터의 일부임을 인식하고 9300번 포트를 통해 서로 통신한다.

---
`Sharding`

여러 대의 머신으로 구성된 클러스터가 있고, products 라는 인덱스가 있다고 가정한다. 만약 products 인덱스에 저장되는 문서의 양이 많아져 한 노드에서 처리하기 어렵다고 판단되면,
인덱스를 여러 개의 샤드(shard)로 나누어 저장할 수 있다. 즉 큰 인덱스를 여러 개의 작은 조각으로 나누는 것이다.
각 샤드는 클러스터 내의 서로 다른 노드에 분산되어 저장될 수 있다. 이를 통해 데이터 저장 용량을 늘리고, 검색 성능도 향상시킬 수 있다. (이러한 확장을 수평 확장이라고 부른다.)
샤드는 기본적으로 프라이머리 샤드(primary shard)와 리플리카 샤드(replica shard)로 구성된다.

프라이머리 샤드는 실제 데이터를 저장하는 기본 샤드이다. 리플리카 샤드는 프라이머리 샤드의 복제본으로, 데이터의 가용성과 내구성을 높이기 위해 사용된다.
만약 프라이머리 샤드가 저장된 노드가 다운되더라도 리플리카 샤드가 다른 노드에 존재한다면 데이터 손실 없이 계속 서비스를 제공할 수 있다.
샤드의 개수는 인덱스를 생성할 때 지정할 수 있으며, 기본값은 1이다. 리플리카 샤드의 개수도 지정할 수 있으며, 기본값은 1이다.
즉 기본 설정에서는 각 인덱스가 1개의 프라이머리 샤드와 1개의 리플리카 샤드를 가지게 된다.    
샤딩은 대규모 데이터셋을 효율적으로 관리하고 검색 성능을 향상시키는 데 중요한 역할을 한다.

--- 
`Routing`

샤딩된 인덱스에 문서를 추가하거나 검색할 때, Elasticsearch는 내부적으로 라우팅(routing) 메커니즘을 사용하여 해당 문서가 어느 샤드에 저장될지 결정한다.
기본적으로 ID 값을 해싱하여 샤드를 결정한다. 예를 들어, 5개의 샤드가 있는 인덱스에 문서를 추가할 때, Elasticsearch는 문서의 ID를 해싱하여 0부터 4까지의 숫자 중 하나로 매핑하고,
해당 샤드에 문서를 저장한다.
이러한 라우팅 메커니즘은 데이터가 고르게 분산되도록 하여 샤드 간의 부하 균형을 유지하는 데 도움을 준다.
인덱스에 대한 전체 검색을 수행할 떄, 또는 ID값 없이 특정 필드로 검색할 때, Elasticsearch는 모든 샤드에 쿼리를 전송하여 결과를 집계한다.

---
`Replica Shard`

이번에는 복제(replication)에 대해 설명한다. 4개의 프라이머리 샤드로 구성된 인덱스가 있다고 가정한다.
문서가 각 프라이머리 샤드에 분산되어 저장되었다. 만약 하나의 샤드가 다운되었다면 해당 샤드에 저장된 데이터는 접근할 수 없게 된다.
일부 서비스에서는 잠깐의 다운타임이 괜찮을 수도 있지만, 규모가 큰 서비스에서는 이는 절대 허용할 수 없다.

이를 해결하기 위해 리플리카 샤드를 사용한다. 리플리카 샤드는 프라이머리 샤드의 복제본으로, 다른 노드에 저장된다.
예를 들어, 4개의 프라이머리 샤드가 있고 각 샤드에 대해 1개의 리플리카 샤드를 생성한다고 가정한다.
이렇게 하면 총 8개의 샤드가 존재하게 된다. 만약 프라이머리 샤드 중 하나가 다운되더라도 해당 샤드의 리플리카 샤드가 다른 노드에 존재하므로 데이터 손실 없이 계속 서비스를 제공할 수 있다.
리플리카 샤드는 단순히 데이터의 복제본 역할을 할 뿐만 아니라, 읽기 작업의 부하를 분산시키는 데도 도움을 준다.
즉, 검색 쿼리가 들어올 때 프라이머리 샤드뿐만 아니라 리플리카 샤드에서도 데이터를 읽을 수 있으므로, 읽기 성능이 향상된다.

**동작 방식**

- 평소와 같이 Elasticsearch 클러스터에 문서 저장 요청이 들어온다.
- Elasticsearch는 문서의 ID를 해싱하여 해당 문서가 저장될 프라이머리 샤드를 결정한다.
- 문서가 프라이머리 샤드에 인덱싱 된다.
- 인덱싱이 끝난 데이터는 다른 노드의 레플리카 샤드에도 복제된다.

만약 프라이머리 샤드가 다운된다면, Elasticsearch는 자동으로 해당 프라이머리 샤드의 리플리카 샤드를 프라이머리 샤드로 승격(promote)시킨다.

중요한 점은 프라이머리 샤드와 그 리플리카 샤드는 동일한 노드에 저장될 수 없다는 것이다. 이는 데이터의 가용성을 높이기 위한 것이다.
만약 동일한 노드에 저장된다면, 해당 노드가 다운될 때 프라이머리 샤드와 리플리카 샤드 모두 접근할 수 없게 되어 데이터 손실이 발생할 수 있다.
인덱싱 작업은 프라이머리에서 시작되어야 하지만, 검색 작업은 프라이머리 샤드와 리플리카 샤드 모두에서 수행될 수 있다. 이를 통해 읽기 성능이 향상된다.

---
`Index Creation With Shard And Replica`

이전에 인덱스를 생성할 때 클러스터 상태가 "yellow"로 표시되었던 이유는 단일 노드 클러스터에서는 리플리카 샤드를 할당할 수 없기 때문이다.

인덱스의 샤드 상태를 확인하는 요청을 보내면 해당 인덱스의 샤드와 리플리카 샤드의 상태를 확인할 수 있다.

```http request
GET /_cat/shards/product?v
```

```text
index   shard prirep state      docs store dataset ip         node
product 0     p      STARTED       1 5.6kb   5.6kb 172.28.0.2 059ddab81793
product 0     r      UNASSIGNED                               
```

prirep 필드는 샤드가 프라이머리(p)인지 리플리카(r)인지를 나타내며 state 필드는 샤드의 현재 상태를 나타낸다.
리플리카 샤드가 "UNASSIGNED" 상태인 것을 볼 수 있다. 이는 단일 노드 클러스터에서는 리플리카 샤드를 할당할 수 없기 때문이다.
기능 자체는 정상적으로 동작하지만, 리플리카 샤드가 할당되지 않아 클러스터 상태가 "yellow"로 표시된다.

만약 인덱스를 생성할 때 settings에서 레플리카 샤드의 개수를 0으로 설정하면, 리플리카 샤드가 생성되지 않으므로 클러스터 상태가 "green"으로 표시된다.

```http request
PUT /product
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}
```

프라이머리 샤드과 그 리플리카 샤드는 동일한 노드에 저장될 수 없지만,
노드가 부족할 때 여러 개의 프라이머리 샤드가 동일한 노드에 저장될 수는 있다. 예를 들어, 2개의 노드로 구성된 클러스터에 4개의 프라이머리 샤드가 있는 인덱스를 생성하면,
각 노드에 2개의 프라이머리 샤드가 저장된다.

여러 프라이머리 샤드를 두는 것은 확장성과 성능 향상에 도움이 될 수 있지만, 노드의 리소스(메모리, CPU 등)를 더 많이 사용하게 된다.
따라서 샤드의 개수를 설정할 때는 클러스터의 리소스와 데이터의 특성을 고려하여 적절한 값을 선택하는 것이 중요하다.

---
`Node Roles`

이번에는 클러스터 내에서 각 노드가 수행하는 역할에 대해 살펴본다.
기본적으로 클러스터를 구성하면, 노드 중 하나가 마스터 노드(master node)로 자동으로 지정된다.
마스터 노드는 클러스터의 상태를 관리하고, 샤드 할당, 인덱스 생성 및 삭제 등의 작업을 담당한다.
만약, 마스터 노드가 장애로 내려가면 다른 노드가 투표를 통해 새로운 마스터 노드를 선출한다.

마스터 노드는 클러스터의 안정성과 성능에 중요한 역할을 하므로,
운영 환경에서는 전용 마스터 노드를 별도로 구성하는 것이 좋다. 이렇게 하면 마스터 노드가 다른 작업으로 인해 과부하되지 않도록 할 수 있다.

마스터 노드 외에도 데이터 노드(data node), 인제스트 노드(ingest node) 등 다양한 역할을 수행하는 노드가 있다.
데이터 노드는 실제 데이터를 저장하고 검색하는 역할을 담당하며, 인제스트 노드는 데이터 전처리 작업을 수행한다.
각 노드는 여러 역할을 동시에 수행할 수도 있다. 예를 들어, 하나의 노드가 마스터 노드이자 데이터 노드 역할을 동시에 할 수 있다.

이외에도 노드의 역할에는 여러가지가 있지만 여기서는 중요한 몇개만을 다룬다.

---
`Three Node Elasticsearch Cluster`

지금부터는 Docker compose를 사용하여 3개의 노드로 구성된 Elasticsearch 클러스터를 만들어본다.
`multi-node-docker-compose.yml` 파일을 보면 세개의 Elasticsearch 서비스와 하나의 Kibana 서비스가 정의되어 있다.
여기서도 보안에 관련된 항목들을 비활성화하며 이는 다음에 다룬다.

정상적으로 실행된 이후 Kibana에서 Dev Tools로 이동하여 클러스터 상태를 확인해보자.

```http request
GET _cluster/health
```

```json
{
  "cluster_name": "es-cluster",
  "status": "green",
  "timed_out": false,
  "number_of_nodes": 3,
  "number_of_data_nodes": 3,
  "active_primary_shards": 25,
  "active_shards": 51,
  "relocating_shards": 0,
  "initializing_shards": 0,
  "unassigned_shards": 0,
  "delayed_unassigned_shards": 0,
  "number_of_pending_tasks": 0,
  "number_of_in_flight_fetch": 0,
  "task_max_waiting_in_queue_millis": 0,
  "active_shards_percent_as_number": 100
}
```

클러스터 상태가 'green' 으로 표시되어 있고 노드가 3개인 것을 확인할 수 있다.

노드의 상태를 확인해보자.

```http request
GET _cat/nodes?v
```

```text
ip         heap.percent ram.percent cpu load_1m load_5m load_15m node.role   master name
172.30.0.4           48          54   1    0.15    0.18     0.29 cdfhilmrstw -      es-node3
172.30.0.2           50          54   1    0.15    0.18     0.29 cdfhilmrstw -      es-node2
172.30.0.5           74          54   1    0.15    0.18     0.29 cdfhilmrstw *      es-node1
```

각 노드의 역할과 상태를 확인할 수 있다. node.role 필드를 보면 각 노드가 여러 역할을 동시에 수행하고 있음을 알 수 있다.
마스터 노드는 es-node1 이며, 나머지 두 노드는 데이터 노드 역할을 수행하고 있다.

sample 인덱스를 생성하고 해당 인덱스의 샤드 상태를 확인해보자

```http request
PUT /sample
```

```http request
GET /_cat/shards/sample?v
```

```text
index  shard prirep state   docs store dataset ip         node
sample 0     r      STARTED    1 5.4kb   5.4kb 172.30.0.2 es-node2
sample 0     p      STARTED    1 5.4kb   5.4kb 172.30.0.4 es-node3
```

샤드가 서로 다른 노드에 분산되어 저장된 것을 확인할 수 있다.

여러 프라이머리 샤드로 생성하도록 인덱스 설정을 변경해보자.

```http request
PUT /sample2
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  }
}
``` 

```http request
GET _cat/shards/sample2/?v
```

```text
index   shard prirep state   docs store dataset ip         node
sample2 0     r      STARTED    0  227b    227b 172.30.0.4 es-node3
sample2 0     p      STARTED    0  227b    227b 172.30.0.5 es-node1
sample2 1     r      STARTED    0  227b    227b 172.30.0.2 es-node2
sample2 1     p      STARTED    0  227b    227b 172.30.0.4 es-node3
sample2 2     p      STARTED    0  227b    227b 172.30.0.2 es-node2
sample2 2     r      STARTED    0  227b    227b 172.30.0.5 es-node1
```

3개의 프라이머리 샤드와 각 샤드에 대한 리플리카 샤드가 서로 다른 노드에 분산되어 저장된 것을 확인할 수 있다.
3개의 프라이머리 샤드가 각각 하나씩 복제하므로 전체 샤드는 6개가 된다.

---

`Master Election`

마스터 노드가 다운되었을 때 클러스터가 어떻게 동작하는지 알아본다.
```http request
GET /_cat/nodes?v
```
```text
ip         heap.percent ram.percent cpu load_1m load_5m load_15m node.role   master name
172.30.0.5           26          53  39    1.37    0.55     0.31 cdfhilmrstw -      es-node1
172.30.0.2           18          53  43    1.37    0.55     0.31 cdfhilmrstw -      es-node2
172.30.0.4           56          53  39    1.37    0.55     0.31 cdfhilmrstw *      es-node3
```
es-node3 가 마스터 노드인 것을 확인할 수 있다. es-node3 컨테이너를 중지시키고 다시 노드 상태를 확인해보자.
```http request
GET /_cat/nodes?v
```
```text
ip         heap.percent ram.percent cpu load_1m load_5m load_15m node.role   master name
172.30.0.2           35          46   2    0.10    0.32     0.26 cdfhilmrstw *      es-node2
172.30.0.5           42          46   2    0.10    0.32     0.26 cdfhilmrstw -      es-node1
```
es-node2 가 새로운 마스터 노드로 선출된 것을 확인할 수 있다. 

--- 
`High Availability Demo`

이번에는 복제와 고가용성에 대해 알아본다. 실습에 앞서 필요한 인덱스와 문서를 생성한다.

```http request
# create index
PUT /products

# get the information about shards
###
GET /_cat/shards/products?v

# store some documents 
###
POST /products/_doc
{
    "name": "product 1"
}

###
POST /products/_doc
{
    "name": "product 2"
}

###
POST /products/_doc
{
    "name": "product 3"
}
```

이렇게 하면 products 인덱스가 생성되고 3개의 문서가 저장된다. 이제 샤드 상태를 확인해보자.

```http request
GET /_cat/shards/products?v
```
```text
index    shard prirep state   docs  store dataset ip         node
products 0     p      STARTED    3 13.9kb  13.9kb 172.30.0.2 es-node2
products 0     r      STARTED    3   14kb    14kb 172.30.0.4 es-node3
```

이 상태를 보면 두 개의 노드에 products 인덱스의 샤드가 분산되어 저장되어 있는 것을 확인할 수 있다.
반대로 es-node1 노드는 products 인덱스의 샤드를 가지고 있지 않는다. 이 상황에서 es-node1에게 검색 요청을 보내보자.

```commandline
curl localhost:9201/products/_search
```
샤드를 가지고 있지 않는 노드로 검색 요청을 보내도 정상적으로 결과가 반환되는 것을 확인할 수 있다.
즉, 데이터를 가지고 있지 않는 노드로도 검색 요청을 보낼 수 있다. 이는 클러스터 내의 모든 노드가 서로 통신할 수 있기 때문이다.

이제 es-node2 컨테이너를 중지시켜보자. es-node2는 products 인덱스의 프라이머리 샤드를 가지고 있다.
이러면 es-node2가 다운되었기 때문에 products 인덱스의 프라이머리 샤드에 접근할 수 없게 된다. 
하지만 리플리카 샤드가 es-node3에 존재하므로 데이터 손실 없이 서비스가 계속 동작한다. 

리플리카 샤드였던 es-node3이 자동으로 프라이머리 샤드로 승격한 후 기본 대기 시간(1분) 이후에는 현재 남아 있는 노드들에 리플리카 샤드를 다시 생성한다.
여기서 기본 대기 시간이 존재하는 이유는 다운된 노드가 곧바로 복구될 수도 있기 때문이다. 만약 바로 리플리카 샤드를 생성한다면, 다운된 노드가 복구되었을 때
동일한 샤드가 두 개 존재하게 되어 데이터 불일치 문제가 발생할 수 있다.
따라서 기본 대기 시간 동안 다운된 노드가 복구될 시간을 주는 것이다. 이 시간은 설정을 통해 변경할 수 있다.