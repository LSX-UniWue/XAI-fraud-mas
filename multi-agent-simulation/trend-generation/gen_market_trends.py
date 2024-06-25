
import numpy as np
import pandas as pd

def main(datasource):

    def write_trends_to_file(df, include_amount):
        for name, group in df.groupby('Material'):
            print(f'\t\t{name}')
            filename = 'DailyPricesVolumes/' + str(name) + '-Price.arff'
            file = open(filename, 'w')
            file.write('% Title: ' + str(name) +
                       '-Price Trend \n% Source: \'Julian Tritscher et al., Open ERP System Data For Occupational Fraud Detection, 2022\' \n@RELATION '+
                       str(name) + f'-Time-Price{"-Amount" if include_amount else ""} \n@ATTRIBUTE time NUMERIC\n@ATTRIBUTE price NUMERIC' +
                       ("\n@ATTRIBUTE amount NUMERIC" if include_amount else "") + '\n@DATA\n')

            group_by_day = group.groupby('day').mean().reindex(range(256), fill_value=np.nan).interpolate(method='linear', limit_direction='both')

            for day, row in group_by_day.iterrows():
                out_str = str(day) + ',' + str(round(row['Stueckpreis'], 2))
                if include_amount:
                    out_str += ',' + str(round(row['Menge']))
                if day != len(group_by_day) - 1:
                    out_str += '\n'
                file.write(out_str)
            file.close()


    # load data
    df = pd.read_csv(f'../ml-framework/data/erp_fraud/{datasource}', encoding='ISO-8859-1', dtype={'Sperrgrund Menge' : 'string'})
    df = df.fillna('nan')

    # add individual price
    df = df[['Erfassungsuhrzeit', 'Material', 'Menge', 'Betrag Hauswaehr', 'Transaktionsart', 'Wertestring', 'Vorgangsart GL']]
    df['Erfassungsuhrzeit'] = pd.to_datetime(df['Erfassungsuhrzeit'], format='%H:%M:%S')
    df['Stueckpreis'] = np.round(df['Betrag Hauswaehr'] / df['Menge'], 2)

    # convert timestamps to days
    time_mapping = pd.read_csv('time_mapping.csv', encoding='ISO-8859-1', sep='\t', index_col=0)
    time_mapping['TIMESTAMP'] = pd.to_datetime(time_mapping['TIMESTAMP'], format='%H:%M:%S')
    intervals = pd.IntervalIndex.from_breaks(time_mapping[time_mapping['ACTION'] == 'CLOSE_DAY']['TIMESTAMP'])
    df['day'] = df['Erfassungsuhrzeit'].apply(lambda x: intervals.contains(x).argmax())

    # save educt trends
    print('\tGenerating Educt Trends...')
    df_educts = df[(df['Transaktionsart'] == 'Sachkontenbuchung') & (df['Wertestring'] == 'RE01')]
    write_trends_to_file(df=df_educts, include_amount=False)

    # save product trends
    print('\tGenerating Product Trends...')
    df_prod = df[(df['Transaktionsart'] == 'Sachkontenbuchung') & (df['Vorgangsart GL'] == 'SD00')]
    write_trends_to_file(df=df_prod, include_amount=True)
