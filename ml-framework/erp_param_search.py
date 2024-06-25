
from sklearn.model_selection import ParameterGrid

from erp_detectors import detect_anomalies


def get_param_grid(algorithm, setting, seed):

    if algorithm == 'Autoencoder':
        if 'param_search' in setting:
            return ParameterGrid({'n_layers': [2, 3, 4], 'n_bottleneck': [8, 16, 32], 'learning_rate': [1e-2, 1e-3, 1e-4],
                                  'epochs': [50], 'batch_size': [2048], 'cpus': [8], 'shuffle': [True], 'verbose': [1],
                                  'device': ['cuda'], 'save_path': [None]})
        elif 'best' in setting:
            return ParameterGrid({'cpus': [8], 'n_layers': [2], 'n_bottleneck': [32], 'epochs': [50],  # mas1+2 best buckets
                                  'batch_size': [2048], 'learning_rate': [1e-2], 'shuffle': [True],
                                  'verbose': [1], 'device': ['cuda'], 'save_path': [None]})

    else:
        raise ValueError(f"Variable algorithm was: {algorithm}")


if __name__ == '__main__':

    numeric = 'buckets'
    algorithm = 'Autoencoder'
    # Setting: One of ['param_search_mas1', 'param_search_mas2', 'best_erpsim1', 'best_erpsim2', 'baseline_erpsim1', 'baseline_erpsim2']
    setting = 'best_erpsim1'

    erpClassParams = {'eval_path': None,
                      'info_path': './data/erp_fraud/column_information.csv'}  # column names and dtypes
    if setting == 'param_search_mas1':
        erpClassParams['train_path'] = './data/erp_mas/normal/mas1.csv'
        erpClassParams['test_path'] = './data/erp_mas/fraud/mas1.csv'
    elif setting == 'param_search_mas2':
        erpClassParams['train_path'] = './data/erp_mas/normal/mas2.csv'
        erpClassParams['test_path'] = './data/erp_mas/fraud/mas2.csv'
    elif setting in ['best_erpsim1', 'baseline_erpsim1']:
        erpClassParams['train_path'] = './data/erp_fraud/erpsim1.csv'
        erpClassParams['test_path'] = './data/erp_fraud/erpsim1.csv'
    elif setting in ['best_erpsim2', 'baseline_erpsim2']:
        erpClassParams['train_path'] = './data/erp_fraud/erpsim2.csv'
        erpClassParams['test_path'] = './data/erp_fraud/erpsim2.csv'
    seeds = [0]  # list(range(0, 5))  # 0 for best, 0-4 for param_search

    for seed in seeds:
        param_grid = get_param_grid(algorithm=algorithm, setting=setting, seed=seed)
        for j, params in enumerate(param_grid):
            detect_anomalies(algorithm=algorithm,
                             **erpClassParams,
                             experiment_name=f'{algorithm}_erp_fraud_{str(seed)}_{str(j)}',
                             categorical='onehot',
                             numeric=numeric,
                             params=params,
                             output_scores=True,
                             seed=seed)
