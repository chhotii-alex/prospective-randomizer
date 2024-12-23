import time
import requests
from pathlib import Path

homedir = Path(".")


protocol_name = "foo"
groups = ["C", "D"]
variables = ["weight"]

require_restart = True

subject_file_path = homedir / ("subjects_%s.txt" % protocol_name)
subject_file_path.unlink(missing_ok=True)

if require_restart:
    reply = input("Please shut down the Spring Boot implemenation; hit Enter when that's done")

    print("Please start the Spring Boot implementation now.")
    time.sleep(0.25)
    reply = input("Please hit the Enter key when you see 'Started ProsRandApplication' in the console.")

r = requests.get('http://localhost:8080/')
assert r.status_code == 200
assert r.text == "This is the group randomization server."

r = requests.get('http://localhost:8080/version')
assert r.status_code == 200
assert r.text == "5"

r = requests.post('http://localhost:8080/foo/start',
                json={"groupNames": groups, "variableSpec": variables, "allowRevision": False}
                )
assert r.status_code == 200


r = requests.post('http://localhost:8080/foo/subject/s01',
                  json={'weight': 50},
                )
assert r.status_code == 200

r = requests.get('http://localhost:8080/foo/subject/s01/group')
assert r.status_code == 200

r = requests.post('http://localhost:8080/foo/subject/s02/group',
                  json={'weight': 150},
                 )
assert r.status_code == 200

r = requests.post('http://localhost:8080/foo/subject/s02/commit',
                  json={},
                 )
assert r.status_code == 200

r = requests.get('http://localhost:8080/foo/subject/s02/committed')
assert r.status_code == 200
assert r.text == 'true'


r = requests.get('http://localhost:8080/foo/subject/s01/committed')
assert r.status_code == 200
assert r.text == 'false'

r = requests.get('http://localhost:8080/foo/groups')
assert r.status_code == 200
returned_groups = r.json()
assert len(groups) == len(returned_groups)
for g in groups:
    assert g in returned_groups

r = requests.get('http://localhost:8080/foo/variables')
assert r.status_code == 200
returned_vars = r.json()
assert len(variables) == len(returned_vars)
for g in variables:
    assert g in returned_vars

r = requests.get('http://localhost:8080/foo/subjects')
assert r.status_code == 200
print(r.json())
# TODO inspect these results

r = requests.post('http://localhost:8080/foo/subject/s03',
                  json={'weight': 75},
                )
assert r.status_code == 200

r = requests.post('http://localhost:8080/foo/subject/s04',
                  json={'weight': 60},
                )
assert r.status_code == 200


r = requests.post('http://localhost:8080/foo/assignall',
                  json={})
assert r.status_code == 200

r = requests.get('http://localhost:8080/foo/subjects')
assert r.status_code == 200
for s in r.json().values():
    assert "groupName" in s
    assert s["groupName"] in groups

print("Success, done!")

    
