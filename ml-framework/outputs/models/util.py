
import pickle
from pathlib import Path

from anomaly_detection.autoencoder_torch import Autoencoder


def load_best_detector(model, train_path, test_path, model_folder='./outputs/models/'):
    if model == 'AE':
        if 'mas1' in train_path:
            detector = Autoencoder(**pickle.load(open(Path(model_folder) / 'best_params_mas.p', 'rb')))
            detector = detector.load(Path(model_folder)/ 'AE_mas1', only_model=False)
        elif 'mas2' in train_path:
            detector = Autoencoder(**pickle.load(open(Path(model_folder) / 'best_params_mas.p', 'rb')))
            detector = detector.load(Path(model_folder) / 'AE_mas2', only_model=False)
        elif 'erpsim1' in train_path:
            detector = Autoencoder(**pickle.load(open(Path(model_folder) / 'best_params_erpsim1.p', 'rb')))
            detector = detector.load(Path('./outputs/models/') / 'AE_erpsim1', only_model=False)
        elif 'erpsim2' in train_path:
            detector = Autoencoder(**pickle.load(open(Path(model_folder) / 'best_params_erpsim2.p', 'rb')))
            detector = detector.load(Path('./outputs/models/') / 'AE_erpsim2', only_model=False)
        else:
            raise ValueError("Unknown train and test dataset combination.")
    else:
        raise ValueError(f"'model' was {model}")

    return detector
