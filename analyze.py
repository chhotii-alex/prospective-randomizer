import pandas as pd
import numpy as np
from scipy.stats import ttest_rel, ttest_ind, pearsonr, linregress
import matplotlib.pyplot as plt
import matplotlib.transforms as mtrans
import seaborn as sns

df = pd.read_csv('results.csv')
print(df.columns)

df['is_bad'] = df['pvalue'] < 0.25

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
print("Runs with pvalue < 0.25")
print(df[result1_filter].groupby('algorithm')['is_bad'].sum())
print()

result1_alt = df[result1_filter & (df['algorithm'] == 'Alternating')]
result1_bal = df[result1_filter & (df['algorithm'] == 'Balanced')]
m = pd.merge(result1_alt, result1_bal, on=["exp", 'n_vars', 'n_groups', 'var_name', 'place_interval', 'n', 'is_used'], suffixes=('_alt', '_bal'))
print("Subset of dataset for first part of results:")
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
fig.savefig('fig1.png')

fig, ax = plt.subplots(4, 2, sharex=True, sharey=True, figsize=(6, 8))
for i, n_groups in enumerate(m['n_groups'].unique()):
    print("group i=", i)
    for j, var_name in enumerate(m['var_name'].unique()):
        if var_name == 'score':
            var_type = 'continuous'
        else:
            var_type = 'categorical'
        subfilter = (m['n_groups']==n_groups) & (m['var_name']==var_name)
        ax[2*j, i].hist(m[subfilter]['pvalue_bal'])
        ax[2*j+1, i].hist(m[subfilter]['pvalue_alt'])
        ax[2*j, i].set_title('%s variable, %d groups' % (var_type, n_groups))
        ax[2*j, i].set_xlabel('p-values, Balanced')
        ax[2*j+1, i].set_xlabel('p-values, Alternating')
fig.tight_layout()
line = plt.Line2D([0.52, 0.52],[0, 1], transform=fig.transFigure, color="black")
fig.add_artist(line)
line = plt.Line2D([0, 1],[0.515,0.515], transform=fig.transFigure, color="black")
fig.add_artist(line)

fig.savefig("fig2.pdf")
fig.savefig("fig2.png")

result2_filter = (df['n_vars']==1) & df['is_used'] & (df['n_groups'] <= 3)
result2_alt = df[result2_filter & (df['algorithm'] == 'Alternating')]
result2_bal = df[result2_filter & (df['algorithm'] == 'Balanced')]
m = pd.merge(result2_alt, result2_bal, on=["exp", 'n_vars', 'n_groups', 'var_name', 'place_interval', 'n', 'is_used'], suffixes=('_alt', '_bal'))
m['advantage'] = m['pvalue_bal'] - m['pvalue_alt']
print("Subset of dataset for second part of results:")
print(m)

place_vs_advantage = m.groupby('place_interval')['advantage'].mean()
print("effect of place interval on balanced's advantage")
print(place_vs_advantage)

place_vs_pvalue = m.groupby('place_interval')['pvalue_bal'].mean()
print("effect of place interval on balanced's p-value")
print(place_vs_pvalue)
print("Difference between 1 and 0")
print(place_vs_pvalue[1] - place_vs_pvalue[0])
fig, ax = plt.subplots()
#sns.violinplot(data=m, x="place_interval", y="pvalue_bal", ax=ax)
sns.boxplot(data=m, x="place_interval", y="pvalue_bal", ax=ax)
#ax.scatter(place_vs_pvalue.index, place_vs_pvalue)
#ax.set_title("Effect of Place Interval on Balanced p-value")
ax.set_xlabel("Place Interval")
ax.set_ylabel("Balanced pvalue")
fig.savefig('fig3.pdf')
fig.savefig('fig3.png')

the0s = m.loc[m['place_interval'] == 0, 'pvalue_bal']
the1s = m.loc[m['place_interval'] == 1, 'pvalue_bal']
r = ttest_ind(the0s, the1s)
print("Place interval is 0 vs. 1:")
print(r)

r = pearsonr(place_vs_pvalue.index[1:], place_vs_pvalue[1:])
print("Pearson corellation coef for place interval vs. Balanced p-values")
print(r)
r = linregress(place_vs_pvalue.index[1:], place_vs_pvalue[1:])
print("Linear regression of improvement with plae interval:")
print(r)

print("Analyses of effect of varying number of features")

print(df['n_vars'].describe())
filter = df['is_used']
filter_alt = filter & (df['algorithm'] == 'Alternating')
filter_bal = filter & (df['algorithm'] == 'Balanced')
m = pd.merge(df[filter_alt], df[filter_bal], on=['exp', 'n_vars', 'n_groups', 'var_name', 'place_interval', 'n', 'is_used'], suffixes=('_alt', '_bal'))
m['advantage'] = m['pvalue_bal'] - m['pvalue_alt']
print(m)
g = m.groupby('n_vars')['advantage'].mean()
print(g)

fig, ax = plt.subplots()
sns.boxplot(data=m, x='n_vars', y="advantage", ax=ax)
#ax.scatter(g.index, g)
ax.set_xlabel("Number of Baseline Variables Submitted")
ax.set_ylabel("(Balanced pvalue) - (Alternating pvalue)")
fig.savefig('fig4.pdf')
fig.savefig('fig4.png')
r = pearsonr(m['n_vars'], m['advantage'])
print("Correlation, # variables vs. p-value advantage:")
print(r)

print()
print("all data:")
print(df)
print("diversity calculations")
rho_results = df.drop_duplicates(subset=['algorithm', 'n', 'place_interval', 'n_vars', 'n_groups', 'exp', 'norm_rho']).copy()
rho_results.drop(columns=['norm_rho_used', 'is_used', 'is_bad', 'var_name'], inplace=True)
print(rho_results)
filter_alt = (rho_results['algorithm'] == 'Alternating')
filter_bal = (rho_results['algorithm'] == 'Balanced')
m = pd.merge(rho_results[filter_alt], rho_results[filter_bal], on=['exp', 'n_vars', 'n_groups', 'place_interval', 'n'], suffixes=('_alt', '_bal'))
print(m)
print(m['norm_rho_alt'].describe())
print(m['norm_rho_bal'].describe())
s = m.groupby('n_vars')['norm_rho_bal'].describe()
print(s)

g = m.groupby('n_vars')['norm_rho_bal'].mean()
print(g)

fig, ax = plt.subplots()
sns.boxplot(data=m, x='n_vars', y='norm_rho_bal', ax=ax)
#ax.scatter(g.index, g)
ax.set_xlabel("Number of Baseline Variables Submitted")
ax.set_ylabel("$\\bar{R}$")
fig.savefig('fig5.pdf')
fig.savefig('fig5.png')
r = pearsonr(m['n_vars'], m['norm_rho_bal'])
print("Correlation, # variables vs. Rho-bar:")
print(r)
