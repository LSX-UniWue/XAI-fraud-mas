import numpy as np
import pandas as pd
from lime import lime_tabular

from data.erp_fraud.erpDataset import ERPDataset


def explain_anomalies(X_anomalous,
                      X_benign,
                      xai_type,
                      detector,
                      out_template,
                      predict_fn=None,
                      dataset=None,
                      **kwargs):
    if predict_fn is None:
        predict_fn = detector.score_samples
    # Create and train the LIME explainer
    explainer = lime_tabular.LimeTabularExplainer(training_data=X_benign.values,  # need numpy array, not pandas df
                                                  feature_names=X_benign.columns.values.tolist(),
                                                  class_names='Fraud Score',
                                                  mode='regression',
                                                  categorical_features=list(range(len(X_benign.columns))),  # all
                                                  random_state=42)  # reproducibility

    if xai_type == 'lime_ordinal':
        # Create onehot dataset
        dataset_onehot = ERPDataset(train_path=dataset.train_path,
                                    test_path=dataset.test_path,
                                    numeric_preprocessing=dataset.numeric_preprocessing,
                                    categorical_preprocessing='onehot',
                                    keep_index=True,
                                    **kwargs)

        def predict_ordinal(x):
            # 1. Inverse transform data from ordinal to 'normal'
            x = pd.DataFrame(data=x, columns=X_anomalous.columns)
            categorical_data = dataset.preprocessed_data["cat_prep"].inverse_transform(x.iloc[:, :42])
            numerical_data = x.iloc[:, 42:]
            # 2. One-Hot encode categorical data
            X_test_ordinal_onehot = dataset_onehot.preprocessed_data["cat_prep"].transform(categorical_data)
            data = X_test_ordinal_onehot.join(numerical_data)
            # 3. Feed into the mode, score and return
            return predict_fn(data.to_numpy())

    lime_explanation = np.empty(X_anomalous.shape)
    for i in range(X_anomalous.shape[0]):
        # Explain all instances in the X_anomalous data
        exp = explainer.explain_instance(data_row=X_anomalous.iloc[i].to_numpy(),
                                         predict_fn=predict_ordinal if xai_type == 'lime_ordinal'
                                         else predict_fn,
                                         num_features=X_anomalous.shape[1])
        lime_exp = list(exp.local_exp.items())[0]
        lime_sample = lime_exp[1]  # LIME feature scores
        lime_sample = sorted(lime_sample, key=lambda x: x[0])  # Sort
        lime_sample = [x[1] for x in lime_sample]
        lime_explanation[i] = lime_sample

    if out_template:
        pd.DataFrame(lime_explanation,
                     columns=X_anomalous.columns,
                     index=X_anomalous.index).to_csv(out_template.format(xai_type))

    return lime_explanation
