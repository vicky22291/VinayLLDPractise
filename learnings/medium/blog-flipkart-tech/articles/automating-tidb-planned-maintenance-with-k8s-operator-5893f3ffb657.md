---
title: "Automating TiDB Planned Maintenance with k8s Operator"
author: "Raj Suvariya"
date: "Sep 29, 2024"
url: "https://blog.flipkart.tech/automating-tidb-planned-maintenance-with-k8s-operator-5893f3ffb657"
tags: ['Tidb', 'K8s', 'K8s Operator', 'Maintenance']
---

# Automating TiDB Planned Maintenance with k8s Operator

TiDB is an open-source relational distributed SQL-compatible database. It is compatible with MySQL clients and stores data in key-value stores underneath. As it is distributed in nature, it provides additional features such as horizontal scalability and high availability over the traditional MySQL.

We are a platform team at Flipkart hosting TiDB on k8s for business use cases cross-cutting multiple systems across the length and breadth of Flipkart; primarily serving OLTP workloads, with a few using OLAP workloads.


---

## A bit of TiDB Database

### TiDB Architecture

### TiDB Terminology

**Region**: A smaller data chunk, generally 96MB.

**PD (Placement Driver)**: Component that is the brain of the entire TiDB Database architecture. It manages region scheduling, key distribution, and issues transaction IDs.

**TiKV: **Key value store which holds all the data in a key-value form and uses RocksDB underneath.

**TiDB: **MySQL interface over TiKV, which processes SQL queries, aggregates results from multiple TiKVs, and handles DDL queries.

**TiFlash:** Key-value store similar to TiKV used for OLAP queries.

**TiCDC:** A component used for change data capture use cases and various supported downstream databases such as MySQL, Kafka, Pulsar, and TiDB.

**Monitor:** The component that monitors the entire TiDB cluster, and it hosts Prometheus and Grafana services.

### High Availability and Durability of TiDB

TiDB stores multiple replicas (by default 3) on different TiKV nodes, as illustrated, to ensure the High Availability of the database and durability of the data stored on the database. These replicas form a quorum based on Raft (majority consensus), so the database can continue to operate even when 2 out of 3 replicas are available. Therefore, one pod failure will not affect the overall database.

In case of a failure, the TiDB operator detects the failure and begins the recovery, wherein an additional pod is spawned and data can be copied to the new node from the latest replicas of all the regions hosted on the failed pod.

During this time, when data copy is in progress, one more pod failure can take down the entire cluster. We refer to this as an unplanned failure tolerance.

