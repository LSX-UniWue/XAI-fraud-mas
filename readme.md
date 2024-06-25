# Data Generation for Explainable Occupational Fraud Detection

This repository contains code for the paper 'Data Generation for Explainable Occupational Fraud Detection' published at the 47th German Conference on Artificial Intelligence (KI 2024).

## Abstract

Occupational fraud, the deliberate misuse of company assets by employees, causes damages of around 5% of yearly company revenue. Recent work therefore focuses on automatically detecting occupational fraud through machine learning on the company data contained within enterprise resource planning systems. Since interpretability of these machine learning approaches is considered a relevant aspect of occupational fraud detection, first works have already integrated post-hoc explainable artificial intelligence approaches into their fraud detectors. While these explainers show promising first results, systematic advancement of explainable fraud detection methods is currently hindered by the general lack of ground truth explanations to evaluate explanation quality and choose suitable explainers. To avoid expensive expert annotations, we propose a data generation scheme based on multi-agent systems to obtain company data with labeled occupational fraud cases and ground truth explanations. Using this data generator, we design a framework that enables the optimization of post-hoc explainers for unlabeled company data. On two datasets, we experimentally show that our framework is able to successfully differentiate between explainers of high and low explanation quality, showcasing the potential of multi-agent-simulations to ensure proper performance of post-hoc explainers.

## Contained Materials

This implementation is based on our preliminary work [1] with code available at https://github.com/LSX-UniWue/ERP-fraud-mas.

### Multi-Agent-Based Simulation
The java-based multi-agent-based simulation for generating normal and fraudulent business processes with associated explanations is contained in the folder `multi-agent-simulation`.

Trends for the simulation can be generated through the provided python code in the folder `multi-agent-simulation/trend-generation`.

### Machine Learning Framework
All machine learning experiments from the paper are contained in the folder `ml-framework`.

The data contained in `ml-framework/data/erp_fraud` is the ERP fraud detection data from [2] which is openly available at https://professor-x.de/erp-fraud-data.

The MAS-generated data that is used in the paper can be found in `ml-framework/data/erp_fraud`.

New anomaly detectors can be trained through `ml-framework/erp_param_search.py`.

Post-hoc explanations for the fully trained models in `ml-framework/outputs/models` can be generated through `ml-framework/erp_xai.py`.

Results of the xai experiments from the paper can be found unter `ml-framework/outputs/explanation/erp_mas`.

[1] Tritscher, Julian, et al. "Occupational Fraud Detection through Agent-based Data Generation." 8th Workshop on MIning DAta for financial applicationS (MIDAS). (2023).
[2] Tritscher, Julian, et al. "Open ERP System Data For Occupational Fraud Detection." arXiv preprint arXiv:2206.04460 (2022).
