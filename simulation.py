import os
import time
import requests
from pathlib import Path
import random
from pprint import pp

random.seed(42)

protocols = {
    "simpletest" : {
        'groupNames': ['C', 'D'],
        'variableSpec': {'score': None},
        'allowRevision': True,
        'algorithm': 'Balanced',
    }
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
    print(url)
    r = requests.post(url,
                      json=protocols[protocol_name])
    assert r.status_code == 200

# This submits feature values but doesn't demand immediate group assignment
def put_random_subject(subjectID, protocol_name, pid):
    r = requests.post(make_url(protocol_name, pid, ['subject', subjectID]),
                      json=make_features_for(protocol_name))
    assert r.status_code == 200

# This submits feature values and demands immediate group assignment
def place_random_subject(subjectID, protocol_name, pid):
    r = requests.post(make_url(protocol_name, pid, ['subject', subjectID, 'group']),
                      json=make_features_for(protocol_name))
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
    r = requests.get(make_url(protocol_name, pid, ['groups']))
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

setup();
for pid in pid_gen():
    for protocol_name in protocols.keys():
        for placing in [True, False]:
            for algorithm in ['Alternating', 'Balanced']:
                protocols[protocol_name]['algorithm'] = algorithm
                prot_suff = pid + algorithm + str(placing)
                start_protocol(protocol_name, prot_suff)
                put_subjects = []
                for s_id in id_gen():
                    if placing:
                        place_random_subject(s_id, protocol_name, prot_suff)
                    else:
                        put_random_subject(s_id, protocol_name, prot_suff)
                    put_subjects.append(s_id)
                    if (len(put_subjects) >= 12):
                        break
                assign_all(protocol_name, prot_suff)
                groups = get_groups(protocol_name, prot_suff)
                (min_group_size, max_group_size) = (999999, 0)
                for group_name, group_data in groups.items():
                    group_size = len(group_data['subjects'])
                    if group_size < min_group_size:
                        min_group_size = group_size
                    if group_size > max_group_size:
                        max_group_size = group_size
                pp(groups)
                assert (max_group_size - min_group_size) <= 1

                
        

