import os
import time
import requests
from pathlib import Path
import random
from pprint import pp
from scipy.stats import f_oneway, ttest_ind, chi2_contingency
import numpy as np
import pandas as pd
from collections import defaultdict
from greylock import Metacommunity
from greylock.similarity import SimilarityFromFunction

random.seed(42)

protocols = {
    "simpletest" : {
        'groupNames': ['A', 'B'],
        'variableSpec': {'score': None},
        'allowRevision': False,
    },
    "multivar": {
        'groupNames': ['A', 'B'],
        'variableSpec': {'iq': None,
                         'shoesize': None},
        'allowRevision': False,
    },
    "moregroups": {
        'groupNames': ['A', 'B', 'C', 'D'],
        'variableSpec': {'score': None},
        'allowRevision': False,
    },
    "categorical": {
        'groupNames': ['A', 'B'],
        'variableSpec': {'state': ['Iowa', 'Ohio', 'Illinois', 'Pennsylvania']},
        'allowRevision': False,
    },
    "multitype": {
        'groupNames': ['A', 'B'],
        'variableSpec': {'state': ['Iowa', 'Ohio', 'Illinois', 'Pennsylvania'],
                         'score': None},
        'allowRevision': False,
    },
}

def setup():
    print("Please shut down the Spring Boot implementation and start it again.")
    time.sleep(0.25)
    reply = input("Please hit the Enter key when you see 'Started ProsRandApplication' in the console.")

def make_phony_features(variables):
    data = {}
    for var in variables:
        choices = variables[var]
        if choices is None:
            data[var] = random.gauss(mu=50, sigma=20) #random.uniform(1, 99)
        else:
            data[var] = random.choice(choices)
    return data

def make_features_for(protocol_name):
    return make_phony_features(protocols[protocol_name]['variableSpec'])

def make_url(protocol_name, pid, parts):
    url = 'http://localhost:8080/%s_%s/%s' % (protocol_name, pid, '/'.join(parts))
    return url

def start_protocol(protocol_name, pid):
    url = make_url(protocol_name, pid, ['start?temp=true'])
    r = requests.post(url,
                      json=protocols[protocol_name])
    assert r.status_code == 200

# This submits feature values but doesn't demand immediate group assignment
def put_random_subject(subjectID, protocol_name, pid, features=None):
    if features is None:
        features = make_features_for(protocol_name)
    r = requests.post(make_url(protocol_name, pid, ['subject', subjectID]),
                      json=features)
    assert r.status_code == 200

# This submits feature values and demands immediate group assignment
def place_random_subject(subjectID, protocol_name, pid, features=None):
    if features is None:
        features = make_features_for(protocol_name)
    r = requests.post(make_url(protocol_name, pid, ['subject', subjectID, 'group']),
                      json=features)
    assert r.status_code == 200

# Trigger assignment of this one subject specifically (although might trigger
# other subjects as well as a side-effect) and return the group for that subject
def get_group(subjectID, protocol_name, pid):
    r = requests.get(make_url(protocol_name, pid, ['subject', subjectID, 'group']))
    assert r.status_code == 200
    return r.text

def assign_all(protocol_name, pid):
    r = requests.post(make_url(protocol_name, pid, ['assignall']), {})
    assert r.status_code == 200

def get_groups(protocol_name, pid):
    r = requests.get(make_url(protocol_name, pid, ['groups', 'strings']))
    assert r.status_code == 200
    return r.json()

def id_gen():
    num = 1
    while True:
        yield 'S' + str(num).zfill(3)
        num += 1

def pid_gen():
    for num in range(25):
        yield 'P' + str(num).zfill(2)

max_subject_count = 20

### Relevant to diversity analysis of group composition

def make_similarity_function(vars):
    def similarity(s1, s2):
        accum = 0.0
        for var_name, var_levels in vars.items():
            if var_levels is None:
                diff = float(getattr(s1, var_name))-float(getattr(s2, var_name))
                r = np.exp(-abs(diff)/25)
            else:
                if getattr(s1, var_name) == getattr(s2, var_name):
                    r = 1.0
                else:
                    r = 0.0
            accum += r
        result = accum/len(vars)
        return result
    return similarity

