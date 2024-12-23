import time
import requests
from pathlib import Path

homedir = Path(".")

protocols = {
    "foo" : ['A', 'B'],
    "bar" : ['C', 'D', 'E']
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

def run_test(protocol_name = "foo",
             groups = ["C", "D"],
             variables = ["weight"]):
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
                      json={'weight': 50},
                    )
    print(r.status_code)
    # TODO: this should be a 404, not 500
    assert r.status_code != 200
    
    
    r = requests.post(make_url(True, 'start'),
                    json={"groupNames": groups, "variableSpec": variables, "allowRevision": False}
                    )
    assert r.status_code == 200


    r = requests.post(make_url(True, 'subject/s01'),
                      json={'weight': 50},
                    )
    assert r.status_code == 200

    r = requests.get(make_url(True, 'subject/s01/group'))
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s02/group'),
                      json={'weight': 150},
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
    assert len(groups) == len(returned_groups)
    for g in groups:
        assert g in returned_groups

    r = requests.get(make_url(True, 'variables'))
    assert r.status_code == 200
    returned_vars = r.json()
    assert len(variables) == len(returned_vars)
    for g in variables:
        assert g in returned_vars

    r = requests.get(make_url(True, 'subjects'))
    assert r.status_code == 200
    print(r.json())
    # TODO inspect these results

    r = requests.post(make_url(True, 'subject/s03'),
                      json={'weight': 75},
                    )
    assert r.status_code == 200

    r = requests.post(make_url(True, 'subject/s04'),
                      json={'weight': 60},
                    )
    assert r.status_code == 200


    r = requests.post(make_url(True, 'assignall'),
                      json={})
    assert r.status_code == 200

    r = requests.get(make_url(True, 'subjects'))
    assert r.status_code == 200
    for s in r.json().values():
        assert "groupName" in s
        assert s["groupName"] in groups

for protocol_name, groups in protocols.items():
    run_test(protocol_name, groups=groups)
print("Success, done!")

    
