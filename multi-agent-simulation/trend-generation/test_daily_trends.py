import gen_market_trends
import plot_daily_trends
import shutil

datasource = 'erpsim2.csv'
regen_data = 1
show_plots = 1

if regen_data:
    print('Regenerating Data...')
    gen_market_trends.main(datasource)
    print('DONE')
    print('Zipping .arff Files...')
    shutil.make_archive('OutputData/dailyPrices', 'zip', 'DailyPricesVolumes')
    print('DONE')
print('Plotting .arff Files...')
plot_daily_trends.main(show_plots)
print('DONE')
