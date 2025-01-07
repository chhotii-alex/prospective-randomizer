import os
import time
import requests
from pathlib import Path
import random
from pprint import pp
from scipy.stats import f_oneway, ttest_ind, chi2_contingency
import numpy as np
import pandas as pd

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
}

def setup():
    print("Please shut down the Spring Boot implemenation and start it again.")
    time.sleep(0.25)
    reply = input("Please hit the Enter key when you see 'Started ProsRandApplication' in the console.")

def make_phony_features(variables):
    data = {}
    for var in variables:
        choices = variables[var]
        if choices is None:
            data[var] = random.uniform(1, 99)
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
    r = requests.get(make_url(protocol_name, pid, ['subject', subjectID, group]))
    assert r.status_code == 200
    return r.text()

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

d = {
    'algorithm': [],
    'n': [],
    'placing': [],
    'n_vars': [],
    'pvalue': [],
}

def append_row(algorithm, n, placing, n_vars, pvalue):
    d['algorithm'].append(algorithm)
    d['n'].append(n)
    d['placing'].append(placing)
    d['n_vars'].append(n_vars)
    d['pvalue'].append(pvalue)

setup();
for pid in pid_gen():
    for protocol_name in protocols.keys():
        for placing in [True, False]:
            for max_subject_count in [12, 24, 36]:
                competitors = {}
                for algorithm in ['Alternating', 'Balanced']:
                    protocols[protocol_name]['algorithm'] = algorithm
                    prot_suff = pid + algorithm + str(placing) + str(max_subject_count)
                    start_protocol(protocol_name, prot_suff)
                    competitors[algorithm] = prot_suff
                put_subjects = []
                for s_id in id_gen():
                    features = make_features_for(protocol_name)
                    for prot_suff in competitors.values():
                        if placing:
                            place_random_subject(s_id, protocol_name, prot_suff, features)
                        else:
                            put_random_subject(s_id, protocol_name, prot_suff, features)
                    put_subjects.append(s_id)
                    if (len(put_subjects) >= max_subject_count):
                        break
                    
                for algorithm, prot_suff in competitors.items():
                    assign_all(protocol_name, prot_suff)
                    groups = get_groups(protocol_name, prot_suff)
                    pp(groups)
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
                            TODO do not use all possible var_options, as we may get zeros in expected frequencies
                            table = np.zeros((len(var_options), len(groups)))
                            for j, group in enumerate(groups):
                                for subject in group['subjects']:
                                    feature_value = subject[var]
                                    table[var_options.index(feature_value), j] += 1
                            r = chi2_contingency(table)
                        append_row(algorithm, max_subject_count, placing,
                                   len(protocols[protocol_name]['variableSpec']),
                                   r.pvalue)

df = pd.DataFrame(d)
print(df)

                
        

