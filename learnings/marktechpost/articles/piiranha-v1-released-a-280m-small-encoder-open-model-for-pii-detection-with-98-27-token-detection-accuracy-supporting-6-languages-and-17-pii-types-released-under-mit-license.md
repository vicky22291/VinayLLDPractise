---
title: "Piiranha-v1 Released: A 280M Small Encoder Open Model for PII Detection with 98.27% Token Detection Accuracy, Supporting 6 Languages and 17 PII Types, Released Under MIT License"
date: "2024-09-14T09:22:37"
modified: "2024-09-14T09:22:41"
url: "https://www.marktechpost.com/2024/09/14/piiranha-v1-released-a-280m-small-encoder-open-model-for-pii-detection-with-98-27-token-detection-accuracy-supporting-6-languages-and-17-pii-types-released-under-mit-license/"
slug: "piiranha-v1-released-a-280m-small-encoder-open-model-for-pii-detection-with-98-27-token-detection-accuracy-supporting-6-languages-and-17-pii-types-released-under-mit-license"
---

![Piiranha-v1 Released: A 280M Small Encoder Open Model for PII Detection with 98.27% Token Detection Accuracy, Supporting 6 Languages and 17 PII Types, Released Under MIT License](../images/901343f621700856.jpg)

# Piiranha-v1 Released: A 280M Small Encoder Open Model for PII Detection with 98.27% Token Detection Accuracy, Supporting 6 Languages and 17 PII Types, Released Under MIT License

> The Internet Integrity Initiative Team has made a significant stride in data privacy by releasing Piiranha-v1, a model specifically designed to detect and protect personal information. This tool is built to identify personally identifiable information (PII) across a wide variety of textual data, providing an essential service at a time when digital privacy concerns are […]