def make_metacommunity(groups, vars):
    number_of_groups = len(groups)
    number_of_subjects = np.sum([len(g['subjects']) for g in groups])
    abundance = np.zeros((number_of_subjects, number_of_groups))
    row = 0
    for i, group in enumerate(groups):
        n_this_group = len(group['subjects'])
        abundance[row:(row+n_this_group), i] = 1
        row += n_this_group
    similarity_function = make_similarity_function(vars)
    d = defaultdict(list)
    for group in groups:
        for subject in group['subjects']:
            for key, value in subject.items():
                d[key].append(value)
    X = pd.DataFrame(d)
    metacommunity = Metacommunity(abundance,
                                  similarity=SimilarityFromFunction(similarity_function,
                                                                    X=X))
    return metacommunity

def get_diversity_measure(groups, vars, level, measure):
    m = make_metacommunity(groups, vars)
    results = m.to_dataframe(viewpoint=[0, 1, np.inf]).set_index(['viewpoint', 'community'])
    return results.loc[(0.0, level), measure]

### Relevant to saving results to DataFrame

d = defaultdict(list)

def append_row(algorithm, n, place_interval, n_vars, exp_num, var, pvalue, norm_rho):
    d['algorithm'].append(algorithm)
    d['n'].append(n)
    d['place_interval'].append(place_interval)
    d['n_vars'].append(n_vars)
    d['exp_num'].append(exp_num)
    d['var'].append(var)
    d['pvalue'].append(pvalue)
    d['norm_rho'].append(norm_rho)

### Relevant to running many simulations
    
def evaluate_protocol_result(protocol_name, prot_suff, algorithm):
    assign_all(protocol_name, prot_suff)
    groups = get_groups(protocol_name, prot_suff)
    (min_group_size, max_group_size) = (999999, 0)
    for group in groups:
        group_size = len(group['subjects'])
        if group_size < min_group_size:
            min_group_size = group_size
        if group_size > max_group_size:
            max_group_size = group_size
    assert (max_group_size - min_group_size) <= 1
    for var in protocols[protocol_name]['variableSpec']:
        var_options = protocols[protocol_name]['variableSpec'][var]
        if var_options is None:
            continuous = True
        else:
            continuous = False
        if continuous:
            samples = []
            for group in groups:
                samples.append( [float(subject[var]) for subject in group['subjects']])
            if len(groups) > 2:
                r = f_oneway(*samples)
            else:
                r = ttest_ind(*samples)
        else:
            table = np.zeros((len(var_options), len(groups)))
            for j, group in enumerate(groups):
                for subject in group['subjects']:
                    feature_value = subject[var]
                    table[var_options.index(feature_value), j] += 1
            # do not use all possible var_options, as we may get zeros in expected frequencies
            options_to_keep_indices = [i for i in range(len(var_options)) if np.sum(table[i, :]) > 0]
            table = table[options_to_keep_indices, :]
            r = chi2_contingency(table)
        norm_rho = get_diversity_measure(groups, protocols[protocol_name]['variableSpec'],
                                         'metacommunity', 'normalized_rho')
        append_row(algorithm, max_subject_count, place_interval,
                   len(protocols[protocol_name]['variableSpec']),
                   exp_num, var,
                   r.pvalue, norm_rho)

def compare_algorithms(protocol_name, competitors):
    for algorithm, prot_suff in competitors.items():
        evaluate_protocol_result(protocol_name, prot_suff, algorithm)

setup()
exp_num = 0
for pid in pid_gen():
    for protocol_name in protocols.keys():
        for place_interval in [0, 1, 2, 5, 10]:
            exp_num += 1
            competitors = {}
            for algorithm in ['Alternating', 'Balanced']:
                protocols[protocol_name]['algorithm'] = algorithm
                prot_suff = pid + algorithm + str(place_interval) + str(max_subject_count)
                start_protocol(protocol_name, prot_suff)
                competitors[algorithm] = prot_suff
            put_subjects = []
            for count, s_id in enumerate(id_gen()):
                if (count == max_subject_count):
                    break
                features = make_features_for(protocol_name)
                for prot_suff in competitors.values():
                    put_random_subject(s_id, protocol_name, prot_suff, features)
                put_subjects.append(s_id)
                if count >= place_interval:
                    for prot_suff in competitors.values():
                        get_group(put_subjects[0], protocol_name, prot_suff)
                    put_subjects.pop(0)
            compare_algorithms(protocol_name, competitors)

df = pd.DataFrame(d)
df.to_csv('results.csv', index=False)


                
        

