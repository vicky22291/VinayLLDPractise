---
title: "Automated Backup Restore Validation"
subtitle: "As we do in Flipkart"
author: "Isha Aggarwal"
url: "https://blog.flipkart.tech/backup-restore-validation-83b1fea719be"
tags: ['Backup Restore', 'Restore Validation', 'Backup Restore Service', 'Flipkart Backup Restore']
---

# Automated Backup Restore Validation

> As we do in Flipkart

In the previous blog on how [Backup and Restore as a Service](https://blog.flipkart.tech/providing-backup-and-disaster-recovery-as-a-service-2caeff6ce278) or BRaaS is provided within Flipkart, we talked about why it is essential for an organization like Flipkart to have safeguards in place to protect its data against any kind of failures and disasters by storing data backups in a secured off-site repository.

In this blog, we’ll explore Restore as a Service, focussing on Automated Backup Restore Validation. We’ll discuss why it’s important, especially from Flipkart’s point of view. Let’s first get to know some important terms that will help you better understand this blog.

**RPO** (Recovery Point Objective): The maximum acceptable data loss during a disruption.  
**RTO** (Recovery Time Objective): The maximum allowable downtime for restoring a network or application.  
**BCP** (Business Continuity Plan): The strategy to keep a business running at an acceptable level during disruptions.  
**DR** (Disaster Recovery): Part of a BCP, it’s about recovering technology systems and applications after disasters or outages.

From Flipkart’s perspective, a BCP is a vital strategy that we employ to ensure the uninterrupted operation of our critical business functions, even amidst disasters such as natural calamities or cyber-attacks. Our comprehensive framework encompasses all facets of our organization, such as our workforce, communication channels, IT systems, business partners, and stakeholders. Within this framework, we outline specific measures and instruct the system behaviour to guarantee effective response when disasters strike.

Our disaster recovery plan focuses on:

BRaaS is responsible for taking over 7,000 backups daily from over 3,000 virtual machines, securing over 400 terabytes of data every day. However, all these backups would be pointless if customers couldn’t easily restore their systems during a disaster.

Now here’s the big question — How can we ensure that this backed-up data will actually work when needed to get our systems back on track during a crisis? This is where Automated Backup Restore Validation steps in. It’s a crucial process to make sure that your backed-up data can be successfully restored when things go wrong. Let’s dig deeper into why Backup Restore Validation is so important.

## Backup Restore Validation from SOX perspective

Backup Restore validation plays a major role in meeting SOX compliance requirements which revolve around key elements: Access control, Change management, Data Backup, and IT security. The Data Backup component of SOX compliance stipulates that systems must be in place to ensure that all financial records and other sensitive data are backed up — both onsite and offsite, using appropriate storage systems and these backups should be periodically tested for restorability.

The aim of this provision is to guarantee that data backups and restores serve as a safeguard against potential loss and damage in the event of a disaster. Accordingly we came up with the feature of validating the restorability of humongous data backups, in case of a disaster.

In Flipkart, out of the 1900+ BRaaS onboarded database clusters, around 70% are using MySQL as their datastore. Hence, we began with Automated Restore Validation feature of these MySQL clusters using our in-house managed MySQL service, **Altair**. We leveraged Altair’s proficiency in managing and upholding High Availability MySQL clusters for carrying out restore validations. We are extending this feature to other managed databases in future.

## Design

We follow the following steps to perform the Automated Restore Validation using the following design:

In our architecture, we have tried to optimize several key parameters to enhance efficiency and performance of the validation process. Let’s delve into the specifics:

**2. Database Version Dependency**

**3. Data Size and Disk Type**

**4. Cost and Network Bandwidth Usage**

## What’s Next?

**High level Design for Data Certification**

As the DB cluster owners know their cluster the best, and what data needs to be validated to certify a restore, we will ask for the data validation queries from the owners, with a constraint on the total execution time of these queries, because execution of these queries will add up to the total backup and restoration time.

We take a flush table read lock on the tables, before taking the snapshot of the data, to freeze the database and to ensure the consistency of the taken snapshot. Post that, these shared DB queries will be executed every time the full backup runs for that cluster. Instead of storing the output of the queries, which could vary in length and type, we will store the checksum of the outputs in the database.

When the same backup gets restored during automated restore validation, we run the same queries again on the restored cluster and fetch the checksum output. We match this checksum with the checksum stored while running the same set of queries during the backup, to ensure that the restored data matches with the backed up data.

## Summary

In this tech blog, we’ve delved into the critical role of BRaaS in guaranteeing the functionality and restorability of backups for various MySQL clusters within Flipkart. We’ve explored the architecture for Automated Restore Validation, identified its present limitations, and detailed how we’ve effectively addressed these shortcomings in the new design, leveraging the capabilities of the managed MySQL platform.

Furthermore, we’ve provided insights into the exciting future roadmap for Automated Restore Validation to encompass additional datastores like TiDB and enhance the validation process, ensuring even greater data reliability and integrity.

In the world of data restoration, it’s not just about bits and bytes, but the magic of bringing your precious data back to life. With the right tools and a sprinkle of tech wizardry (Read : BRaaS), your backups can go from zeros to heroes!

Thanks for reading :)

---
**Tags:** Backup Restore · Restore Validation · Backup Restore Service · Flipkart Backup Restore
