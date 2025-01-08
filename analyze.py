import pandas as pd

df = pd.read_csv('results.csv')
summ = df.groupby(['algorithm', 'n_vars', 'place_interval'])['pvalue'].mean()

print("P values:")
print(summ)

summ = df.groupby(['algorithm', 'n_vars', 'place_interval'])['norm_rho'].mean()

print("Normalized Rho:")
print(summ)


