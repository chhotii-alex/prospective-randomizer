import time
import requests
from pathlib import Path
import random
import traceback

random.seed(42)

homedir = Path(".")

protocols = {
    "simpletest" : {
        'groupNames': ['C', 'D', 'E'],
        'variableSpec': {'score': None},
        'allowRevision': True,
        'algorithm': 'Balanced',
    },
    "multidim" : {
        'groupNames': ['A', 'B'],
        'variableSpec': {'weight': None, 'snark':None},
        'allowRevision': False,
        'algorithm': 'Balanced',
    },
    "withcategorical" : {
        'groupNames': ['C', 'D', 'E'],
        'variableSpec': {'state': ['Montana', 'Wyoming', 'Colorodo']},
        'allowRevision': True,
        'algorithm': 'Balanced',
    },
    "alternating" : {
        'groupNames': ['C', 'D', 'E'],
        'variableSpec': {'state': None},
        'allowRevision': True,
        'algorithm': 'Alternating',
    },
    "colliding": {
        'groupNames': ['A', 'B'],
        'variableSpec': {'package' : ['can', 'jar', 'box'],
                         'verb': ['can', 'may', 'will'],
                         'month': ['may', 'september'],
                         'document': ['will', 'lease', 'divorce']},
        'allowRevision': True,
        'algorithm': 'Balanced',
    },
}

def delete_old_data():
    for protocol_name in protocols.keys():
        subject_file_path = homedir / ("subjects_%s.txt" % protocol_name)
        subject_file_path.unlink(missing_ok=True)

def make_phony_features(variables):
    data = {}
    for var in variables:
        choices = variables[var]
        if choices is None:
            data[var] = random.uniform(1, 99)
        else:
            data[var] = random.choice(choices)
    return data

def run_test(protocol_name,
             protocol_spec):

    def make_url(protocol_specific, endpart):
        url = 'http://localhost:8080/'
        if protocol_specific:
            url = url + protocol_name + "/"
        url = url + endpart
        print(url)
        return url

    r = requests.get(make_url(False, ""))
    assert r.status_code == 200
    assert r.text == "This is the group randomization server."

    # undefined endpoints should return 404
    r = requests.get(make_url(True, "teapot"))
    assert r.status_code == 404

    r = requests.get(make_url(False, "version"))
    assert r.status_code == 200
    assert r.text == "5"

    # not found when not yet started
    r = requests.post(make_url(True, 'subject/s01'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                    )
    assert r.status_code == 404
    
    
    r = requests.post(make_url(True, 'start'),
                      json=protocol_spec)
    print(protocol_spec)
    print(r.status_code)
    assert r.status_code == 200

    # Requesting start of the exact same protocol again should be ok
    r = requests.post(make_url(True, 'start'),
                      json=protocol_spec)
    assert r.status_code == 200

    # But changing anything about the protocol details should result in 400 Bad Request
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": ['x', 'y'],
                          "variableSpec": protocol_spec['variableSpec'],
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": protocol_spec['groupNames'],
                          "variableSpec": {'froopiness':None, 'towel_hue':None},
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": protocol_spec['groupNames'] + ['another'],
                          "variableSpec": protocol_spec['variableSpec'],
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400
    more_vars = {'loft':None}
    more_vars.update(protocol_spec['variableSpec'])
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": protocol_spec['groupNames'],
                          "variableSpec": more_vars,
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400

    # Haven't added this subject yet, thus 404:
    r = requests.get(make_url(True, 'subject/s01'))
    print(r.status_code)
    assert r.status_code == 404
    
    r = requests.post(make_url(True, 'subject/s01'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                    )
    assert r.status_code == 200

    # Now that the subject exists in the databse, is found...
    # This gives us the "EXISTS" functionality
    r = requests.get(make_url(True, 'subject/s01'))
    assert r.status_code == 200
    
    r = requests.post(make_url(True, 'subject/s01'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                    )
    if protocol_spec['allowRevision']:
        assert r.status_code == 200
    else:
        assert r.status_code == 400

    r = requests.get(make_url(True, 'subject/s01/group'))
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s02/group'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                     )
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s11/group'),
                      json=make_phony_features({'gooviness':None})
                      )
    assert r.status_code == 400

    r = requests.post(make_url(True, 'subject/s02/commit'),
                      json={},
                     )
    assert r.status_code == 200

    r = requests.get(make_url(True, 'subject/s02/committed'))
    assert r.status_code == 200
    assert r.text == 'true'

    r = requests.post(make_url(True, 'subject/s02'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                      )
    assert r.status_code == 400
    
    r = requests.get(make_url(True, 'subject/s01/committed'))
    assert r.status_code == 200
    assert r.text == 'false'

    r = requests.get(make_url(True, 'groups'))
    assert r.status_code == 200
    returned_groups = r.json()
    print(returned_groups)
    returned_group_names = [group['name'] for group in returned_groups]
    assert len(protocol_spec['groupNames']) == len(returned_groups)
    for g in protocol_spec['groupNames']:
        assert g in returned_group_names

    r = requests.get(make_url(True, 'variables'))
    assert r.status_code == 200
    returned_vars = r.json()
    assert len(protocol_spec['variableSpec']) == len(returned_vars)
    for g in protocol_spec['variableSpec']:
        assert g in returned_vars

    r = requests.get(make_url(True, 'subjects'))
    assert r.status_code == 200
    subjects = r.json()
    assert len(subjects) == 2
    for feature in subjects[0]['features'].keys():
        feature in protocol_spec['variableSpec']

    r = requests.post(make_url(True, 'subject/s03'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                    )
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s04'),
                      json=make_phony_features(protocol_spec['variableSpec'])
                    )
    assert r.status_code == 200


    r = requests.post(make_url(True, 'assignall'),
                      json={})
    assert r.status_code == 200

    r = requests.get(make_url(True, 'subjects'))
    assert r.status_code == 200
    subjects = r.json()
    subject_ids = [s['id'] for s in subjects]
    for num in range(1, 5):
        assert ('s%s' % str(num).zfill(2)) in subject_ids
    for s in subjects:
        assert "groupName" in s
        assert s["groupName"] in protocol_spec['groupNames']

    stop_endpoint = make_url(True, 'stop')
    r = requests.delete(stop_endpoint)
    assert r.status_code == 200
    
    r = requests.delete(stop_endpoint)
    assert r.status_code == 404

delete_old_data()
print("Please start the Spring Boot implementation now.")
time.sleep(0.25)
reply = input("Please hit the Enter key when you see 'Started ProsRandApplication' in the console.")
try:
    for protocol_name, protocol_spec in protocols.items():
        run_test(protocol_name, protocol_spec)
    print()
    print("Success, done!")
except Exception as e:
    print(e)
    traceback.print_stack()
finally:
    delete_old_data()


    
