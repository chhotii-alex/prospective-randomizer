# TODO:
# * test more than one variable
# * test categorical variable
# * test alternating randomizer (-a)

from pathlib import Path
import subprocess

# Functions for communicating with command-line process
# (thanks to Stack Overflow https://stackoverflow.com/questions/19880190/interactive-input-output-using-python)

def start(executable_file):
    file_and_arguments = executable_file.split()
    return subprocess.Popen(
        file_and_arguments,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE)


def read(process):
    response = process.stdout.readline().decode("utf-8").strip()
    print("Response: ")
    print("   ", response)
    return response

def write(process, message):
    print("Sending:")
    print("  ", message)
    process.stdin.write(f"{message.strip()}\n".encode("utf-8"))
    process.stdin.flush()


def terminate(process):
    process.stdin.close()
    process.terminate()
    process.wait(timeout=0.2)


homedir = Path(".")

protocols = {
    "foo" : {
        'groupNames': ['A', 'B'],
        'variables': ['weight'],
        'allowRevision': False,
    },
    "bar" : {
        'groupNames': ['C', 'D', 'E'],
        'variables': ['score'],
        'allowRevision': True,
    }
}

def run_test(protocol_name,
             protocol_spec):
    
    subject_file_path = homedir / ("subjects_%s.txt" % protocol_name)
    subject_file_path.unlink(missing_ok=True)
    with open('groups_%s.txt' % protocol_name, 'w') as f:
        for groupName in protocol_spec['groupNames']:
            print(groupName, file=f)
    with open('variables_%s.xml' % protocol_name, 'w') as f:
        print( '<Variables>', file=f)
        for varName in protocol_spec['variables']:
            print('  <Variable name="%s" type="continuous" />' % varName, file=f)
        print( '</Variables>', file=f)

    command = "java -cp server.jar org.sleepandcognition.prosrand.RandomizerServer  "
    command += "-s %s -g groups_%s.txt -r variables_%s.xml -c " % (subject_file_path, protocol_name, protocol_name)
    if protocol_spec['allowRevision']:
        command += " -x"

    print(command)
    process = start(command)
    read(process)

    # Unknown commands should return "?"
    write(process, "brew my tea")
    response = read(process)
    assert (response == "?")

    write(process, "HELLO RAND!")
    assert (read(process) == "HI CLIENT! v5")

    write(process, "put s01 %s=9" % protocol_spec['variables'][0])
    assert (read(process) == "OK")

    write(process, "put s01 %s=100" % protocol_spec['variables'][0])
    if protocol_spec['allowRevision']:
        assert (read(process) == "OK")
    else:
        assert (read(process) == "?")

    # TODO: parse these responses
    write(process, "get s01")
    while True:
        response = read(process)
        if "group means" in response:
            break
    for g in protocol_spec['groupNames']:
        response = read(process)
    response = read(process)

    write(process, "place s02 %s=150" % protocol_spec['variables'][0])
    while True:
        response = read(process)
        if "group means" in response:
            break
    for g in protocol_spec['groupNames']:
        response = read(process)
    response = read(process)

    write(process, "committed s02")
    response = read(process)
    print("response to 'committed s02': :%s:" % response)
    assert (response == "NO")

    write(process, "commit s02")
    assert (read(process) == "OK")

    write(process, "committed s02")
    assert (read(process) == "YES")

    write(process, "committed s01")
    assert (read(process) == "NO")

    write(process, "place s03 %s=75" % protocol_spec['variables'][0])
    while True:
        response = read(process)
        if "group means" in response:
            break
    for g in protocol_spec['groupNames']:
        response = read(process)
    response = read(process)

    write(process, "place s04 %s=60" % protocol_spec['variables'][0])
    while True:
        response = read(process)
        if "group means" in response:
            break
    for g in protocol_spec['groupNames']:
        response = read(process)
    response = read(process)

    write(process, "assign")
    assert (read(process) == "OK")

for protocol_name, protocol_spec in protocols.items():
    run_test(protocol_name, protocol_spec)
print()
print("Success, done!")

    
