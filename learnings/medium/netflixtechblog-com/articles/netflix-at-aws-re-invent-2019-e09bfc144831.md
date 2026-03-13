---
title: "Netflix at AWS re:Invent 2019"
author: "Netflix Technology Blog"
date: "Nov 22, 2019"
url: "https://netflixtechblog.com/netflix-at-aws-re-invent-2019-e09bfc144831"
tags: ['AWS', 'Netflixoss', 'Reinvent', 'Cloud Computing', 'Netflixsecurity']
---

# Netflix at AWS re:Invent 2019

_by _[_Shefali Vyas Dalal_](https://www.linkedin.com/in/shefalivyasdalal/)

AWS [re:Invent](https://reinvent.awsevents.com/) is a couple weeks away and our engineers & leaders are thrilled to be in attendance yet again this year! Please stop by our “Living Room” for an opportunity to connect or reconnect with Netflixers. We’ve compiled our speaking events below so you know what we’ve been working on. We look forward to seeing you there!

**Monday — December 2**

**1pm-2pm **[**CMP 326-R **](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=CMP326&searchType=session&tc=0&sortBy=abbreviationSort&p=)**Capacity Management Made Easy with Amazon EC2 Auto Scaling**

[Vadim Filanovsky](https://www.linkedin.com/in/vfilanovsky/), Senior Performance Engineer & Anoop Kapoor, AWS

**Abstract**:Amazon EC2 Auto Scaling offers a hands-free capacity management experience to help customers maintain a healthy fleet, improve application availability, and reduce costs. In this session, we deep-dive into how Amazon EC2 Auto Scaling works to simplify continuous fleet management and automatic scaling with changing load. Netflix delivers shows like Sacred Games, Stranger Things, Money Heist, and many more to more than 150 million subscribers across 190+ countries around the world. Netflix shares how Amazon EC2 Auto Scaling allows its infrastructure to automatically adapt to changing traffic patterns in order to keep its audience entertained and its costs on target.

**4:45pm-5:45pm **[**NFX 202**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=NFX202&searchType=session&tc=0&sortBy=abbreviationSort&p=)** A day in the life of a Netflix Engineer**

[Dave Hahn](https://twitter.com/relix42), SRE Engineering Manager

**Abstract**: Netflix is a large, ever-changing ecosystem serving millions of customers across the globe through cloud-based systems and a globally distributed CDN. This entertaining romp through the tech stack serves as an introduction to how we think about and design systems, the Netflix approach to operational challenges, and how other organizations can apply our thought processes and technologies. In this session, we discuss the technologies used to run a global streaming company, growing at scale, billions of metrics, benefits of chaos in production, and how culture affects your velocity and uptime.

**4:45pm-5:45pm **[**NFX 209**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=NFX209&searchType=session&tc=0&sortBy=abbreviationSort&p=)** File system as a service at Netflix**

[Kishore Kasi](https://www.linkedin.com/in/kishore-kasi-8a63165/), Senior Software Engineer

**Abstract**: As Netflix grows in original content creation, its need for storage is also increasing at a rapid pace. Technology advancements in content creation and consumption have also increased its data footprint. To sustain this data growth at Netflix, it has deployed open-source software Ceph using AWS services to achieve the required SLOs of some of the post-production workflows. In this talk, we share how Netflix deploys systems to meet its demands, Ceph’s design for high availability, and results from our benchmarking.

**Tuesday — December 3**

**5:30pm-6:30pm **[**CMP 326-R**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=CMP326&searchType=session&tc=0&sortBy=abbreviationSort&p=)** Capacity Management Made Easy**

[Vadim Filanovsky](https://www.linkedin.com/in/vfilanovsky/), Senior Performance Engineer & Anoop Kapoor, AWS

**Abstract**: Amazon EC2 Auto Scaling offers a hands-free capacity management experience to help customers maintain a healthy fleet, improve application availability, and reduce costs. In this session, we deep-dive into how Amazon EC2 Auto Scaling works to simplify continuous fleet management and automatic scaling with changing load. Netflix delivers shows like Sacred Games, Stranger Things, Money Heist, and many more to more than 150 million subscribers across 190+ countries around the world. Netflix shares how Amazon EC2 Auto Scaling allows its infrastructure to automatically adapt to changing traffic patterns in order to keep its audience entertained and its costs on target.

**Wednesday — December 4**

**10am-11am **[**NFX 203**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=NFX203&searchType=session&tc=0&sortBy=abbreviationSort&p=)** From Pitch to Play: The technology behind going from ideas to streaming**

[Ryan Schroeder](https://www.linkedin.com/in/ryan-schroeder-7229214a/), Senior Software Engineer

**Abstract**: It takes a lot of different technologies and teams to get entertainment from the idea stage through being available for streaming on the service. This session looks at what it takes to accept, produce, encode, and stream your favorite content. We explore all the systems necessary to make and stream content from Netflix.

**1pm-2pm **[**NFX 207**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=NFX207&searchType=session&tc=0&sortBy=abbreviationSort&p=)** Benchmarking stateful services in the cloud**

[Vinay Chella](https://twitter.com/vinaykchella), Data Platform Engineering Manager

**Abstract**: AWS cloud services make it possible to achieve millions of operations per second in a scalable fashion across multiple regions. Netflix runs dozens of stateful services on AWS under strict sub-millisecond tail-latency requirements, which brings unique challenges. In order to maintain performance, benchmarking is a vital part of our system’s lifecycle. In this session, we share our philosophy and lessons learned over the years of operating stateful services in AWS. We showcase our case studies, open-source tools in benchmarking, and how we ensure that AWS cloud services are serving our needs without compromising on tail latencies.

**3:15pm-4:15pm **[**OPN 209**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=OPN209&searchType=session&tc=0&sortBy=abbreviationSort&p=)** Netflix’s application deployment at scale**

[Andy Glover](https://twitter.com/aglover), Director Delivery Engineering & Paul Roberts, AWS

**Abstract**: Spinnaker is an open-source continuous-delivery platform created by Netflix to improve its developers’ efficiency and reduce the time it takes to get an application into production. Netflix has over 140 million members, and in this session, Netflix shares the tooling it uses to deploy applications to meet its customers’ needs. Join us to learn why Netflix created Spinnaker, how the platform is being used at scale, how the company works with the broader open-source community, and the work it’s doing with AWS to build out a new functions compute primitive.

**4pm-5pm **[**OPN 303-R**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=OPN303&searchType=session&tc=0&sortBy=abbreviationSort&p=)** BPF Performance Analysis**

[Brendan Gregg](https://twitter.com/brendangregg), Senior Performance Engineer

**Abstract**: Extended BPF (eBPF) is an open-source Linux technology that powers a whole new class of software: mini programs that run on events. Among its many uses, BPF can be used to create powerful performance-analysis tools capable of analyzing everything: CPUs, memory, disks, file systems, networking, languages, applications, and more. In this session, Netflix’s Brendan Gregg tours BPF tracing capabilities, including many new open-source performance analysis tools he developed for his new book “BPF Performance Tools: Linux System and Application Observability.” The talk also includes examples of using these tools in the Amazon Elastic Compute Cloud (Amazon EC2) cloud.

**Thursday — December 5**

**12:15pm-1:15pm **[**NFX 205**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=NFX205&searchType=session&tc=0&sortBy=abbreviationSort&p=)** Monitoring anomalous application behavior**

[Travis McPeak](https://twitter.com/travismcpeak), Application Security Engineering Manager & William Bengston, Director HashiCorp

**Abstract**: AWS CloudTrail provides a wealth of information on your AWS environment. In addition, teams can use it to perform basic anomaly detection by adding state. In this talk, Travis McPeak of Netflix and Will Bengtson introduce a system built strictly with off-the-shelf AWS components that tracks CloudTrail activity across multi-account environments and sends alerts when applications perform anomalous actions. By watching applications for anomalous actions, security and operations teams can monitor unusual and erroneous behavior. We share everything attendees need to implement CloudTrail in their own organizations.

**1pm-2pm **[**OPN 303-R1**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=OPN303&searchType=session&tc=0&sortBy=abbreviationSort&p=)** BPF Performance Analysis**

[Brendan Gregg](https://twitter.com/brendangregg), Senior Performance Engineer

**Abstract**: Extended BPF (eBPF) is an open-source Linux technology that powers a whole new class of software: mini programs that run on events. Among its many uses, BPF can be used to create powerful performance-analysis tools capable of analyzing everything: CPUs, memory, disks, file systems, networking, languages, applications, and more. In this session, Netflix’s Brendan Gregg tours BPF tracing capabilities, including many new open-source performance analysis tools he developed for his new book “BPF Performance Tools: Linux System and Application Observability.” The talk also includes examples of using these tools in the Amazon Elastic Compute Cloud (Amazon EC2) cloud.

**1:45pm-2:45pm **[**NFX 201**](https://www.portal.reinvent.awsevents.com/connect/search.ww?#loadSearch-searchPhrase=NFX201&searchType=session&tc=0&sortBy=abbreviationSort&p=)** More Data Science with less engineering: ML Infrastructure**

[Ville Tuulos](https://twitter.com/vtuulos), Machine Learning Infrastructure Engineering Manager

**Abstract**: Netflix is known for its unique culture that gives an extraordinary amount of freedom to individual engineers and data scientists. Our data scientists are expected to develop and operate large machine learning workflows autonomously without the need to be deeply experienced with systems or data engineering. Instead, we provide them with delightfully usable ML infrastructure that they can use to manage a project’s lifecycle. Our end-to-end ML infrastructure, Metaflow, was designed to leverage the strengths of AWS: elastic compute; high-throughput storage; and dynamic, scalable notebooks. In this session, we present our human-centric design principles that enable the autonomy our engineers enjoy.

---
**Tags:** AWS · Netflixoss · Reinvent · Cloud Computing · Netflixsecurity
