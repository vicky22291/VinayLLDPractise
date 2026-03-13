---
title: "Deploying Active-Passive topology in TiDB"
subtitle: "Using EC and TS-Map features of TiCDC"
author: "Lalit Kant Roul"
url: "https://blog.flipkart.tech/deploying-active-passive-topology-in-tidb-cd5b8e9a8fc9"
tags: ['Database', 'Multi Region', 'Disaster Recovery', 'MySQL', 'Backend']
---

# Deploying Active-Passive topology in TiDB

> Using EC and TS-Map features of TiCDC

## Introduction

With our database clusters spanning across regions, the application teams at Flipkart needed a comprehensive database deployment strategy in tune with our use cases while also ensuring high availability, optimum performance, and quick disaster recovery.

We chose TiDB, as it is an open-source NewSQL database compatible with MySQL and provides strong consistency, horizontal scalability, high availability, and HTAP (Hybrid Transactional/Analytical Processing) features. It also offers various deployment topologies for multi-region deployment, of which the active-passive topology seemed to solve our requirements the best.

The active-passive topology is a known database pattern that is deployed to achieve this for 2-region deployments. In this setup, two database clusters are present in separate regions, where the active cluster receives all the writes and an asynchronous replication happens from the active cluster to the passive cluster.

## Active-Passive Topology in TiDB

The active-passive topology in TiDB is a setup of two TiDB clusters present in different regions (or, data centers), with "asynchronous replication" happening from the "active" to the "passive" regions using the TiCDC component.

**Description:**

## What are the replication guarantees?

## What are the limitations of replication in TiCDC?

In cross-table transactions, the latest data in the passive cluster may not be in a consistent state. This can happen if only a few tables of the last set of transactions have replicated to the passive cluster.

This limitation has two major implications for applications that opt for an active-passive topology:

Many applications expect these problems to be solved by the database and cannot move to this kind of deployment with these limitations.

## Overcoming the replication limitations in Active-Passive topology

We put forth these implications for Flipkart with the TiDB team. The v6.1.1 version of TiCDC came with the following features to address the existing limitations:

## TS-map replication feature - data consistency

TiCDC has a TS-map feature for data consistency that maps the transaction TSO boundary in the active cluster to a transaction TSO in the passive cluster. With the _tidb_snaphot_ session variable, which is used to read data at a point in time, we can use the TS-map to read data in the passive cluster at a transaction boundary that is consistent with the active cluster. Set the _sync-point_ parameter to true in the _Changefeed_ configuration to enable the TS-map.

## Eventually Consistent (EC) replication feature for disaster recovery

TiCDC supports backing up incremental data from an upstream TiDB cluster to S3 storage. This incremental data backup is called "redo logs” which contain the recent transactions of the active cluster. When the upstream cluster encounters a disaster and becomes unavailable, we can apply the redo logs and bring the data in the passive cluster to a recent consistent state. This is the eventually consistent (EC) replication feature in TiCDC.

During replication, TiCDC ensures that a transaction is first persisted in redo logs and then replicated to the passive cluster. If a transaction is partially replicated to the passive cluster at any point in time, causing it to be in an inconsistent state, the redo logs have the complete transaction data which are applied if a disaster happens at that point.

With this capability, we can switch applications to the passive cluster quickly, avoiding long downtime and improving service continuity. The last few transactions can get lost if the disaster happens before they are persisted in the redo logs, resulting in some data loss.

Set the _consistent.level_ parameter to _“eventual”_ in the _Changefeed_ configuration to enable the EC feature. The redo logs contain only the most recent transactions, and older transactions that are no longer required to be kept in redo logs are periodically cleaned up. The _Changefeed_ allows disabling or enabling EC dynamically with a simple pause, update, and resume set of steps that do not break replication.

The redo logs of TiCDC are stored in an S3 bucket in GCS that is highly available and managed in GCP.

## What do we do during the switchover for Disaster Recovery?

To do the switchover in a disaster scenario in the active region:

To apply the redo logs, we use a command that looks like this -

It gives the value of _checkpoint-ts_ in the output, which tells us the last applied transaction TSO. We calculate the amount of data loss based on that value.

2. We prepare the passive cluster for an active role, and the application can switch over to the newly active cluster after that.

As per our testing, the P95 of RPO is <= 10 seconds and the RTO is <= 5 minutes.

## Benchmarking of replication in Active-Passive Topology

