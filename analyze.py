import pandas as pd
from scipy.stats import ttest_rel
import matplotlib.pyplot as plt

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

result1_filter = (df['n_vars']==1) & (df['place_interval']==4) & df['is_used'] & (df['n_groups'] <= 3)
print(df[result1_filter])
result1_alt = df[result1_filter & (df['algorithm'] == 'Alternating')]
result1_bal = df[result1_filter & (df['algorithm'] == 'Balanced')]
m = pd.merge(result1_alt, result1_bal, on=["exp", 'n_vars', 'n_groups', 'var_name', 'place_interval', 'n', 'is_used'], suffixes=('_alt', '_bal'))
print(m)

print("p-values, alt:")
print(m['pvalue_alt'].describe())

print("p-values, bal:")
print(m['pvalue_bal'].describe())

m['advantage'] = m['pvalue_bal'] - m['pvalue_alt']
print("Amount that p-value is better for Balanced than Alternating:")
print(m['advantage'].describe())
bal_better_proportion = float((m['advantage'] > 0.0).sum()/len(m['advantage']))
print(bal_better_proportion)
as_percent = bal_better_proportion*100
print("Percent time Balanced was better: %d" % as_percent)

result = ttest_rel(m['pvalue_alt'], m['pvalue_bal'])
print(result)

fig, ax = plt.subplots(2, 1, sharex=True, sharey=True)
ax[0].hist(m['pvalue_bal'])
ax[0].set_title("Balanced")
ax[1].hist(m['pvalue_alt'])
ax[1].set_title("Alternating")
for a in ax:
    a.set_ylabel("Number of Simulations")
ax[1].set_xlabel("p-value, difference in groups")
fig.savefig('fig1.pdf')

print("Variables")
print(m['var_name'].unique())
fig, ax = plt.subplots(4, 2, sharex=True, sharey=True)
print(ax)
for i, n_groups in enumerate(m['n_groups'].unique()):
    for j, var_name in enumerate(m['var_name'].unique()):
        if var_name == 'score':
            var_type = 'continuous'
        else:
            var_type = 'categorical'
        subfilter = (m['n_groups']==n_groups) & (m['var_name']==var_name)
        ax_index = i*4+j*2
        ax[i*2, j].hist(m[subfilter]['pvalue_bal'])
        ax_index = i*4+j*2+1
        ax[i*2, j+1].hist(m[subfilter]['pvalue_alt'])
fig.savefig("fig2.pdf")

