import pandas as pd

df = pd.read_csv('results.csv')
print(df.columns)

print("Number of subjects per protocol:")
print(df['n'].describe())
print()

print("Place intervals used:")
print(df['place_interval'].unique())
print()

summ = df.groupby(['algorithm', 'n_vars', 'place_interval'])['pvalue'].mean()

print("P values:")
print(summ)

summ = df.groupby(['algorithm', 'n_vars', 'place_interval'])['norm_rho'].mean()

print("Normalized Rho:")
print(summ)

result1_filter = (df['n_vars']==1) & (df['place_interval']==4) & df['is_used']
print(df[result1_filter])

