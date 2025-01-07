import pandas as pd

df = pd.read_excel('results.xlsx')
summ = df.groupby(['algorithm', 'place_interval'])['pvalue'].mean()

print(summ)
