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
from tqdm import tqdm

random.seed(42)

def dict_interleave(d1, d2):
    as_list = [x for xs in zip(d1.items(), d2.items()) for x in xs]
    return dict(as_list)

def dict_subset(d, n):
    return dict(list(d.items())[:n])

groupNames = ['W', 'X', 'Y', 'Z']
variables = {
    "continuous": {
        "score": None,
        "age": None,
        "shoesize": None,
        "iq": None,
    },
    "categorical": {
        'state': ['Iowa', 'Ohio', 'Illinois', 'Pennsylvania'],
        'fruit': ['apple', 'banana', 'cherry', 'durian'],
        'mouse': ['micky', 'minnie'],
        'student': ['BS', 'MS', 'PhD'],
    },
}
protocols = {}
for n_groups in range(2, 5):
    protocol_groups = groupNames[:n_groups]
    for n_vars in range(1, 5):
        variable_types = ["continuous", "categorical"]
        if n_vars > 1:
            variable_types += ["both"]
        for variable_type in variable_types:
            if variable_type == 'both':
                protocol_vars = dict_interleave(*variables.values())
            else:
                protocol_vars = variables[variable_type]
            all_vars = dict_subset(protocol_vars, 4)
            protocol_vars = dict_subset(protocol_vars, n_vars)
            protocol_name = "%d_%d_%s" % (n_groups, n_vars, variable_type)
            protocols[protocol_name] = {
                "spec" : {
                    'groupNames': protocol_groups,
                    'variableSpec': protocol_vars,
                    'allowRevision': False,
                },
                "allVars": all_vars,
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
    all_features = make_phony_features(protocols[protocol_name]['allVars'])
    features = {}
    for key in protocols[protocol_name]['spec']['variableSpec'].keys():
        features[key] = all_features[key]
    return (features, all_features)

def make_url(protocol_name, pid, parts):
    url = 'http://localhost:8080/%s_%s/%s' % (protocol_name, pid, '/'.join(parts))
    return url

def start_protocol(protocol_name, pid):
    url = make_url(protocol_name, pid, ['start?temp=true'])
    r = requests.post(url,
                      json=protocols[protocol_name]['spec'])
    assert r.status_code == 200

# This submits feature values but doesn't demand immediate group assignment
def put_random_subject(subjectID, protocol_name, pid, features):
    r = requests.post(make_url(protocol_name, pid, ['subject', subjectID]),
                      json=features)
    assert r.status_code == 200

# This submits feature values and demands immediate group assignment
def place_random_subject(subjectID, protocol_name, pid, features):
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
    r = requests.get(make_url(protocol_name, pid, ['groups']))
    assert r.status_code == 200
    return r.json()

def stop_protocol(protocol_name, pid):
    url = make_url(protocol_name, pid, ['stop'])
    r = requests.delete(url)
    assert r.status_code == 200

def id_gen():
    num = 1
    while True:
        yield 'S' + str(num).zfill(3)
        num += 1

rep_count = 100
def pid_gen():
    for num in range(rep_count):
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

def make_metacommunity(groups, all_features_by_subject, vars):
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
            s_id = subject['id']
            for key in vars:
                d[key].append(all_features_by_subject[s_id][key])
    X = pd.DataFrame(d)
    metacommunity = Metacommunity(abundance,
                                  similarity=SimilarityFromFunction(similarity_function,
                                                                    X=X))
    return metacommunity

def get_diversity_measure(groups, all_features_by_subject, vars, level, measure):
    m = make_metacommunity(groups, all_features_by_subject, vars)
    results = m.to_dataframe(viewpoint=[0, 1, np.inf],
                             measures=[measure]).set_index(['viewpoint', 'community'])
    return results.loc[(0.0, level), measure]

### Relevant to saving results to DataFrame

d = defaultdict(list)

def append_row(**kwargs):
    for key, value in kwargs.items():
        d[key].append(value)

### Relevant to running many simulations
    
def evaluate_protocol_result(protocol_name, prot_suff, algorithm, all_features_by_subject):
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
    norm_rho = get_diversity_measure(groups,
                                     all_features_by_subject,
                                     protocols[protocol_name]['allVars'],
                                     'metacommunity', 'normalized_rho')
    norm_rho_used = get_diversity_measure(groups,
                                          all_features_by_subject,
                                          protocols[protocol_name]['spec']['variableSpec'],
                                     'metacommunity', 'normalized_rho')
    for var in protocols[protocol_name]['allVars']:
        is_used = var in protocols[protocol_name]['spec']['variableSpec']
        var_options = protocols[protocol_name]['allVars'][var]
        if var_options is None:
            continuous = True
        else:
            continuous = False
        samples = []
        for group in groups:
            samples.append([all_features_by_subject[s['id']][var] for s in group['subjects']])
        if continuous:
            if len(groups) > 2:
                r = f_oneway(*samples)
            else:
                r = ttest_ind(*samples)
        else:
            table = np.zeros((len(var_options), len(groups)))
            for j, group_samples in enumerate(samples):
                for feature_val in group_samples:
                    table[var_options.index(feature_val), j] += 1
            # do not use all possible var_options, as we may get zeros in expected frequencies
            options_to_keep_indices = [i for i in range(len(var_options)) if np.sum(table[i, :]) > 0]
            table = table[options_to_keep_indices, :]
            r = chi2_contingency(table)
        append_row(algorithm=algorithm, n=max_subject_count, place_interval=place_interval,
                   n_vars=len(protocols[protocol_name]['spec']['variableSpec']),
                   n_groups=len(groups),
                   exp=exp_num, var_name=var,
                   pvalue=r.pvalue, norm_rho=norm_rho, norm_rho_used=norm_rho_used,
                   is_used=is_used)

def compare_algorithms(protocol_name, competitors, all_features_by_subject):
    for algorithm, prot_suff in competitors.items():
        evaluate_protocol_result(protocol_name, prot_suff, algorithm, all_features_by_subject)

def stop_protocols(protocol_name, competitors):
    for _, prot_suff in competitors.items():
        stop_protocol(protocol_name, prot_suff)

place_interval_range = 10

setup()

perm_count = rep_count*len(protocols)*place_interval_range
print(perm_count)
with tqdm(total=perm_count) as pbar:
    exp_num = 0
    for pid in pid_gen():
        for protocol_name in protocols.keys():
            for place_interval in range(place_interval_range): 
                exp_num += 1
                competitors = {}
                for algorithm in ['Alternating', 'Balanced']:
                    protocols[protocol_name]['spec']['algorithm'] = algorithm
                    prot_suff = pid + algorithm + str(place_interval) + str(max_subject_count)
                    start_protocol(protocol_name, prot_suff)
                    competitors[algorithm] = prot_suff
                put_subjects = []
                all_features_by_subject = {}
                for count, s_id in enumerate(id_gen()):
                    if (count == max_subject_count):
                        break
                    features, all_features = make_features_for(protocol_name)
                    all_features_by_subject[s_id] = all_features
                    for prot_suff in competitors.values():
                        put_random_subject(s_id, protocol_name, prot_suff, features)
                    put_subjects.append(s_id)
                    if count >= place_interval:
                        for prot_suff in competitors.values():
                            get_group(put_subjects[0], protocol_name, prot_suff)
                        put_subjects.pop(0)
                compare_algorithms(protocol_name, competitors, all_features_by_subject)
                stop_protocols(protocol_name, competitors)
                pbar.update(1)

        df = pd.DataFrame(d)
        df.to_csv('results.csv', index=False)


                
        