For more details about how high availability is ensured in TiDB, please refer to the official [TiDB documentation](https://docs.pingcap.com/tidb-in-kubernetes/stable/tidb-operator-overview).


---

## Infra at Flipkart

The backbone of Flipkart’s technological landscape lies in its robust in-house infrastructure which spans across two in-house data centers. This helps Flipkart ensure accurate control, security, and reliability.

Flipkart uses different hardware and persistence layers, catering optimally to different use cases. For example, latency-sensitive workloads use local SSD disks and highest generation vcores, whereas latency-insensitive workloads use NAS (Network-Attached Storage) and lower generation vcores. This helps balance the performance vs. cost tradeoff.


---

## Problem Statement

For critical databases, we use local SSD disks, but it comes with certain drawbacks. If there is any issue with the Compute of the node, it cannot be recreated on any other Compute server considering the data is locally attached and cannot be accessed from outside. In case of any detected issue that requires hardware to be taken out for maintenance, the k8s team cannot reschedule the pods without the involvement of the database team. So the k8s team schedules maintenance and informs the database teams to move out of such nodes.

The database team cannot ignore rescheduling such affected nodes because, as mentioned earlier, TiDB can only tolerate one node failure. If we ignore such scheduled maintenance, it can cause an unplanned failure (even though it was planned) and one actual unplanned failure can take down the entire database.

When we have information about scheduled maintenance, we have to move out of such nodes by creating additional replicas to ensure that each region has 3 replicas during the data copy (referred to as rebalancing).

It is a common concern that the TiDB operator does not handle such a workflow. Most of the public clouds run on NAS where the Compute can be easily rescheduled on different servers without rebalancing of data, unlike the specific use case in Flipkart, where we use local PVs (Persistent Volumes) to enhance disk performance.

In this context, our Kubernetes (k8s) operator plays a vital role by automating the movement of specified TiDB components, ensuring seamless operations, and minimising downtime. This operator addresses the complexities specific to our infrastructure, enhancing both efficiency and reliability.


---

## Why Automation?

Automation ensures that each step is executed precisely, significantly reducing the potential for mistakes and freeing up valuable on-call capacity. Our k8s operator meticulously handles each required step, ensuring a seamless transition without the risk of human error. By automating these maintenance tasks, we not only enhance operational efficiency but also improve the reliability of our infrastructure.

In this blog, we delve into the architecture and functionality of our k8s operator, demonstrating how it supports our maintenance processes and improves our operational resilience.


---

## Why k8s operator?

We evaluated multiple approaches, including scripting, using our backend service, k8s jobs, etc., before we chose the k8s operator based on the following evaluations:

**Native Kubernetes Integration: **As** **all actions are performed within the Kubernetes cluster, leveraging a k8s operator aligns seamlessly with our existing infrastructure and operational workflows.

**Control Loop Capabilities: **Kubernetes control loops enable the execution of small, synchronized tasks and support re-queuing reconciliation when asynchronous tasks, such as region migrations in TiKV, are pending verification.

**Fault-Tolerant Design: **The operator architecture ensures fault tolerance. If the operator pod responsible for maintenance fails, the next pod seamlessly takes over and resumes operations from the last checkpoint, ensuring continuity and reliability.

**High Availability (HA) Support: **The solution can be made highly available by deploying multiple operator pods and implementing leader election mechanisms, reducing single points of failure and enhancing operational robustness.

**Extensibility to Other DBaaS Products: **Beyond TiDB, the operator’s architecture can be extended to support other Database as a Service (DBaaS) products at Flipkart, offering a unified approach to managing diverse DBaaS services within our ecosystem.


---

We used an [operator-sdk](https://sdk.operatorframework.io/) framework to streamline development, allowing us to focus on defining reconciliation logic. This framework abstracts Kubernetes API complexities, accelerates development with built-in tools, and ensures robust integration with Kubernetes, enabling efficient management of our TiDB components.

### CRD

Our custom resource definition (CRD) focuses on simplicity, specifying only a scheduled start time for maintenance and the PVC name associated with the pod scheduled for deletion, sample CR as below.

```
apiVersion: maintenance.tidb.flipkart.com/v1alpha1
kind: TiDBScheduledMaintenance
metadata:
  name: maintenance-tikv-test-maintenance-hyd-tikv-0
  namespace: test-namespace
spec:
  pvcName: tikv-test-maintenance-hyd-tikv-0
  startTime: "2024-06-26T16:30:00+05:30"
```

We use the PVC name in the CRD instead of the pod’s name to focus on managing local persistent volume (PV) disks. Kubernetes permits pods to mount multiple disks, including a mix of local and network-attached volumes. Therefore, leveraging PVC names ensures accuracy in targeting disk-specific maintenance activities, enhancing scalability, and operational clarity within our infrastructure.

### Identifying TiDB Cluster and Component from PVC Name

Identifying the TiDB cluster and its components from just the PVC name could have been challenging, involving iteration over pods or string manipulation. Fortunately, the TiDB operator labels PVCs with this information, simplifying identification by making all necessary details readily available.

```
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  ...
  labels:
    app.kubernetes.io/component: tikv
    app.kubernetes.io/instance: tidb-cluster-1
    app.kubernetes.io/managed-by: tidb-operator
    app.kubernetes.io/name: tidb-cluster
    tidb.pingcap.com/cluster-id: "7385505036965769661"
    tidb.pingcap.com/pod-name: tidb-cluster-1-tikv-0
    tidb.pingcap.com/store-id: "1"
  name: tikv-tidb-cluster-1-tikv-0
  namespace: test-namespace
spec:
  ...
```

### Connection with PD

As it is clear from the manual steps, our operations frequently rely on data from PD — such as stores, member statuses, and triggers for store or member deletions — making it essential for the operator to establish a connection with PD.

To adhere to the DRY (Don’t Repeat Yourself) principle, we seamlessly integrated the [pdapi](https://github.com/pingcap/tidb-operator/blob/master/pkg/pdapi/pdapi.go) interface from the [tidb-operator](https://github.com/pingcap/tidb-operator/tree/master). This interface already supports all necessary calls to the PD REST API client, ensuring efficiency and consistency in our operations.

### Reconciliation

Once we identified TiDBCluster and Component with scheduled start time and a PD connection, all we had to do was to write a simple reconciliation that automates the steps described below.

### Steps to migrate data out of each TiDB component

We followed a few steps to move out the TiDB components that use the local PV storage.

> **_PD (Placement Driver)_**

_Check Leadership:_

```
./pd-ctl member leader resign
```

**_Scale-out PD replicas_**

**_Delete Member_**_:_

```
./pd-ctl member delete name <POD_NAME>
```

**_Delete Pod and PVC_**_:_

```
kubectl delete pvc/<PVC_NAME> pod/<POD_NAME> --context <k8s_context> -n <k8s_namespace>
```

**_Verify New Pod Creation_**_:_

```
./pd-ctl member
```

**_Scale-in PD replicas_**

> **_TiKV / TiFlash_**

**_Scale-Out TiKV:_**

**_Validate Scale-Out:_**

```
./pd-ctl store
```

**_Delete Store:_**

```
./pd-ctl store delete <STORE_ID>
```

**_Wait for Rebalancing:_**

```
./pd-ctl store <STORE_ID>
```

**_Delete Pod and PVC:_**

```
kubectl delete pvc/<PVC_NAME> pod/<POD_NAME> --context <k8s_context> -n <k8s_namespace>
```

**_Validate Store Registration:_**

```
./pd-ctl store
```

**_Initiate Scale-In:_**

> **_TiCDC_**

**_Delete Pod and PVC:_**


---

## Almost Done!

We’ve reached a point where our operator is functionally ready to perform maintenance on the TiDB cluster. However, we must also address non-functional requirements (NFRs) to ensure minimal performance impact on the cluster.

When multiple maintenance operations on TiKV or TiFlash run in parallel, data migration can consume significant CPU resources, potentially causing resource over-utilisation and affecting running queries. Therefore, it’s crucial to run these maintenance tasks sequentially to avoid compounding the CPU impact.

We implemented a locking mechanism using TiDBCluster CR annotations to manage sequential execution. When a maintenance operation locks the resource, it runs first, while others wait and periodically check for the lock every 5 minutes. Once the running maintenance completes and releases its lock, the next maintenance can acquire the lock and proceed.

```
apiVersion: pingcap.com/v1alpha1
kind: TidbCluster
metadata:
  annotations:
    maintenance.flipkart.com/maintenance-status: InProgress
    maintenance.flipkart.com/active-maintenance: maintenance-tikv-test-maintenance-hyd-tikv-0
    ...
```

Kubernetes resource updates ensure pessimistic locking. In this mechanism, if two threads attempt to read and update the same resource concurrently, the Kubernetes API server handles the conflict. When one thread updates the resource, it increments the resource version. The next thread, upon attempting an update, will detect that the resource version has changed and will be marked as a conflict. This ensures that only one maintenance operation acquires the lock in case of race conditions, thereby preventing the simultaneous execution of conflicting tasks.

```
func (r *TidbReconciler) lockTidbClusterForMaintenance(
   ctx context.Context, tc *pingcapv1alpha1.TidbCluster, maintenanceName string,
) error {
   tcCopy := tc.DeepCopy()
   if tcCopy.Annotations == nil {
      tcCopy.Annotations = make(map[string]string)
   }
   tcCopy.Annotations[utils.AnnotationMaintenanceStatus] = v1alpha1.StatusInProgress
   tcCopy.Annotations[utils.AnnotationActiveMaintenance] = maintenanceName
   err := r.Client.Update(ctx, tcCopy)
   return err
}
```

```
// Try to lock TiDB cluster
err := appReconciler.lockTidbClusterForMaintenance(ctx, tc, maintenanceName)
if err != nil {
   if apierrors.IsConflict(err) {
      logger.Error(
         err, fmt.Sprintf(
         "Failed to lock maintenance for %s due to conflict, requeue the request", utils.AppTiDBCluster,
         ),
      )
      // lock failed due to conflict, retry the lock
      return ctrl.Result{RequeueAfter: time.Minute * 5}, nil
   }
   // lock failed due to other error, mark the CR failed
   logger.Error(
      err, fmt.Sprintf("Failed to lock maintenance for %s", utils.AppTiDBCluster),
   )
   r.StatusUpdater.MarkCRStatusFailed(ctx, scheduledMaintenance, logger)
   return ctrl.Result{}, err
}

// lock succeeded, mark current CR InProgress
statusErr := r.StatusUpdater.MarkCRInProgress(ctx, scheduledMaintenance, logger)
```

Given the low frequency of multiple maintenance on the same cluster, this implementation approach balances simplicity with efficiency.


---

## And we are done!

And that’s how we implemented our Kubernetes operator to handle scheduled movements ahead of maintenance at Flipkart. As we continue to refine and expand its capabilities, we look forward to further optimising our infrastructure management and enhancing operational resilience at Flipkart.


---

## Results

Ever since it has been deployed in production, the maintenance operator has yielded impressive results. The operator has successfully handled hundreds of maintenance tasks, significantly reducing our on-call workload and ensuring consistency throughout each process step. By leveraging this operator, we have not just saved a lot of operational hours, but also brought significant reliability to the system by removing the human error from the process.


---

That’s our journey in developing the Kubernetes operator for managing scheduled movements ahead of maintenance at Flipkart. As we continue to innovate and refine our infrastructure management practices, we remain committed to leveraging technology for delivering exceptional performance and service reliability. We look forward to sharing more insights and advancements in the future.

**_References_**  
[_https://docs.pingcap.com/tidb/stable/pd-control_](https://docs.pingcap.com/tidb/stable/pd-control)_  
_[_https://docs.pingcap.com/tidb-in-kubernetes/stable/maintain-a-kubernetes-node#if-the-node-storage-cannot-be-automatically-migrated_](https://docs.pingcap.com/tidb-in-kubernetes/stable/maintain-a-kubernetes-node#if-the-node-storage-cannot-be-automatically-migrated)

---
**Tags:** Tidb · K8s · K8s Operator · Maintenance
