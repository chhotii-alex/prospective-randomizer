import time
import requests
from pathlib import Path

homedir = Path(".")

protocols = {
    "foo" : {
        'groupNames':['A', 'B'],
        'variableSpec': ['weight'],
        'allowRevision': False,
    },
    "bar" : {
        'groupNames':['C', 'D', 'E'],
        'variableSpec': ['score'],
        'allowRevision': True,
    }
}

require_restart = True
if require_restart:
    reply = input("Please shut down the Spring Boot implemenation; hit Enter when that's done")

    for protocol_name in protocols.keys():
        subject_file_path = homedir / ("subjects_%s.txt" % protocol_name)
        subject_file_path.unlink(missing_ok=True)

    print("Please start the Spring Boot implementation now.")
    time.sleep(0.25)
    reply = input("Please hit the Enter key when you see 'Started ProsRandApplication' in the console.")

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
                      json={protocol_spec['variableSpec'][0]: 50},
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
                          "variableSpec": ['froopiness', 'towel_hue'],
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": protocol_spec['groupNames'] + ['another'],
                          "variableSpec": protocol_spec['variableSpec'],
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": protocol_spec['groupNames'] + ['another'],
                          "variableSpec": protocol_spec['variableSpec'] + ['loft'],
                          "allowRevision": protocol_spec['allowRevision']}
                    )
    assert r.status_code == 400
    

    r = requests.post(make_url(True, 'subject/s01'),
                      json={protocol_spec['variableSpec'][0]: 50},
                    )
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s01'),
                      json={protocol_spec['variableSpec'][0]: 100},
                    )
    if protocol_spec['allowRevision']:
        assert r.status_code == 200
    else:
        assert r.status_code == 400

    r = requests.get(make_url(True, 'subject/s01/group'))
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s02/group'),
                      json={protocol_spec['variableSpec'][0]: 150},
                     )
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s02/commit'),
                      json={},
                     )
    assert r.status_code == 200

    r = requests.get(make_url(True, 'subject/s02/committed'))
    assert r.status_code == 200
    assert r.text == 'true'


    r = requests.get(make_url(True, 'subject/s01/committed'))
    assert r.status_code == 200
    assert r.text == 'false'

    r = requests.get(make_url(True, 'groups'))
    assert r.status_code == 200
    returned_groups = r.json()
    print(returned_groups)
    assert len(protocol_spec['groupNames']) == len(returned_groups)
    for g in protocol_spec['groupNames']:
        assert g in returned_groups

    r = requests.get(make_url(True, 'variables'))
    assert r.status_code == 200
    returned_vars = r.json()
    assert len(protocol_spec['variableSpec']) == len(returned_vars)
    for g in protocol_spec['variableSpec']:
        assert g in returned_vars

    r = requests.get(make_url(True, 'subjects'))
    assert r.status_code == 200
    print(r.json())
    # TODO inspect these results

    r = requests.post(make_url(True, 'subject/s03'),
                      json={protocol_spec['variableSpec'][0]: 75},
                    )
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s04'),
                      json={protocol_spec['variableSpec'][0]: 60},
                    )
    assert r.status_code == 200


    r = requests.post(make_url(True, 'assignall'),
                      json={})
    assert r.status_code == 200

    r = requests.get(make_url(True, 'subjects'))
    assert r.status_code == 200
    for s in r.json().values():
        assert "groupName" in s
        assert s["groupName"] in protocol_spec['groupNames']

for protocol_name, protocol_spec in protocols.items():
    run_test(protocol_name, protocol_spec)
print()
print("Success, done!")

    
