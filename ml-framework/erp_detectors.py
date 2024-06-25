
import os
import numpy as np
import pandas as pd

from data.erp_fraud.erpDataset import ERPDataset
from anomaly_detection.util import get_score_dict
from anomaly_detection.autoencoder_torch import Autoencoder


def detect_anomalies(algorithm,
                     train_path,
                     test_path,
                     eval_path,
                     info_path,
                     experiment_name,
                     categorical='onehot',
                     numeric='buckets',
                     params=None,
                     output_scores=False,
                     seed=0):
    dataset = ERPDataset(train_path=train_path,
                         test_path=test_path,
                         eval_path=eval_path,
                         info_path=info_path,
                         numeric_preprocessing=numeric,
                         categorical_preprocessing=categorical,
                         seed=seed)

    X_train, y_train, X_eval, y_eval, X_test, y_test, num_prep, cat_prep = dataset.preprocessed_data.values()

    if algorithm == 'Autoencoder':
        detector_class = Autoencoder
        params['n_inputs'] = X_train.shape[1]
        params['seed'] = seed
    else:
        raise ValueError(f"Variable algorithm was: {algorithm}")

    # path to save autoencoder model
    save_path = None
    if 'save_path' in params.keys():
        save_path = params.pop('save_path')

    # Training
    if 'device' in params.keys():
        detector = detector_class(**params).fit(X_train, device=params['device'])
    else:
        detector = detector_class(**params).fit(X_train)

    # Anomaly classification outputs
    out_dict = params.copy()
    out_scores = {}
    for split in ['eval', 'test']:
        path = eval_path if split == 'eval' else test_path
        X_task = X_eval if split == 'eval' else X_test
        if path is None:
            continue

        if 'erp_fraud' in path:
            y_task = dataset.make_labels_fraud_only(split)
        else:
            y_task = dataset.preprocessed_data[f"y_{split}"]

        score_dict = detector.test(data=X_task, device=params.get('device', 'cpu'), return_metrics=False)
        scores = -1 * score_dict['pred']
        evaluation_dict = get_score_dict(scores=scores, y_true=y_task)

        evaluation_dict = {key + f'_{split}': val for key, val in evaluation_dict.items()}
        out_dict.update(evaluation_dict)
        out_scores.update({f'scores_{split}': scores, f'y_{split}': y_task})

    out_df = pd.DataFrame()
    out_df = out_df.append(out_dict, ignore_index=True)
    out_df.to_csv(os.path.join('./outputs/', experiment_name + '.csv'), index=False)
    print(out_df)
    if output_scores:
        score_df = pd.concat([pd.Series(ndarr, name=name) for name, ndarr in out_scores.items()], axis=1)
        score_df.to_csv(os.path.join('./outputs/', experiment_name + '_scores.csv'), index=False)

    if save_path:
        detector.save(save_path=save_path)
