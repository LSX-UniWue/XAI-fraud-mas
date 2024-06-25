
from functools import partial

from scipy.optimize import minimize
import numpy as np
import torch


def optimize_input_quasi_newton(data_point, kept_feature_idx, predict_fn, threshold=None, points_to_keep_distance=(),
                                proximity_weight=0.01, diversity_weight=0.01, device='cpu'):
    """
    idea from: http://www.bnikolic.co.uk/blog/pytorch/python/2021/02/28/pytorch-scipyminim.html

    Uses quasi-Newton optimization (Sequential Least Squares Programming) to find optimal input alteration for model
    according to:
    loss = predict_fn(y) + gamma * mean squared distance between optimized and original point (excluding fixed values)
           (optionally + delta * negative distance to points that should be avoided [Haldar2021])
    :param data_point:          numpy model input to optimize
    :param kept_feature_idx:    index of feature in data_point to keep, or None for not constraining any feature
                                Can also contain a list of indices to keep
    :param predict_fn:          function of pytorch model to optimize loss for
    :param proximity_weight:    float weight loss factor for proximity to the optimized input
    :param diversity_weight:    float weight loss factor for points_to_keep_distance
    :param points_to_keep_distance: list of numpy data points to keep distance from (distance added to loss function)
    :return:                    numpy optimized data_point
    """
    data_point = torch.autograd.Variable(torch.from_numpy(data_point.astype('float32')), requires_grad=True).to(device)
    proximity_weight = torch.Tensor([proximity_weight]).to(device)
    diversity_weight = torch.Tensor([diversity_weight]).to(device)
    if threshold is not None:
        threshold = torch.Tensor([threshold]).to(device)
    zero_elem = torch.tensor(0.0)
    if len(points_to_keep_distance) > 0:
        points_to_keep_distance = torch.tensor(np.concatenate([p.reshape([1, -1]) for p in points_to_keep_distance]),
                                               dtype=torch.float32, device=device)
    else:
        points_to_keep_distance = None

    def val_and_grad(x):
        pred_loss = predict_fn(x)
        if threshold is not None:  # hinge loss for anomaly score
            pred_loss = torch.max(zero_elem, pred_loss - threshold)
        prox_loss = proximity_weight * torch.linalg.vector_norm(data_point - x)
        if points_to_keep_distance is not None:
            divs_loss = diversity_weight * torch.max(-1 * torch.norm(points_to_keep_distance - x.repeat(len(points_to_keep_distance), 1), dim=1))
        else:
            divs_loss = 0
        loss = pred_loss + prox_loss + divs_loss

        loss.backward()
        grad = x.grad
        return loss, grad

    def func(x):
        """scipy needs flattened numpy array with float64, tensorflow tensors with float32"""
        return [vv.cpu().detach().numpy().astype(np.float64).flatten() for vv in
                val_and_grad(torch.tensor(x.reshape([1, -1]), dtype=torch.float32, requires_grad=True))]

    if kept_feature_idx is None:
        constraints = ()
    elif type(kept_feature_idx) == int:
        constraints = {'type': 'eq', 'fun': lambda x: x[kept_feature_idx] - data_point.detach().numpy()[:, kept_feature_idx]}
    elif len(kept_feature_idx) != 0:
        kept_feature_idx = np.where(kept_feature_idx)[0]
        constraints = []
        for kept_idx in kept_feature_idx:
            constraints.append(
                {'type': 'eq', 'fun': partial(lambda x, idx: x[idx] - data_point.detach().numpy()[:, idx], idx=kept_idx)})
    else:
        constraints = ()

    res = minimize(fun=func,
                   x0=data_point.detach().cpu(),
                   jac=True,
                   method='SLSQP',
                   constraints=constraints)
    opt_input = res.x.astype(np.float32).reshape([1, -1])

    return opt_input
