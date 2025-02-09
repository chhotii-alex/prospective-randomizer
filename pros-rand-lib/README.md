# Simple Socket / Command Line / .jar Prospective Randomizer

After using maven to compile all the Java code, an implementation of Prospective Randomizer can be run with a command like this:
```
java -cp target/pros-rand-lib-1.0-SNAPSHOT.jar org.sleepandcognition.prosrand.RandomizerServer -g ../groups.txt -r ../variables.xml
```
You may rename `target/pros-rand-lib-1.0-SNAPSHOT.jar` to `server.jar` and copy it to wherever it is needed.

## Simple socket interface

Example code that demonstrates the use of the simple socket interface from within program code is included in this
repo, in the file [my-app/src/main/java/org/sleepandcognition/prosrandclient/App.java](https://github.com/chhotii-alex/prospective-randomizer/blob/main/my-app/src/main/java/org/sleepandcognition/prosrandclient/App.java). 
However, this is not the most robust approach to take, so for new development, use of the [API](#HTTP-Interface) 
is recommended.

Here are the commands that can be entered if Prospective Randomizer is running a command-line interface in a terminal window, or submitted via the socket connection
if running in network mode. Commands are not case-sensitive. However, subject identifier are case sensitive; s1 and S1 would be regarded as different subjects.

Any command starting with # is simply echoed, and has no effect.

HELLO RAND!  
Program responds "HI CLIENT!" Followed by a version identifier.

EXISTS
Must be followed by a subject ID
Program responds with "YES" or "NO" depending on whether there is already a record of this subject ID.

PUT  
PUT must be followed by the subject ID, then each of the variables with their values. For example:
PUT S1 score=4.5 sex=F
Variable name and value must be joined with just an equal sign, no spaces in between.
There must be exactly one space before the subject ID, and before each variable name.
This command tells prospective randomizer that this subject exists, with the given variable values, but does not immediately trigger the addition of the subject to a group.
Thus, PR has the opportunity to gain more information about other subjects before adding this one to a group, which may affect the subject's group assignment.
Program responds with "OK".

GET  
GET followed by a subject ID (one space between the word GET and the subject ID) triggers the assignment of the subject to a group, if not already assigned.
Program responds with the name of the group this subject was assigned to.

PLACE  
Like PUT (see PUT command for syntax), immediately followed by GET.
Program responds with the name of the group this subject was assigned to.

ASSIGN  
Triggers assignment of all known subjects to groups if they haven't already.
Program responds with "OK".

### Sample session transcripts

In these transciprts, any line of text entered by the user starts with a lowercase letter, whereas texts of text emitted by the program Begin With UPPERCASE.

Here we have renamed the `.jar` file that's built in `pros-rand-lib\target` to `server.jar`.
It can be run  in command-line mode in your shell. Here we start it, and greet it to check that it's reporting the version we expect:

```
alex@dandelion pros-rand-lib % java -cp server.jar org.sleepandcognition.prosrand.RandomizerServer -g ../groups.txt -r ../variables.xml -c
hello rand!
HI CLIENT! v5
```

Here we submit the scores for two subjects, but do not ask that they are immediately added to groups, so that later data can be taken into account
for optimal group assignment: 

```
put s1 score=9
OK
put s2 score=1
OK
```

Now, when we  need group assignments for these subjects:
```
get s1
Assigned s1 to A
Current group means:
A: {score=9.0}
B: null
A
get s2
Assigned s2 to B
Current group means:
A: {score=9.0}
B: {score=1.0}
B
```

Here we demonstrate submitting the feature value(s) for a subject and asking for their group assignment in one atomic operation:
```
place s3 score=8
Mean of score, all subjects: 6.000000  Std dev of score: 3.559026
Considering groups:
A: 1    
B: 1    
Assigned s3 to B
Current group means:
A: {score=9.0}
B: {score=4.5}
B
```

Here we attempt to submit a new value for the same subject ID as above. As the program is not in the mode whereby it allows updates, this is refused:
```
place s3 score=4
Attempt to add duplicate subject ID 
?
```

Quit gracefully, and then we have our shell prompt back:
```
quit
OK
alex@dandelion pros-rand-lib % 
```
## Updatable Variables Mode

If the program is started with the -x flag, one has the option to re-submit variable values for a subject, potentially altering the subject's group assignment,
until the subject has been "committed". Note that such a group assignment change can be effected even after the group assignment is taken and used; so be sure
to "COMMIT" a subject at the point in time when the subject starts to proceed down one arm or the other of the study in the real world!

Relevant commands:

COMMIT  
COMMIT followed by a subject ID causes re-submission of variable values to be forbidden from that point in time onwards.
Program responds with "OK".

COMMITTED  
COMMITTED followed by a subject ID looks up whether the COMMIT command was issued for the given subject.
Program responds "YES" or "NO".

Sample session transcript with the revision option on:

Command line is the same, except that we add the `-x` option to the command:
```
alex@dandelion pros-rand-lib % java -cp server.jar org.sleepandcognition.prosrand.RandomizerServer -g ../groups.txt -r ../variables.xml -c -x
```

Now we add some subjects as usual&mdash;submitting their feature values and getting their group assignments:
```
place s1 score=1
Assigned s1 to A
Current group means:
A: {score=1.0}
B: null
A
place s2 score=9
Assigned s2 to B
Current group means:
A: {score=1.0}
B: {score=9.0}
B
place s3 score=8
Mean of score, all subjects: 6.000000  Std dev of score: 3.559026
Considering groups:
A: 1    
B: 1    
Assigned s3 to A
Current group means:
A: {score=4.5}
B: {score=9.0}
A
```

Since subject s3 is not yet committed (which we can find out with the COMMIT command), submitting a new score is allowed, and actually changes the group assignment:
```
committed s3
NO
place s3 score=2
Mean of score, all subjects: 5.000000  Std dev of score: 3.535534
Considering groups:
A: 1    
B: 1    
Assigned s3 to B
Current group means:
A: {score=1.0}
B: {score=5.5}
B
get s3
B
```

We can then lock down s3's group assignment, once they have become a participant in that group:
```
commit s3
OK
place s3 score=9
Attempt to add duplicate subject ID
?
```