The Internet Integrity Initiative Team has made a significant stride in data privacy by releasing [**Piiranha-v1**](https://huggingface.co/iiiorg/piiranha-v1-detect-personal-information), a model specifically designed to detect and protect personal information. This tool is built to identify personally identifiable information (PII) across a wide variety of textual data, providing an essential service at a time when digital privacy concerns are paramount.

Piiranha-v1, a lightweight 280M encoder model for PII detection, has been released under the MIT license, offering advanced capabilities in detecting personal identifiable information. Supporting six languages, English, Spanish, French, German, Italian, and Dutch, Piiranha-v1 achieves near-perfect detection, with an impressive 98.27% PII token detection rate and a 99.44% overall classification accuracy. It excels in identifying 17 types of PII, with 100% accuracy for emails and near-perfect precision for passwords. Piiranha-v1 is based on the powerful DeBERTa-v3 architecture. This makes it a versatile tool suitable for global data protection efforts.

The model’s performance in detecting various PII types is particularly noteworthy. For example, it has near-perfect accuracy in identifying email addresses and telephone numbers, with an F1 score of 1.0 and 0.99, respectively. Piiranha-v1 is extremely effective at recognizing passwords and usernames, with an accuracy of nearly 100% in these areas. These metrics indicate its utility in safeguarding sensitive information in digital communication and transaction environments.

One of Piiranha-v1’s key advantages is its ability to flag PII even when the specific data category may be misclassified. For instance, the model may occasionally confuse first names with last names, but it still correctly identifies the information as PII. This flexibility makes Piiranha-v1 a robust tool for real-world applications where data inconsistencies often occur. Such misclassifications, while technically errors, do not compromise the model’s primary goal of identifying and protecting sensitive data.

In collaboration with partners like Hugging Face and Akash Network, the Internet Integrity Initiative Team trained Piiranha-v1 using a comprehensive dataset comprising over 400,000 records of masked PII. This extensive training has resulted in a model that boasts high accuracy and demonstrates resilience in varied linguistic and contextual scenarios. The use of H100 GPUs during training allowed the model to reach high levels of efficiency, ensuring rapid identification of PII in real-time applications.

Despite its high accuracy, the developers of Piiranha-v1 emphasize that it should be used with caution. While the model is highly reliable, the team does not assume responsibility for any incorrect predictions it may produce. This advisory serves as a reminder of the limitations inherent in any machine learning model, particularly one tasked with something as complex as PII detection across multiple languages and data formats.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXcQGO8_fT9rzH_D5P6HYG3ZMHZa4IKaC4I6ULAUU5K_LY0iMXPO2xo83f3R8cI7Iro6vPM9ZKO1hJZL77OkkK-IxTnlWEPmoYyZTZSr75ckqBD26cuVO6oocKzLsgRFZNozSN5JTalSRuJ7RaDii4oEpqeR?key=JGVKNGelsurWzvV8gi7WNg)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXcQGO8_fT9rzH_D5P6HYG3ZMHZa4IKaC4I6ULAUU5K_LY0iMXPO2xo83f3R8cI7Iro6vPM9ZKO1hJZL77OkkK-IxTnlWEPmoYyZTZSr75ckqBD26cuVO6oocKzLsgRFZNozSN5JTalSRuJ7RaDii4oEpqeR?key=JGVKNGelsurWzvV8gi7WNg)*[**Image Source**](https://huggingface.co/iiiorg/piiranha-v1-detect-personal-information)*

The training process for Piiranha-v1 was meticulously planned to optimize its performance. The model was trained for five epochs using a batch size of 128. It leveraged mixed-precision training with Native AMP to ensure speed and accuracy during the learning process. The result is a highly refined model capable of recognizing subtle variations in PII tokens, which is particularly important for identifying information that might be obscured or presented in non-standard formats.

The model’s evaluation results further highlight its impressive capabilities. Piiranha-v1 achieves an F1-score of 93.12% when tested on a dataset containing approximately 73,000 sentences. Its precision and recall metrics are also strong, at 93.16% and 93.08%, respectively. These figures, while slightly lower than the overall accuracy due to the model’s multi-class classification task, still represent a high level of competence in PII detection.

In practical terms, Piiranha-v1 can be used in various applications. It is particularly well-suited for organizations that handle large volumes of personal data, such as financial institutions, healthcare providers, and tech companies. By integrating Piiranha-v1 into their data processing pipelines, these businesses and organizations can ensure that sensitive information is automatically flagged and redacted, reducing the risk of data breaches & ensuring compliance with privacy regulations like the GDPR and CCPA.

![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXfQqY5ytcwRiUYNhpPOb8_saA5_9StRMBGnfKDByO-lZ9P4zBJx8n_XLtXReRunaRCmW49TxkaxZGv5aZ0KiYHPgRpHCFafRcn3U9bBcQipny7XYOcerez7lLiipERfkvpB5C_KaPuATICwo4CQT6NGSSI_?key=JGVKNGelsurWzvV8gi7WNg)![](https://lh7-rt.googleusercontent.com/docsz/AD_4nXfQqY5ytcwRiUYNhpPOb8_saA5_9StRMBGnfKDByO-lZ9P4zBJx8n_XLtXReRunaRCmW49TxkaxZGv5aZ0KiYHPgRpHCFafRcn3U9bBcQipny7XYOcerez7lLiipERfkvpB5C_KaPuATICwo4CQT6NGSSI_?key=JGVKNGelsurWzvV8gi7WNg)*[**Image Source**](https://huggingface.co/iiiorg/piiranha-v1-detect-personal-information)*

The Piiranha-v1 model is also available for deployment through Hugging Face’s platform, where it can be easily integrated into existing workflows. The model is under the Creative Commons BY-NC-ND 4.0, which allows for broad usage within the confines of non-commercial applications. This open-access approach further reinforces the Internet Integrity Initiative Team’s commitment to improving data privacy on a global scale.

In conclusion, Piiranha-v1 represents a significant advancement in PII detection. Its high accuracy, multi-language support, and flexible application possibilities make it a valuable tool for any organization looking to enhance its data privacy efforts. The Internet Integrity Initiative Team has delivered a model that meets the technical challenges of PII detection and reflects the growing importance of safeguarding personal information in today’s digital world. As concerns over data privacy continue to escalate, tools like Piiranha-v1 will undoubtedly play a crucial role in protecting individuals’ sensitive information from exposure and misuse.

---

Check out the **[Model Card](https://huggingface.co/iiiorg/piiranha-v1-detect-personal-information) and [Colab Notebook](https://colab.research.google.com/github/williamgao1729/piiranha-quickstart/blob/main/piiranha_quickstart%20(1).ipynb)**. All credit for this research goes to the researchers of this project. Also, don’t forget to follow us on **[Twitter](https://twitter.com/Marktechpost)** and join our **[Telegram Channel](https://pxl.to/at72b5j)** and [**LinkedIn Gr**](https://www.linkedin.com/groups/13668564/)[**oup**](https://www.linkedin.com/groups/13668564/).

📨 If you like our work, you will love our[** Newsletter..**](https://marktechpost-newsletter.beehiiv.com/subscribe)

Don’t Forget to join our **[50k+ ML SubReddit](https://www.reddit.com/r/machinelearningnews/)**

**[⏩ ⏩ FREE AI WEBINAR: ‘SAM 2 for Video: How to Fine-tune On Your Data’ (Wed, Sep 25, 4:00 AM – 4:45 AM EST)](https://encord.com/webinar/sam2-for-video/?utm_medium=affiliate&utm_source=newsletter&utm_campaign=marktechpost&utm_content=sam2video)**
