---
title: "Case Study: Handling Multi DC/Region with Apache HBase"
author: "Mallikarjun"
date: "May 8, 2024"
url: "https://blog.flipkart.tech/case-study-handling-multi-dc-region-with-apache-hbase-3dbf187a842e"
tags: ['Multi Dc Region', 'Apache Hbase', 'Apache Hbase Topology', 'Business Continuity', 'Disaster Recovery']
---

# Case Study: Handling Multi DC/Region with Apache HBase

## Introduction

[Apache HBase](https://hbase.apache.org/book.html) is an open-source non-relational distributed database modeled after Google’s Bigtable and written in Java. It is developed as part of Apache Software Foundation’s Apache Hadoop project and runs on top of HDFS, providing Bigtable-like capabilities for Hadoop.

We are a platform team at Flipkart hosting on Apache HBase for over 180 business use cases primarily OLTP workloads, with over 1 PetaByte data, 10000+ vcores and over 50 TB of RAM provisioned capacity.

Business Continuity and Disaster Recovery is a necessity for every organization. One aspect of it is to make their backend systems available in more than one Region / Data Center. At Flipkart, we have our infrastructure available across multiple Regions and here is a case study on how we architected our multi region constructs with a case study of Apache HBase as a data store.

For handling multi region constructs, we made certain decisions which laid the foundation for our architecture

We run truly multi-tenant hbase clusters. More details can be found in II part series of blogs — [Part I](./hbase-multi-tenancy-part-i-37cad340c0fa.md) and [Part II.](./hbase-multi-tenancy-part-ii-79488c19b03d.md)

In this blog, we broadly cover different aspects of handling multi-region Apache HBase topology setup to cover all functional needs of an application which includes:

### Hbase Terminology


---

## Server Side:

Here is a sample demonstration of multi-region Apache HBase cluster topology where multiple clusters are replicating data asynchronously.

### Deployment Strategy:

### ACyclic Data Replication:

### Conflict Resolution:

Any application which writes to a datastore across multiple masters (in this case across multiple regions) needs the underlying datastore to handle conflict resolutions in case a conflict arises. Apache HBase handles conflicts with some of the following underlying constructs:

In Below diagram


---

## Client Side:

With the server side presence of Apache HBase clusters in multiple regions, applications should leverage these multiple clusters to deploy their data sets in varied topologies. It can be as simple as Active-Passive setup to the complex topology of multiple clusters with multiple datasets. Following are some goals we had put to solve from an application point of view. Also, the client library is open source and available [here](https://github.com/flipkart-incubator/hbase-client).

### Multi-Region Aware HBase Configuration:

```
{
  "multiZoneStoreConfig": {
    "defaultConfig": {
      "hbaseConfig": {
        "hbase.client.pause": "50",
        "hbase.client.pause.cqtbe": "1000",
        "hbase.client.retries.number": "10",
        "hbase.client.max.total.tasks": "500",
        "hbase.client.max.perserver.tasks": "50",
        "hbase.client.max.perregion.tasks": "10",
        "hbase.client.ipc.pool.type": "RoundRobinPool",
        "hbase.client.ipc.pool.size": "10",
        "hbase.client.operation.timeout": "2000",
        "hbase.client.meta.operation.timeout": "5000",
        "hbase.rpc.timeout": "200",
        "hbase.rpc.read.timeout": "200",
        "hbase.rpc.write.timeout": "200",
        "hbase.rpc.shortoperation.timeout": "100"
      }
    },
    "regions": {
      "REGION_A": {
        "sites": {
          "SITE_A": {
            "poolSize": 40,
            "indexPurgeQueueSize": 30,
            "storeName": "zoneAA",
            "hbaseConfig": {
              "hbase.zookeeper.quorum": "zkAA-1,zkAA-2,zkAA-3"
            }
          }
        }
      },
      "REGION_B": {
        "sites": {
          "SITE_A": {
            "poolSize": 40,
            "indexPurgeQueueSize": 30,
            "storeName": "zoneBA",
            "hbaseConfig": {
              "hbase.zookeeper.quorum": "zkBA-1,zkBA-2,zkBA-3"
            }
          }
        }
      }
    }
```

### Routing Traffic:

When you have multiple clusters with a data sharding logic on the client side, it is important for the client library to provide flexibility with right feedback on consistency guarantees. With the aim of that, here is how we have provided routing capabilities to our clients.

**Site**: An independent deployment of an HBase cluster within a single Region. A site can connect to other HBase Site asynchronously via inter-cluster replication.

**Region**: A region is a specific geographical location where you can host your sites (HBase Clusters)

**Replica Sets**: A group of Sites (across or within a region) which has the same dataset synchronized via inter-cluster replication. A Replica Set can have different topologies such as [MasterSlave Replica Set](https://github.com/flipkart-incubator/hbase-client/blob/main/pipelined-client/src/main/java/com/flipkart/yak/client/pipelined/models/MasterSlaveReplicaSet.java) (Single Primary cluster for a particular primary key and one or more secondary clusters) and [HotCold Replica Set](https://github.com/flipkart-incubator/hbase-client/blob/main/pipelined-client/src/main/java/com/flipkart/yak/client/pipelined/models/HotColdReplicaSet.java) (Hot cluster gives better latencies and receives all writes)

**Consistency Levels**: Client library provides various consistency guarantees depending on the application’s functional requirements.

[WriteConsistency](https://github.com/flipkart-incubator/hbase-client/blob/main/pipelined-client/src/main/java/com/flipkart/yak/client/pipelined/models/WriteConsistency.java) levels are:

[ReadConsistency](https://github.com/flipkart-incubator/hbase-client/blob/main/pipelined-client/src/main/java/com/flipkart/yak/client/pipelined/models/ReadConsistency.java) levels are:

**Hot Router**:

Example: In the below example implementation of HotRouter, CustomRouter returns a different Replica Set based on the prefix of routeKey. This method is invoked for every api call that is made to the database, and hence it can dynamically alter the routing based on the changing dynamics of the topology.

```
 class CustomRouter implements HotRouter<MasterSlaveReplicaSet, String> {
    @Override public MasterSlaveReplicaSet getReplicaSet(Optional<String> routeKey)
        throws NoSiteAvailableToHandleException {
      SiteId Region1SiteA = new SiteId(SITEA, Region.REGION_1);
      SiteId Region2SiteA = new SiteId(SITEA, Region.REGION_2);
      String REGION1_PREFIX = "R1";
      String REGION2_PREFIX = "R2";
      String rkey = null;
      
      if (routeKey.isPresent()) {
        rkey = routeKey.get();
        if (rkey.startsWith(REGION1_PREFIX)) {
          return new MasterSlaveReplicaSet(Region1SiteA, Arrays.asList(Region2SiteA));
        } else if (rkey.startsWith(REGION2_PREFIX)) {
          return new MasterSlaveReplicaSet(Region2SiteA, Arrays.asList(Region1SiteA));
        }
      }
      
      throw new NoSiteAvailableToHandleException("No Sites found for the routekey " + rkey);
    }
  };
```


---

## Change Data Capture:

Change data capture(CDC) refers to capturing the changes recorded in a data store typically in a distributed queue such as Apache Kafka, for a destination system to consume and perform what are called side effects. With data store spread across multiple regions, the requirements for CDC can be things like capturing the changes in a single region, few regions, all regions, etc.

We have designed a CDC mechanism which implements [ReplicationEndpoint](https://github.com/apache/hbase/blob/rel/2.5.3/hbase-server/src/main/java/org/apache/hadoop/hbase/replication/BaseReplicationEndpoint.java) and registered as a [ReplicationEndPoint](https://github.com/apache/hbase/blob/rel/2.5.3/hbase-server/src/main/java/org/apache/hadoop/hbase/replication/ReplicationEndpoint.java#L47) [Peer](https://hbase.apache.org/book.html#hbase.replication.management). Library is open source and available [here.](https://github.com/flipkart-incubator/hbase-cdc)

CDC in multi-region context should propagate the mutations as per functional needs:

In the diagram below, on the left side — Change data is replicated to distributed queue in the same region as the hbase cluster, on the right side — Change data of all regions are replicated in a distributed queue in one region together.

Here are the implementation details of the CDC with multi-region support.


---

## Example Topology:

Lets consider an over simplified payment application in e-commerce context(_this is just for demonstration_) with following details

With the above requirements, I have designed a topology as shown below:

Call flow can be as follows:

Following is the client library configuration:

```
{
  "multiZoneStoreConfig": {
    "defaultConfig": {
      "hbaseConfig": {
        "hbase.client.pause": "50",
        "hbase.client.retries.number": "3",
        "hbase.client.operation.timeout": "2000",
        "hbase.rpc.timeout": "200",
        "hbase.rpc.read.timeout": "200",
        "hbase.rpc.write.timeout": "200"
      }
    },
    "regions": {
      "REGION_R1": {
        "sites": {
          "SITE_A": {
            "poolSize": 40,
            "storeName": "zoneAA",
            "hbaseConfig": {
              "hbase.zookeeper.quorum": "zkAA-1,zkAA-2,zkAA-3"
            }
          },
          "SITE_B": {
            "poolSize": 40,
            "storeName": "zoneAB",
            "hbaseConfig": {
              "hbase.zookeeper.quorum": "zkAB-1,zkAB-2,zkAB-3"
            }
          }
        }
      },
      "REGION_R2": {
        "sites": {
          "SITE_A": {
            "poolSize": 40,
            "storeName": "zoneBA",
            "hbaseConfig": {
              "hbase.zookeeper.quorum": "zkBA-1,zkBA-2,zkBA-3"
            }
          },
          "SITE_B": {
            "poolSize": 40,
            "storeName": "zoneBB",
            "hbaseConfig": {
              "hbase.zookeeper.quorum": "zkBB-1,zkBB-2,zkBB-3"
            }
          }
        }
      }
    }
  }
}
```

Following is router implementation.

```
class CustomRouter implements HotRouter<MasterSlaveReplicaSet, String> {
    enum STATE {
        INIT,PENDING,SUCCESSFUL,FAILED
    }
    String MYREGION = "R1";
    String REGIONR1 = "R1";
    String REGIONR2 = "R2";
    SiteId RegionR1SiteA = new SiteId(SITEA, Region.REGION_R1);
    SiteId RegionR1SiteB = new SiteId(SITEB, Region.REGION_R1);
    SiteId RegionR2SiteA = new SiteId(SITEA, Region.REGION_R2);
    SiteId RegionR2SiteB = new SiteId(SITEB, Region.REGION_R2);

    @Override public MasterSlaveReplicaSet getReplicaSet(Optional<String> routeKey)
            throws NoSiteAvailableToHandleException {
        String rkey = null; // Routekey is STATE

        if (routeKey.isPresent()) {
            rkey = routeKey.get();
            if (rkey.equals(STATE.INIT.name()) && MYREGION.equals(REGIONR1)) {
                return new MasterSlaveReplicaSet(RegionR1SiteA, Arrays.asList());
            } else if (rkey.equals(STATE.INIT.name()) && MYREGION.equals(REGIONR2)) {
                return new MasterSlaveReplicaSet(RegionR1SiteB, Arrays.asList());
            } else if (rkey.equals(STATE.FAILED.name()) || rkey.equals((STATE.PENDING.name())) || rkey.equals(STATE.SUCCESSFUL.name())) {
                return new MasterSlaveReplicaSet(RegionR2SiteB, Arrays.asList(RegionR2SiteA));
            }
        }

        throw new NoSiteAvailableToHandleException("No Sites found for the routekey " + rkey);
    }
};
```

In the above Routing logic, we are routing the traffic as per the state of the transaction. Say routing of INIT state transaction to the region local cluster, where each region has its own cluster independent of other regions. Whereas other states, such as PENDING, SUCCESS, FAILURE transactions route the traffic to a single active cluster in one of the region, to maintain a high level of consistency across regions.

## Conclusion

As business grows, there is a need for their technology solutions to become resilient to failures and business continuity becomes crucial. Largely, these requirements result in technology being available in multi-regions spread across geographical locations.

---
**Tags:** Multi Dc Region · Apache Hbase · Apache Hbase Topology · Business Continuity · Disaster Recovery
