"""
Use this script to test that any change to the code that should not make any change to the results of the algorithm, doesn't.
Before altering code, run this script (requires the Spring Boot implementation is up.) This create test_results.csv.
Stash that file away. After changing code, re-run this script, and diff the old and new versions of test_results.csv.
They should be identical.

# TODO: there's a bunch of code duplication between this, simulation.py, and end_to_end.py; DRY
"""

import os
import time
import requests
from pathlib import Path
import random
import numpy as np
import pandas as pd
from collections import defaultdict
from tqdm import tqdm
from pprint import pp

random.seed(2008)

def interleave(d1, d2):
    as_list = zip(d1, d2)
    return [item for items in as_list for item in items]

groupNames = ['W', 'X', 'Y', 'Z']
variable_defs = {
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
variables = {}
for the_type, varbyname in variable_defs.items():
    variables[the_type] = []
    for name, levels in varbyname.items():
        new_var_spec = {'name': name, 'type': the_type, 'levels': levels}
        variables[the_type].append(new_var_spec)
pp(variables, indent=4)

protocols = {}
for n_groups in range(2, 5):
    protocol_groups = groupNames[:n_groups]
    for n_vars in range(1, 3):
        variable_types = ["continuous", "categorical"]
        if n_vars > 1:
            variable_types += ["both"]
        for variable_type in variable_types:
            if variable_type == 'both':
                protocol_vars = interleave(*variables.values())
            else:
                protocol_vars = variables[variable_type]
            all_vars = protocol_vars[:4]
            protocol_vars = protocol_vars[:n_vars]
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
        name = var['name']
        if var['type'] == 'categorical':
            choices = var['levels']
            data[name] = random.choice(choices)
        else:
            data[name] = random.gauss(mu=50, sigma=20)
    return data

def make_features_for(protocol_name):
    all_features = make_phony_features(protocols[protocol_name]['allVars'])
    features = {}
    for onevar in protocols[protocol_name]['spec']['variableSpec']:
        features[onevar['name']] = all_features[onevar['name']]
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

rep_count = 2
def pid_gen():
    for num in range(rep_count):
        yield 'P' + str(num).zfill(2)

max_subject_count = 10

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
    for var in protocols[protocol_name]['allVars']:
        is_used = len([v for v in protocols[protocol_name]['spec']['variableSpec'] if v['name'] == var['name']]) > 0
        if var['type'] == 'continuous':
            continuous = True
        else:
            continuous = False
        means = {}
        for group in groups:
            feature_values = [all_features_by_subject[s['id']][var['name']] for s in group['subjects']]
            if not continuous:
                feature_values = [np.power(max_subject_count, var['levels'].index(value)) for value in feature_values]
            means[group['name']] = np.mean(feature_values)
        for groupName in groupNames:
            if groupName not in means:
                means[groupName] = None
        append_row(algorithm=algorithm, place_interval=place_interval,
                   var_name=var['name'],
                   is_used=is_used)
        append_row(**means)

def compare_algorithms(protocol_name, competitors, all_features_by_subject):
    for algorithm, prot_suff in competitors.items():
        evaluate_protocol_result(protocol_name, prot_suff, algorithm, all_features_by_subject)

def stop_protocols(protocol_name, competitors):
    for _, prot_suff in competitors.items():
        stop_protocol(protocol_name, prot_suff)

setup()

place_interval_options = [0, 3]
perm_count = rep_count*len(protocols)*len(place_interval_options)
print(perm_count)
with tqdm(total=perm_count) as pbar:
    exp_num = 0
    for pid in pid_gen():
        for protocol_name in protocols.keys():
            for place_interval in place_interval_options:
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
        df.to_csv('test_results.csv', index=False)
