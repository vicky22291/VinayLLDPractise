---
title: "Challenges and Learnings in building hbase k8s operator"
author: "Mallikarjun"
date: "Jun 22, 2023"
url: "https://blog.flipkart.tech/challenges-and-learnings-in-building-hbase-k8s-operator-d074c0a55fe1"
tags: ['Hbase K8s Operator', 'Hbase', 'Kubernetes Operator']
---

# Challenges and Learnings in building hbase k8s operator

HBase is an open-source, non-relational distributed database inspired by Google’s Big Table and written in Java. It is developed as part of the Apache Software Foundation’s Apache Hadoop project and runs on top of HDFS, providing Bigtable-like capabilities for Hadoop.

K8s Operators are software extensions to Kubernetes using custom resources to manage applications and their components. Operators follow Kubernetes principles, notably the control loop.

## Problem Statement:

Hbase is a complex piece of software with components such as hmaster, Region Server, zookeeper, namenode, DataNode, and journal node, making it operationally very heavy to keep the lights on.

In the Flipkart HBase team, we run several clusters of size ~500 Region Servers (DataNodes co-hosted). Maintenance of such a huge infrastructure of HBase was operationally heavy and so we built an HBase[ k8s operator](https://github.com/flipkart-incubator/hbase-k8s-operator) to automate running Apache HBase clusters.

In this article, we talk about challenges faced and learning gained while building a K8s Operator to run and manage an Hbase Cluster. Here is the [GitHub link](https://github.com/flipkart-incubator/hbase-k8s-operator).

## Hbase Terminology

**Hmaster:** Co-ordinator Node in HBase, which coordinates between region servers for performing certain cluster-wide activities such as region assignment, balancing, etc

**Region Server: **Node on which regions(shards) are hosted in memory that serves client RPC calls and whose data is stored in hdfs DataNodes.

**Zookeeper**: HBase uses ZooKeeper as a distributed coordination service for use cases such as region assignments, Region Server heartbeats, and cluster replication check pointing.

**NameNode**: is the node in the Apache Hadoop HDFS Architecture that maintains and manages the blocks present on the DataNodes

**Journal nodes**: Perform the synchronization activities between Active & Passive NameNode.

**DataNode:** DataNodes store data in a Hadoop cluster and is the name of the daemon that manages the data. File data is replicated on multiple DataNodes for fault tolerance and so that localized computation can be executed near the data.

**K8s Pod: **Pods are the smallest deployable units of computing that you can create and manage in Kubernetes. A Pod is a group of one or more containers, with shared storage and network resources, and a specification for how to run the containers. A Pod’s contents are always co-located and co-scheduled, and run in a shared context.

**K8s Operator:** Software extensions in Kubernetes that use custom resources to manage applications and their components. Operators follow Kubernetes principles, notably the[ control loop](https://kubernetes.io/docs/concepts/architecture/controller).

**Custom resource:** An extension of the Kubernetes API that is not necessarily available in a default Kubernetes installation. It represents the customization of a particular Kubernetes installation.

**K8s Service**: A Service is a method to expose a network application that is running as one or more Pods in your cluster so clients can interact with it.

**K8s Kind**: All resource types such as pods, namespaces, services have a concrete representation (their object schema) which is called a kind.

**Fault Domain**: A fault domain is a set of hardware components that share a single point of failure. To be fault tolerant to a certain level, you need multiple fault domains at that level.

**Unix Domain Socket**: A Data communications endpoint to exchange data between processes within a host operating system. It is also called an IPC socket (inter-process communication socket).

For the rest of the discussion, we are going to refer to the following setup:

## Understanding the Hbase k8s Operator Artifacts

## Working with HBase k8s Operator


---

## Challenges in building HBase-k8s-operator

As with all great findings, we too had to tide through a set of categorized challenges in building the HBase-k8s-operator.

## DNS IP Caching — Legacy systems

Stateful stacks like Zookeeper use DNS to provide a static identity of the ephemeral nature of VM/pod. With static identity, you don’t need to change configurations and clients should be able to auto-recover failure by re-resolving the DNS in case of vm/pod recreation. However, many versions have broken DNS caching mechanisms, where they may cache the IP address without re-resolving the DNS. Here are a few examples:

**Zookeeper Clients:**

Let’s say, zk-1,zk-2,zk-3:2181 is the quorum for a zookeeper cluster.

Re-creation of the zookeeper nodes does not require any configuration changes, as the identity of the quorum members does not change if you are using DNS. However, old zookeeper clients with version lesser than 3.4.13 wouldn’t refresh the ip, once DNS is resolved, as mentioned in this[ ticket](https://issues.apache.org/jira/browse/ZOOKEEPER-2184).

```
2020-07-20 14:11:52,103 INFO  [main-SendThread(10.33.51.217:2181)] zookeeper.ClientCnxn: Opening socket connection to server 10.33.51.217/10.33.51.217:2181. Will not attempt to authenticate using SASL (unknown er
ror)
2020-07-20 14:11:52,105 WARN  [main-SendThread(10.33.51.217:2181)] zookeeper.ClientCnxn: Session 0x4730fc8a3f90072 for server null, unexpected error, closing socket connection and attempting reconnect
java.net.ConnectException: Connection refused
       at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
       at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717)
       at org.apache.zookeeper.ClientCnxnSocketNIO.doTransport(ClientCnxnSocketNIO.java:361)
```

**HDFS Clients**

HDFS clients which connect to namenode for making rpc calls have a similar problem. They cache the ip address of the first dns resolution and never refresh. Below are some logs from HBase Region Servers (hdfs clients). This bug is mentioned in this[ ticket](https://issues.apache.org/jira/browse/HADOOP-18365).

```
2023-03-08T13:33:55,273 WARN  [LeaseRenewer:yak@hbase-store] ipc.Client: Address change detected. Old: nn-0/10.53.195.11:8020 New: nn-0/10.53.196.136:8020
2023-03-08T13:33:55,274 INFO  [LeaseRenewer:yak@hbase-store] retry.RetryInvocationHandler: org.apache.hadoop.net.ConnectTimeoutException: Call From dn-0/10.53.179.87 to nn-0:8020 failed on socket timeout exception: org.apache.hadoop.net.ConnectTimeoutException: 20000 millis timeout while waiting for channel to be ready for connect. ch : java.nio.channels.SocketChannel[connection-pending remote=nn-0/10.53.195.11:8020]; For more details see:  http://wiki.apache.org/hadoop/SocketTimeout, while invoking ClientNamenodeProtocolTranslatorPB.renewLease over nn-0/10.53.195.11:8020 after 1 failover attempts. Trying to failover after sleeping for 1336ms. Current retry count: 1.
2023-03-08T13:33:56,623 INFO  [LeaseRenewer:yak@hbase-store] retry.RetryInvocationHandler: tobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:604)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:572)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:556)
       at org.apache.hadoop.ipc.RPC$Server.call(RPC.java:1093)
       at org.apache.hadoop.ipc.Server$RpcCall.run(Server.java:1043)
       at org.apache.hadoop.ipc.Server$RpcCall.run(Server.java:971)
       at java.security.AccessController.doPrivileged(Native Method)
       at javax.security.auth.Subject.doAs(Subject.java:422)
       at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1878)
       at org.apache.hadoop.ipc.Server$Handler.run(Server.java:2976)
```

**NameNode Connecting Journalnodes:**

NameNodes which connect to the journal nodes, cache the IP address of journal nodes and never re-resolve journal nodes dns. When multiple journal nodes are recreated the majority quorum breaks resulting in a namenode crash. It becomes unavailable for any RPC calls until respawn.

```
23/05/04 08:24:56 INFO namenode.FSEditLog: Starting log segment at 38768
23/05/04 08:25:16 WARN ipc.Client: Address change detected. Old: jn-4/10.69.14.118:8485 New: jn-4/10.69.14.154:8485
23/05/04 08:25:16 INFO ipc.Client: Retrying connect to server: jn-4/10.69.14.154:8485. Already tried 0 time(s); maxRetries=45
```

```
23/05/04 08:14:43 ERROR namenode.FSEditLog: Error: flush failed for required journal (JournalAndStream(mgr=QJM to [10.66.47.15:8485, 10.67.174.115:8485, 10.69.30.229:8485, 10.69.62.172:8485, 10.69.14.118:8485], stream=QuorumOutputStream starting
at txid 38747))
java.io.IOException: Timed out waiting 20000ms for a quorum of nodes to respond.
       at org.apache.hadoop.hdfs.qjournal.client.AsyncLoggerSet.waitForWriteQuorum(AsyncLoggerSet.java:138)
       at org.apache.hadoop.hdfs.qjournal.client.QuorumOutputStream.flushAndSync(QuorumOutputStream.java:113)
       at org.apache.hadoop.hdfs.server.namenode.EditLogOutputStream.flush(EditLogOutputStream.java:126)
       at org.apache.hadoop.hdfs.server.namenode.EditLogOutputStream.flush(EditLogOutputStream.java:120)
       at org.apache.hadoop.hdfs.server.namenode.JournalSet$JournalSetOutputStream$8.apply(JournalSet.java:546)
       at org.apache.hadoop.hdfs.server.namenode.JournalSet.mapJournalsAndReportErrors(JournalSet.java:392)
       at org.apache.hadoop.hdfs.server.namenode.JournalSet.access$200(JournalSet.java:55)
       at org.apache.hadoop.hdfs.server.namenode.JournalSet$JournalSetOutputStream.flush(JournalSet.java:542)
       at org.apache.hadoop.hdfs.server.namenode.FSEditLog.logSync(FSEditLog.java:730)
       at org.apache.hadoop.hdfs.server.namenode.FSEditLogAsync.run(FSEditLogAsync.java:260)
       at java.base/java.lang.Thread.run(Thread.java:833)
23/05/04 08:14:43 WARN client.QuorumJournalManager: Aborting QuorumOutputStream starting at txid 38747
23/05/04 08:14:43 INFO util.ExitUtil: Exiting with status 1: Error: flush failed for required journal (JournalAndStream(mgr=QJM to [10.66.47.15:8485, 10.67.174.115:8485, 10.69.30.229:8485, 10.69.62.172:8485, 10.69.14.118:8485], stream=QuorumOutp
utStream starting at txid 38747))
23/05/04 08:14:43 INFO namenode.NameNode: SHUTDOWN_MSG:                                                                                                                                                                     
************************************************************/
```

### Approach

We approached each of these examples based on feasibility and achievability with k8s constructs. Here’s what we attempted:

**Summary**:

## Multiple primary processes

In HDFS, reads and writes go through the DataNode process. When the Region Server (client) asks the DataNode to read a file, the DataNode reads that file off of the disk and sends the data to the client over a TCP socket.

**Short-circuit reads in Hbase**

**Short-circuit reads in Container world**:

### Approach

We attempted to solve the above challenge with the following approaches:

Approach 1:

We started with the obvious approach of shared persistent volume between Region Server and DataNode container, where _dfs.domain.socket.path _is mounted on the shared volume with [_fsGroup_](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/). With the _fsGroup_ system group, volume is mounted with 0775 permissions so that directories and files can be created by the user who is part of the _fsGroup_ system group as shown below.

```
$ ls -lah /grid/1/hadoop/
total 8.0K
drwxrwsr-x 2 hbase hbase 4.0K Jun  3 19:57 .
drwxrwsr-x 5 root     hbase 4.0K Jun  3 19:52 ..
srw-rw-rw- 1 hbase hbase    0 Jun  3 19:57 dn.9866
```

This has a problem with Hadoop security described [here](https://cwiki.apache.org/confluence/display/HADOOP2/SocketPathSecurity). In this code, exposing socket with a group or world writable poses a security threat and it is disallowed as per this [code](https://github.com/Flipkart/hadoop/blob/trunk/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/net/unix/DomainSocket.c#L341) from the hadoop-commons project. It fails with the following error:

```
java.io.IOException: The path component: '/grid/1' in '/grid/1/hadoop/dn.9866' has permissions 0775 uid 0 and gid 1011. It is not protected because it is group-writable and not owned by root. This might help: 'chmod g-w /grid/1' or 'chown root /grid/1'. For more information: https://wiki.apache.org/hadoop/SocketPathSecurity
        at org.apache.hadoop.net.unix.DomainSocket.validateSocketPathSecurity0(Native Method)
        at org.apache.hadoop.net.unix.DomainSocket.bindAndListen(DomainSocket.java:196)
        at org.apache.hadoop.hdfs.net.DomainPeerServer.<init>(DomainPeerServer.java:40)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.getDomainPeerServer(DataNode.java:1524)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.initDataXceiver(DataNode.java:1491)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.startDataNode(DataNode.java:1731)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.<init>(DataNode.java:564)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.makeInstance(DataNode.java:3148)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.instantiateDataNode(DataNode.java:3054)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.createDataNode(DataNode.java:3098)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.secureMain(DataNode.java:3242)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.main(DataNode.java:3266)
```

We tried again in vain, with [empty directory mount volume](https://kubernetes.io/docs/concepts/storage/volumes/#emptydir), where the directory is given permissions of ‘world writable’ instead of ‘group writable’ as available in persistent volume. See also: Reference [here](https://github.com/kubernetes/kubernetes/blob/master/pkg/volume/emptydir/empty_dir.go#L46).

Approach 2:

With no solution in sight, we patched the Hadoop native library, allowing group writable. We altered this [code block](https://github.com/Flipkart/hadoop/blob/trunk/hadoop-common-project/hadoop-common/src/main/native/src/org/apache/hadoop/net/unix/DomainSocket.c#L341) to rebuild Hadoop native [shared object](https://tldp.org/HOWTO/Program-Library-HOWTO/shared-libraries.html) libraries, as mentioned [here](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/NativeLibraries.html). As we use linux [mount namespaces](https://man7.org/linux/man-pages/man7/mount_namespaces.7.html) for volume mounts, the filesystem is available only within the containers and it is not a security threat.

We also made the following changes:

**Summary**

## Pod DNS Bootstrap

A Pod requires a host name for the Pod’s DNS record to be created. A Pod with no host name but with sub-domain will only create the A or AAAA or DNS record for the headless Service (default-subdomain.my-namespace.svc.cluster-domain.example), pointing to the Pod’s IP address. Pod needs to become ready in order to have a DNS record. You can read more details in this[ document](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/).

**DataNode Registration with NameNode**

```
23/04/19 11:30:29 WARN datanode.DataNode: RemoteException in register
org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.hdfs.server.protocol.DisallowedDatanodeException): Datanode denied communication with namenode because hostname cannot be resolved (ip=10.52.117.55, hostname=10.52.117.55): DatanodeRegistration(0.0.0.0:9866, datanodeUuid=948619de-5bae-4515-bfaa-d29bca496e2e, infoPort=9864, infoSecurePort=0, ipcPort=9867, storageInfo=lv=-57;cid=CID-80d2b728-dc30-460d-82a5-3c1b9bb37e3d;nsid=1101016859;c=1677151858288)
       at org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager.registerDatanode(DatanodeManager.java:1147)
       at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.registerDatanode(BlockManager.java:2566)
       at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.registerDatanode(FSNamesystem.java:4235)
       at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.registerDatanode(NameNodeRpcServer.java:1578)
       at org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolServerSideTranslatorPB.registerDatanode(DatanodeProtocolServerSideTranslatorPB.java:101)
       at org.apache.hadoop.hdfs.protocol.proto.DatanodeProtocolProtos$DatanodeProtocolService$2.callBlockingMethod(DatanodeProtocolProtos.java:33760)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:604)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:572)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Server$ProtoBufRpcInvoker.call(ProtobufRpcEngine2.java:556)
       at org.apache.hadoop.ipc.RPC$Server.call(RPC.java:1093)
       at org.apache.hadoop.ipc.Server$RpcCall.run(Server.java:1043)
       at org.apache.hadoop.ipc.Server$RpcCall.run(Server.java:971)
       at java.base/java.security.AccessController.doPrivileged(AccessController.java:712)
       at java.base/javax.security.auth.Subject.doAs(Subject.java:439)
       at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1878)
       at org.apache.hadoop.ipc.Server$Handler.run(Server.java:2976)

       at org.apache.hadoop.ipc.Client.getRpcResponse(Client.java:1612)
       at org.apache.hadoop.ipc.Client.call(Client.java:1558)
       at org.apache.hadoop.ipc.Client.call(Client.java:1455)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:242)
       at org.apache.hadoop.ipc.ProtobufRpcEngine2$Invoker.invoke(ProtobufRpcEngine2.java:129)
       at jdk.proxy2/jdk.proxy2.$Proxy19.registerDatanode(Unknown Source)
       at org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPB.registerDatanode(DatanodeProtocolClientSideTranslatorPB.java:127)
       at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.register(BPServiceActor.java:793)
       at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.connectToNNAndHandshake(BPServiceActor.java:301)
       at org.apache.hadoop.hdfs.server.datanode.BPServiceActor.run(BPServiceActor.java:854)
       at java.base/java.lang.Thread.run(Thread.java:833)
23/04/19 11:30:29 ERROR datanode.DataNode: Initialization failed for Block pool BP-1137378862-10.53.196.128-1677151858288 (Datanode Uuid 948619de-5bae-4515-bfaa-d29bca496e2e) service to preprod-yak-exp-hyd-nn-0.preprod-yak-exp-hyd.yak-exp-preprod.svc.cluster.local/10.53.196.158:8020 Datanode denied communication with namenode because hostname cannot be resolved (ip=10.52.117.55, hostname=10.52.117.55): DatanodeRegistration(0.0.0.0:9866, datanodeUuid=948619de-5bae-4515-bfaa-d29bca496e2e, infoPort=9864, infoSecurePort=0, ipcPort=9867, storageInfo=lv=-57;cid=CID-80d2b728-dc30-460d-82a5-3c1b9bb37e3d;nsid=1101016859;c=1677151858288)
```

And on NameNode you will see the following failure as this registered node is not part of dfs.include list.

```
23/04/19 11:13:53 WARN blockmanagement.DataNodeManager: Unresolved DataNode registration: hostname cannot be resolved (ip=10.52.117.55, hostname=10.52.117.55)
23/04/19 11:13:53 INFO ipc.Server: IPC Server handler 6 on default port 8020, call Call#3 Retry#0 org.apache.Hadoop.hdfs.server.protocol.DataNodeProtocol.registerDataNode from 10.52.117.55:43560: org.apache.Hadoop.hdfs.server.protocol.DisallowedDataNodeException: DataNode denied communication with namenode because hostname cannot be resolved (ip=10.52.117.55, hostname=10.52.117.55): DataNodeRegistration(0.0.0.0:9866, DataNodeUuid=948619de-5bae-4515-bfaa-d29bca496e2e, infoPort=9864, infoSecurePort=0, ipcPort=9867, storageInfo=lv=-57;cid=CID-80d2b728-dc30-460d-82a5-3c1b9bb37e3d;nsid=1101016859;c=1677151858288)
```

**Approach**

```
- name: init-dnslookup
 command:
 - /bin/bash
 - -c
 - |
   #! /bin/bash
   set -m

   i=0
   while true; do
     echo "$i iteration"
     dig +short $(hostname -f) | grep -v -e '^$'
     if [ $? == 0 ]; then
       sleep 30 # 30 seconds default dns caching
       echo "Breaking..."
       break
     fi
     i=$((i + 1))
     sleep 1
   done
 cpuLimit: "0.2"
 memoryLimit: "128Mi"
 cpuRequest: "0.2"
 memoryRequest: "128Mi"
 securityContext:
   runAsUser: 1101
   runAsGroup: 1101
```

**Summary**:

## NameNode Bootstrap

In a HA(Highly Available) Hadoop cluster, two or more separate machines are configured as NameNodes. At any point in time, exactly one of the NameNodes is in an Active state, and the others are in a Standby state. The Active NameNode manages all client operations in the cluster, while the Standby is simply acting as a replica, maintaining enough state to provide a fast failover if necessary.

When the Active node performs any hdfs namespace modification, it durably records it in an edit log file stored in the shared directory. The Standby node constantly watches this directory for edits and applies them to its own namespace. In the event of a failover, the Standby will ensure that it has read all of the edits from the shared storage before promoting itself to the Active state. This ensures that the namespace state is fully synchronized before a failover occurs.

Further to this workflow, to set up a fresh HDFS cluster you should:

Challenge faced:

You cannot use the same bootstrap (startup scripts, init containers, and probes) when upgrading or restarting a cluster. In the k8s world, there is no mechanism to specify something different at the bootstrap time vs upgrading an already setup cluster.

### Approach

We introduced a global flag IsBootstrap and a field as part of container and init container. When this flag is ‘true’, it is a bootstrap deployment and includes all containers or init containers which are enabled with the IsBootstrap flag. We introduced a couple of init containers to be enabled only during bootstrap and will not be used after initial setup is done by marking isBootstrap flag as false.

The following are the containers which were tagged with IsBootstrap flag:

```
- name: init-zkfc
 isBootstrap: {{ default true .isBootstrap }}
 command:
 - /bin/bash
 - -c
 - |
   #! /bin/bash
   set -m -x

   export HADOOP_LOG_DIR={{ .Values.configuration.hadoopLogPath }}
   export HADOOP_CONF_DIR={{ .Values.configuration.hadoopConfigMountPath }}
   export HADOOP_HOME={{ .Values.configuration.hadoopHomePath }}

   echo "N" | $HADOOP_HOME/bin/hdfs zkfc -formatZK || true
 cpuLimit: "0.5"
 memoryLimit: "512Mi"
 cpuRequest: "0.5"
 memoryRequest: "512Mi"
 securityContext:
   runAsUser: {{ .Values.service.runAsUser }}
   runAsGroup: {{ .Values.service.runAsGroup }}
```

```
- name: init-namenode
 isBootstrap: {{ default true .isBootstrap }}
 command:
 - /bin/bash
 - -c
 - |
   #! /bin/bash
   set -m -x

   export HADOOP_LOG_DIR={{ .Values.configuration.hadoopLogPath }}
   export HADOOP_CONF_DIR={{ .Values.configuration.hadoopConfigMountPath }}
   export HADOOP_HOME={{ .Values.configuration.hadoopHomePath }}

   echo "N" | $HADOOP_HOME/bin/hdfs namenode -format $($HADOOP_HOME/bin/hdfs getconf -confKey dfs.nameservices) || true
 cpuLimit: "0.5"
 memoryLimit: "512Mi"
 cpuRequest: "0.5"
 memoryRequest: "512Mi"
 securityContext:
   runAsUser: 1101
   runAsGroup: 1101
 volumeMounts:
 - name: {{ .Values.mount.namenodeMountName }}
   mountPath: {{ .Values.mount.namenodeMountPath }}
```

**Summary**

## Rack Awareness

A rack is a collection of nodes / VMs which are connected by a network switch. A single rack or a group of racks connected by another network switch is called Fault Domain. For simplicity, this article uses Rack and Fault Domains interchangeably.

**Understanding Rack Awareness**

RackAwareness of a database like HBase requires that the data stored on that database is spread across the racks or fault domains in order to:

There are many ways in which the rack/fault domain can be made available to the applications in Kubernetes. Some of them are environment variables, details on a file that is auto-mounted on the container, labeled with fault domain information, etc. Each of them is available as part of the individual pod that’s coming up and** not available as a unified view globally.**

**Approach**

```
- name: init-faultdomain
 command:
 - /bin/bash
 - -c
 - |
   #! /bin/bash
   set -m -x

   export HBASE_LOG_DIR={{ .Values.configuration.hbaseLogPath }}
   export HBASE_CONF_DIR={{ .Values.configuration.hbaseConfigMountPath }}
   export HBASE_HOME={{ .Values.configuration.hbaseHomePath }}


   FAULT_DOMAIN_COMMAND={{ .Values.commands.faultDomainCommand | quote }}
   HOSTNAME=$(hostname -f)

   echo "Running command to get fault domain: $FAULT_DOMAIN_COMMAND"
   SMD=$(eval $FAULT_DOMAIN_COMMAND)
   echo "SMD value: $SMD"

   if [[ -n "$FAULT_DOMAIN_COMMAND" ]]; then
     echo "create /hbase-operator $SMD" | $HBASE_HOME/bin/hbase zkcli 2> /dev/null || true
     echo "create /hbase-operator/$HOSTNAME $SMD" | $HBASE_HOME/bin/hbase zkcli 2> /dev/null
     echo ""
     echo "Completed"
   fi
 cpuLimit: "0.1"
 memoryLimit: "386Mi"
 cpuRequest: "0.1"
 memoryRequest: "386Mi"
 securityContext:
   runAsUser: 1101
   runAsGroup: 1101
```

```
hmaster-1:/$ cat /opt/share/rack_topology.data
10.65.30.65 9
10.67.222.136 7
10.66.47.30 18
10.67.90.209 3
10.65.26.236 21
10.68.255.121 10
10.68.57.165 15
10.65.174.187 14
10.65.26.170 16
10.69.206.198 23
10.66.206.140 23
10.67.30.110 1
10.65.42.242 9
10.65.42.202 10
```

**Summary**

## Multi tenancy

At Flipkart, we run truly multi-tenant HBase clusters with physical isolation in terms of hardware between tenants. Here is a two-part series which talks about the same — [Part I](https://blog.flipkart.tech/hbase-multi-tenancy-part-i-37cad340c0fa),[ Part II](https://blog.flipkart.tech/hbase-multi-tenancy-part-ii-79488c19b03d%5C). An extension thought process to that is a multi-k8s-namespace cluster with an operator on one namespace, core components (like zookeepers, journalnodes, etc) on another namespace and each tenant in its own namespace. This enables easier management from devOps perspective and provides better isolation in terms of non-functional requirements.

**Approach**

To deploy a multi-tenant HBase cluster on multi-k8s-namespace, we had to work on RBAC policies as follows.

```
apiVersion: v1
kind: ServiceAccount
metadata:
 creationTimestamp: "2024-04-21T11:28:59Z"
 name: hbase-operator-controller
 namespace: operator-ns
 uid: 518563cd-5e80-49c7-a8e3-204a4ba751e1
secrets:
- name: hbase-operator-controller-token-rf34n
```

```
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
 creationTimestamp: null
 name: hbase-operator-manager-role
rules:
- apiGroups:
 - apps
 resources:
 - statefulsets
 verbs:
 - create
 - delete
 - get
 - list
 - patch
 - update
 - watch
```

```
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
 creationTimestamp: "2024-04-24T07:16:55Z"
 name: hbase-operator-controller-rolebinding
 namespace: tenant-ns
 uid: 77975457-3c14-41bc-b726-bc7a0ef09384
roleRef:
 apiGroup: rbac.authorization.k8s.io
 kind: ClusterRole
 name: hbase-operator-controller-role
subjects:
- kind: ServiceAccount
 name: hbase-operator-controller
 namespace: operator-ns
```

These RBAC on each of the namespaces where resources are to be created, authorizes the operator to create and watch for all the required resources such as statefulset, configmap, etc. This means, as soon as HbaseCluster or HbaseTenant Kind is deployed on a namespace, the operator watching for those custom resources on allowed namespaces hooks onto it and creates resources such as statefulsets, configmaps, etc. With all these resources, we built a multi-tenant HBase cluster spanning multiple namespaces, thereby providing highly maintainable large HBase clusters.

**Summary**

Kubernetes has a very flexible authorisation model in Role-Based Access Control (RBAC). There are 3 kinds of objects — Role/ClusterRole, RoleBinding/ClusterRoleBinding and ServiceAccounts. Using these objects, we built a maintainable large HBase cluster spanning across multiple namespaces with thousands of pods to form a single cluster.

## Probes and Entrypoint

Hbase has many components on production workloads, which include namenodes, journalnodes, zookeepers, DataNodes, Region Servers, hmasters with supporting sidecars, and init-containers. Some of these systems are built with legacy thoughts and are not very flexible to bring it up on k8s such as namenodes and journalnodes. This actually means scripts and probes are complex in nature and difficult to get them right.

For example: Here is the entry-point script for a namenode, where you can see — namenode has to become secondary if there is an existing active namenode; whereas it has to become active namenode if there are no other active namenodes.

```
#! /bin/bash
set -m -x

export HADOOP_LOG_DIR=$0
export HADOOP_CONF_DIR=$1
export HADOOP_HOME=$2

function shutdown() {
 echo "Stopping Namenode"
 is_active=$($HADOOP_HOME/bin/hdfs haadmin -getAllServiceState | grep "$(hostname -f)" | grep "active" | wc -l)

 if [[ $is_active == 1 ]]; then
   for i in $(echo $NNS | tr "," "\n"); do
     if [[ $($HADOOP_HOME/bin/hdfs haadmin -getServiceState $i | grep "standby" | wc -l) == 1 ]]; then
       STANDBY_SERVICE=$i
       break
     fi
   done

   echo "Is Active. Transitioning to standby"
   if [[ -n "$MY_SERVICE" && -n "$STANDBY_SERVICE" && $MY_SERVICE != $STANDBY_SERVICE ]]; then
     echo "Failing over from $MY_SERVICE to $STANDBY_SERVICE"
     $HADOOP_HOME/bin/hdfs haadmin -failover $MY_SERVICE $STANDBY_SERVICE
   else
     echo "$MY_SERVICE or $STANDBY_SERVICE is not defined or same. Cannot failover. Exitting..."
   fi
 else
  echo "Is not active"
 fi
 sleep 60
 echo "Completed shutdown cleanup"
 touch /lifecycle/nn-terminated
 $HADOOP_HOME/bin/hdfs --daemon stop namenode
}

NAMESERVICES=$($HADOOP_HOME/bin/hdfs getconf -confKey dfs.nameservices)
NNS=$($HADOOP_HOME/bin/hdfs getconf -confKey dfs.ha.namenodes.$NAMESERVICES)
MY_SERVICE=""
HTTP_ADDR=""
for i in $(echo $NNS | tr "," "\n"); do
 if [[ $($HADOOP_HOME/bin/hdfs getconf -confKey dfs.namenode.rpc-address.$NAMESERVICES.$i | sed 's/:[0-9]\+$//' | grep $(hostname -f) | wc -l ) == 1 ]]; then
   MY_SERVICE=$i
   HTTP_ADDR=$($HADOOP_HOME/bin/hdfs getconf -confKey dfs.namenode.http-address.$NAMESERVICES.$i)
 fi
done

echo "My Service: $MY_SERVICE"

trap shutdown SIGTERM
echo "N" | $HADOOP_HOME/bin/hdfs namenode -bootstrapStandby || true
exec $HADOOP_HOME/bin/hdfs namenode &
wait
```

As multiple components need to be brought up, repetitive code is possible and hence there is scope for reusable code blocks.

For example: Here is one block of code which is common for all components, where these are used to launch custom init-containers and side-car containers. The entire code for the same template can be found[ here](https://github.com/flipkart-incubator/hbase-k8s-operator/blob/main/helm-charts/hbase-chart/templates/meta/_component.tpl).

initContainers:

```
 {{- range $index, $elem := $.initContainers }}
 {{- . }}
 {{- end }}
 {{- range $index, $elem := .root.initContainers }}
 - name: {{ .name }}
   isBootstrap: {{ default false .isBootstrap }}
   command:
   - /bin/bash
   - -c
   - |
     {{- include $elem.templateName . | indent 6 }}
   cpuLimit: {{ .cpuLimit | quote }}
   memoryLimit: {{ .memoryLimit | quote }}
   cpuRequest: {{ .cpuRequest | quote }}
   memoryRequest: {{ .memoryRequest | quote }}
   securityContext:
     runAsUser: {{ $.Values.service.runAsUser }}
     runAsGroup: {{ $.Values.service.runAsGroup }}
   {{- if .volumeMounts }}
   volumeMounts:
   {{- range .volumeMounts }}
   - name: {{ .name }}
     mountPath: {{ .mountPath}}
     {{- if .readOnly }}
     readOnly: true
     {{- else }}
     readOnly: false
     {{- end }}
   {{- end }}
   {{- end }}
 {{- end }}
 {{- end }}
 {{- if .root.sidecarcontainers }}
 sidecarContainers:
 {{- range $index, $elem := .root.sidecarcontainers }}
 - name: {{ .name }}
   image: {{ .image }}
   {{- if .command }}
   command: {{ .command }}
   {{- end }}
   {{- if .args }}
   args: {{ .args }}
   {{- end }}
   cpuLimit: {{ .cpuLimit | quote }}
   memoryLimit: {{ .memoryLimit | quote }}
   cpuRequest: {{ .cpuRequest | quote }}
   memoryRequest: {{ .memoryRequest | quote }}
   securityContext:
     runAsUser: {{ .runAsUser }}
     runAsGroup: {{ .runAsGroup }}
   {{- if .volumeMounts }}
   volumeMounts:
   {{- range .volumeMounts }}
   - name: {{ .name }}
     mountPath: {{ .mountPath}}
     {{- if .readOnly }}
     readOnly: true
     {{- else }}
     readOnly: false
     {{- end }}
   {{- end }}
   {{- end }}
 {{- end }}
 {{- end }}
```

### Approach

We attempted to simplify the HBase cluster deployment with a layer of template abstraction through a helm chart. All the init-containers scripts, container entry points, probes, etc. required for running a reliable HBase cluster were bundled in the helm chart as a dependency. All customizations are done on top of this helm template package.

This will be used by deployment helm charts as a dependency and only provide a very limited set of key-value pairs as values files. You can find examples [here](https://github.com/flipkart-incubator/hbase-k8s-operator/blob/main/examples/hbasecluster-chart/values.yaml) along with the required configuration for HBase and Hadoop components. You can find all of the abstractions[ here](https://github.com/flipkart-incubator/hbase-k8s-operator/tree/main/helm-charts/hbase-chart/templates). These templates include about 1200 lines of code.

**Summary**

Templating Helm and providing it as a dependency is one of the powerful features of the Helm. This means you can abstract out the complexity and provide it as a dependency, whereas the final helm chart to be deployed can be thin. This was put in use as described above. You can find these helm charts [here](https://github.com/flipkart-incubator/hbase-k8s-operator/tree/main/helm-charts).

## Conclusion

At Flipkart, Hbase is a very popular key value, and a consistent database, predominantly used for OLTP use cases. On the downside, HBase is maintenance-heavy, and thus automation on the private cloud needed custom solutions. With statefulset maturing on Kubernetes and Kubernetes operator being a well-accepted recipe for managing custom resources within Kubernetes, we chose to take that route.

Hbase, along with Hadoop, having matured largely in the pre-cloud-native era, had several legacy thoughts not aligned with cloud-native thoughts. This meant we faced several challenges when we ported HBase onto Kubernetes stateful sets. Some of them needed innovative solutions owing to design issues. We have navigated through the challenges as mentioned in this article and seen success in running large production clusters with a massive scale in terms of rps and data volume.

We hope this blog can serve as food for thought for:

---
**Tags:** Hbase K8s Operator · Hbase · Kubernetes Operator
