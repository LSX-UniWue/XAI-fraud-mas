
import pandas as pd
import matplotlib.pyplot as plt

product = ['F12', 'F15', 'F16', 'P01', 'P02', 'R01', 'R02', 'R03', 'R04', 'R05', 'R06']


def main(showplots):
    for prd in product:
        if prd.startswith('F'):
            in_df = pd.read_csv(f'DailyPricesVolumes/AA-{prd}-Price.arff', skiprows=7, header=None)
            in_df.columns = ['day', 'price', 'amount']
        else:
            in_df = pd.read_csv(f'DailyPricesVolumes/AA-{prd}-Price.arff', skiprows=6, header=None)
            in_df.columns = ['day', 'price']
        in_df.plot(x='day', y='price', title=prd)
        plt.savefig(f'OutputData/Plot-{prd}.png')
        if showplots:
            plt.show()
