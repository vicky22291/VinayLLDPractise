---
title: "AutoQC — Providing instant Catalog Feedback to Sellers"
author: "Advaith Sridhar"
date: "Feb 10, 2021"
url: "https://blog.flipkart.tech/autoqc-providing-instant-catalog-feedback-to-sellers-781c0bc901c6"
tags: ['Machine Learning', 'Ecommerce', 'Flipkart', 'Deep Learning', 'Data Science']
---

# AutoQC — Providing instant Catalog Feedback to Sellers

The image of a product is the first of many factors that shape an excellent online shopping experience. Sellers cannot show their products to customers directly (as they would offline), and hence need to present clear images and descriptions of their products for customers to understand the product correctly. Therefore, clear, informative and accurate product details are a necessity for Flipkart’s sellers.

## Flipkart’s Quality Checks

We check every product that sellers upload on Flipkart, to ensure that the images and product details are neat, clear, and understandable for our customers. Some examples of Flipkart’s quality checks are:

**Quality Check 1: Clear image:**

We ask sellers to place the product in the center of the image space and ensure that it covers at least 70% of the image area, so that viewers can see the product clearly.

**Quality Check 2: Appropriate content**

At Flipkart, we ensure that none of the products on our platform have inappropriate images or text that may hurt the sentiments of any religion, community, or the nation. We also do not allow any NSFW (Not Safe For Work) content on the platform.

**Quality Check 3: Correct product details**

We verify that the details of a product(_example: sleeve length, t-shirt color_) provided by the seller on the product page match with the image illustrations. This ensures that customers get accurate data about the product they wish to buy. It also helps our Search, Recommendations and other systems find the right products for each user.

Each product on the platform goes through **35–40** such quality checks (done manually by our internal quality-check team using product images) before being approved for listing on the Flipkart website!

## Maintaining Quality at Scale

We receive a high volume of products for verification and have to engage many people to fact-check incoming products. The high product volume and many quality checks results in the following challenges:

These challenges make our quality control process seem confusing for our Seller community, who would benefit from speed and objectivity in product catalog feedback.


---

## The AutoQC platform — ML for Quality Control

The AutoQC platform, with its ability to build multiple machine learning pipelines, can solve these challenges in product quality checks.

With the growing progress in Deep Neural Networks, we saw success in understanding image content using Image Classification and Object Detection models. The AutoQC platform enables you to build pipelines of high-performing models that understand product images at scale, and uses these model outputs to make quality check decisions for Flipkart.

### Step 1: Object Detection

Object Detection is the first model in the AutoQC pipeline. We train our Object Detection model to localize and identify different products in an image. The model has a single shot detector type architecture with a [ResNet50](https://arxiv.org/abs/1512.03385) backbone along with a feature pyramid network.

### Step 2: Extract Information

We pass the image outputs of Object Detection (here, the watches) through image classification and text extraction models. The following figure shows the information we extract about each watch that enters our catalog.

_Step 2.1: Image Classification_

There are many image classification models that we build for each category of products (watches, shirts, jeans etc), in order to understand relevant information about them. We quickly build these models for each category of products using transfer learning. We create one base model for a product category, following which we only re-train the last few layers of the base model, in order to build multiple classification models for each product category.

Leveraging transfer learning provides multiple benefits for us:

This hashelped us create over **200** image classification models in a brief span of time, with several hundred more planned in the months to come!

_Step 2.2: Text Extraction_

We also run each image through an OCR model to extract and analyse text. We use this to flag sensitive text (such as the product MRP) and enrich our understanding of our products.

### Step 3: Cross-check Seller Information

We use the model output data from the images to verify if they match the information that sellers provide about their products

Similar to the pipeline above, we’ve built several custom pipelines using the AutoQC platform.

## Strengths of the AutoQC Platform

While building individual pipelines is easy, the power of the platform comes from the following features:

## Conclusion

Automating the product quality check process through deep learning has helped us gather more information about our products, be more accurate, save time and effort, and provide faster feedback to our sellers.

**_“Artificial Intelligence is not about building a mind, it’s about the improvement of tools to solve problems”_**_ — _A_ _popular AI Quote

As new challenges change our world every day, we look forward to helping our sellers and customers using the latest breakthroughs that AI can offer.

---
**Tags:** Machine Learning · Ecommerce · Flipkart · Deep Learning · Data Science