Now it’s time to benchmark the TiCDC replication. Performance benchmarking of TiCDC replication is done with the objective to find out how much throughput we can get from the database with minimal replication lag that is contained in the seconds range.

## Test Setup

We have used two data centers to validate the Active-Passive topology. The network latencies between the two data centers are in the order of 10–15 ms. We have set up the active cluster in DC-1 and the passive cluster in DC-2.

The component sizing and configuration are done for a hot data store where the workload is mainly OLTP with high throughput and large datasets.

Components in Active cluster in DC-1 and their configuration:

The Passive cluster in DC-2 has the same configuration as the Active cluster.

## Test Plan

The test comprises running a workload of three tables and using an in-house benchmarking tool that generates an ‘Insert’ load and maintains the QPS at a constant value. This allows us to test the replication performance at any specific QPS. Each test is run for about 45 minutes with some cooling period between them.

A benchmarking script runs the test for varying workloads that generate an overload load ranging from 3k to 30K QPS on the active cluster while the replication is happening from active to passive.

We first run the tests with both EC and TS-map features disabled in the TiCDC replication which helps us to establish a baseline and then we run the test with EC and TS-map features enabled.

## Test Results

**EC and TS-map features disabled:**

With the QPS of workload increasing per run, we saw that the replication lag was contained to a few seconds till about 26k inserts in the disabled case. At 28k inserts, we saw the replication lag increase to about a minute but did not rise further as the test progressed.

The following graphs show us the replication lag at about 28k QPS -

**EC and TS-map features enabled:**

With the QPS of workload increasing per run, we saw that the replication lag was contained to a few seconds till about 25–26k inserts in the enabled case. At 27k inserts, we saw that the replication lag continuously increases slowly as the test progressed.

The following graphs show us the replication lag at about 25k-26k QPS enabled -

We see similar performances in both cases. The impact of EC and TS-map features on TiCDC replication is negligible. We can say that the TiCDC is able to handle a maximum workload of 25k QPS in both cases keeping the replication lag well contained to a few seconds.

On analyzing further and discussing with the TiDB team to understand where the bottleneck at high QPS is, we concluded that the bottleneck is caused in the Sink by the increased latencies of Replace queries in the passive cluster.

This was evident from the p999 value of “MySQL sink write duration” metrics of TiCDC of about one second. TiCDC uses Replace statements to replicate Insert queries. Replace latencies are higher than Insert latencies in TiDB. Thus, replication throughput cannot match the throughput of the active cluster.

## Testing Scalability

Scalability is defined as the ability to handle an increased workload. We measure and validate scalability on two aspects -

We ran the scalability tests using scripts and observed the replication performance from the TiCDC dashboard. We ran these tests for different workloads at different QPS. It showed us that TiCDC replication can scale out and handle increased workloads.

## Testing High Availability

As TiDB has a modular architecture, every component should not only be highly scalable but also highly available. High availability tests validate that when an instance of a component is not available (either temporarily or goes down permanently), the TiCDC replication progresses normally.

In our tests, an instance or a component is made unavailable either by stopping or destroying the instance or by using network iptables rules to introduce unreachability.

We ran the availability tests using scripts that simulate a failure scenario and observe the replication _checkpoint-ts_ to check the replication lag. It showed us that TiCDC replication progresses without failures when some instances become unavailable either temporarily or permanently.

While running these tests we encountered a few bugs and identified issues in TiCDC in the newly introduced EC and TS-map features. We reported these issues to the TiDB team and they helped us with the fixes and validations in our environment.

Through continuous improvements made by the TiDB team, after few iterations, we were able to achieve the Active-Passive topology that we desired.

## Performance Tuning of TiCDC

TiCDC replication has multiple configuration parameters. We have arrived at the values to use after a few iterations of experimenting and following recommendations from the TiDB team.

The following are the different parameters in TiCDC configuration that are related to performance, along with the values that worked best for our environment:

## Conclusion

TiCDC, with the EC and TS-map features, enables us to deploy the Active-Passive topology in TiDB for our applications that make use of multi-table transactions. For disaster recovery, we can bring the passive cluster to a consistent state by applying “redo logs." With the TS-map feature, we can do consistent reads in the passive cluster in a normal scenario.

The performance benchmarking tells us that with these features enabled, we can achieve a throughput of 25–26k QPS which is similar to the throughput with the features disabled, and replication lag is contained to a few seconds range. This proves us that the overhead is negligible and the features can be used in production.

---
**Tags:** Database · Multi Region · Disaster Recovery · MySQL · Backend
