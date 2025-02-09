# <h1> <i>prospective-randomizer</i>: Java server that assigns subjects to matched experimental groups </h1>

- [Introduction](#introduction)
- [Implementation](#implementation)
  - [Compiling and Running](#compiling-and-running)
  - [Simple socket interface](#simple-socket-interface)
  - [HTTP Interface](#http-interface)
  - [Updatable Variables Mode](#updatable-variables-mode)
  - [Limitations and future work](#limitations-and-future-work)
- [Results](#results)

# Introduction

Experimental science seeks to infer causality by isolating the effect of an intervention,
uncontaminated by confounding factors.

Standard teaching on experimental design is, to quote [Munnangi and Boktor 2024](https://pubmed.ncbi.nlm.nih.gov/29262004/):
> Randomized... control trials (RCT) are considered the gold standard of study design...
> Randomization in RCT avoids confounding and minimizes selection bias.
> This enables the researcher to have similar experimental and control groups,
> thereby enabling them to isolate the effect of an intervention.

But is it in fact the _randomization_ that obtains all these advantages? Randomization as compared
to what?

An _observational_ study is likely to be plagued by confounding factors: for example, in a
case-control study contrasting patients with heart disease to healthy subjects, it is difficult to isolate
the precise effect of one factor (such as exercise) given that the cases and controls are likely
to differ on many other factors as well (diet, genetics, socio-economic status, etc.) It is for
this reason that division of subjects into similar intervention and non-intervention groups is the
canonical method of experimentally establishing causality.

But how do we obtain similar groups? Classically the role of _randomization_ is stressed. Essentially,
a coin is flipped; or subjects are alternatively assigned A-B-A-B, with the assumption that the
random factors that influence the exact date of subjects' entry into a study effectively scrambles their
group assignment. This is in contrast to two notoriously bad study designs; one would be the application of some
fixed rule or bias to group assignment ("Shorter people will fit into the experimental rig with less hassle,
so let's put the shorter people in the experimental group..."), or the replacement of a coin or
randomization schedule with a human decision, which allows for whim or bias, either conscious or
unconscious ("Hmm, she looks like the type who would do well on this treatment.") In either case, effects
of the experimental intervention are _not_ isolated, and are confounded by biases in group selection.
However, surely now in the
21st century these are straw men: such studies would indeed be incredibly rare.

The existence of these _bad_ experimental designs demonstrate that it is easy to do _worse_ than
randomization in attempting to make groups similar and isolating intervention effects. But can we do _better_?

The law of large numbers dictates that with sufficiently large group sizes, the mean (of whatever feature)
of a randomly selected group
will approach the population mean, thus ensuring that all randomly selected groups will be similar
in baseline features.
In an ideal world all studies would have large N; however this ideal is often out of reach.
Many studies in fields such as psychology, education, circadian biology, etc. suffer the triple
challenges of stingier funding, more intensive research protocols, and higher variance in
both features and
outcome measures. Economics have hampered researchers' ability to run large-N studies even when they have
the patience and motivation to do so. But at smaller N, sampling error becomes a large problem, with the standard 
error of the mean equal to the standard deviation divided by the square root of the sample size:

$$
s.e.m. = \frac{std dev}{\sqrt{n}}
$$

Thus as the sample size, $n$, decreases, or as the standard deviation increasees, 
the likely sample error increases. Classically variance has been
minimized by selecting subjects from a relatively homogeneous population, _e.g._,
only selecting students between 18 and 22 at
a specific university. Ideally, to avoid confounding factors, one would have identical 
subjects in each experimental group. In
some experiments this is nearly possible, by ordering a litter of six-week-old male inbreed Wistar
lab rats from the same supplier, all kept in the same type of enclosure and fed the same type of chow
their entire lives. But given the difficulties in translating results from animal studies, and the
desire to have broadly applicable results, may studies use human subjects, and apply very few exclusion criteria.
Given the diversity of humans compared to lab animals, it becomes unfortunately likely that group sampling differences 
in a small-N study will have significant baseline (pre-intervention) differences between groups.

In human studies, all measurable features of subjects will have some more or less broad distribution.
Without the resources to go out and search for a subject similar to a subject in group A to put into
group B&dash;in the typical arrangement of taking all comers&mdash;we cannot match subjects, but can
perhaps hope to match groups, at least on selected features. With small-N studies, it is not uncommon for
outcome measures to differ more at baseline than after the experimental intervention.

However, when subjects are
added to the study one at a time (or a few at a time), it is difficult to accurately anticipate the mean
of the subjects who will enroll.

A simple algorithm to add subjects to groups one at a time to keep the mean of one feature approximately the
same between groups: Compare the feature value for each new subject to the mean of all subjects so far;
if that value is less than the overall mean, add the subject to the group with the highest mean, and
if the value is greater, add to the lowest-mean group. This has the complication that the number of subjects
in each group can become quite unequal with an unlucky sequence of subjects. Consider how things go with
in-order assignment of subjects with these values: 5, 6, 9, 1, 13, 2, which winds up with one group having
5 members and the other only one. [Show this?] A compromise solution is to constrain the algorithm to only
add subjects to groups with the least number of subjects already added. Feature values still play a role,
as there may be more than one group tied for having the least subjects. Feature values can be given an
even stronger role if subjects do not need to be added to groups immediately&mdash;that is, if there is some
temporal delay in the protocol between acquiring any given subject's feature value and submitting that to the
algorithm, and acting upon the decision as to which group that subject is in. If values for several subjects
are buffered, even if one group is in the least-filled tier, an optimal decision can be made between the
several subjects as to which subject to assign to that group for optimal group balance. This is done by
swapping roles in the simple algorithm's description: compare the feature mean of the group to the overall
mean; if it is lower, choose the highest-value subject available, and vice versa.

It may be difficult to anticipate what one feature is most important to try to equalize. Fortunately, this
algorithm easily generalizes to multiple dimensions. The feature values in each dimension should be
normalized (centered
and scaled), by subtracting the current mean and dividing by the current standard deviation. A vector
consisting of the mean normalized value of each feature is calculated for each group. The dot product of
each least-filled group's vector and the vector of normalized features for each new subject is calculated, and the
group-subject pairing with the least dot product is selected (that is, negative with the largest magnitude).

A drawback of equalizing more than one feature is that the more features are used, the poorer the
expected equalization on any one dimension.

[maybe work through an example with pictures with arrows]

Non-numeric (i.e. categorical) features can be converted to numeric by encoding as one-hot features. [Do
I need to explain this?] A concern with this approach is that a single feature then potentially becomes
several feature, and thus makes several contributions to the dot product of the feature vectors. This will
give the categorical feature greater importance than other features, unless [... TODO: do I do weighting?]

While this approach could be applied manually (particularly if there is only one feature being equalized),
it is better to computerize the algorithm and leave humans out of the group-assignment loop, thus avoiding
both error and bias. In the realm of psychology, learning and memory, and education, many pre-intervention
assessments are (or can be computerized). Having the computerized task contact a server implementing the
group allocation algorithm allows for the group allocation to be done automatically,
covertly, and within milliseconds of the calculation of the pre-intervention feature.

# Implementation

We have written a Java implementation of the described algorithm. Setup requires identifying a computer that
can run a Java server and be connected to via TCP/IP; this can be deployed in the cloud for multi-site
studies, but may more cheaply be run on a local workstation. A number of other issues would have to be addressed
before rolling out a cloud deployment:
* This implementation does not include any authentication mechanism and thus would have to be wrapped in an
authentication layer (note, an on-campus deployment without adding an authentication layer is essentially "security through obscurity".)
* Information about subjects (their ID's, features, and group assignments) are stored in local text files, rather than a real database,
making these data more challenging to share across virtual hosts.
* Care has to be taken that if subject identifiers can be linked to individuals that any leakage of data would not be a violation of subject privacy.
  
Configuration requires specifying what
the groups are and what features are to be equalized across these groups. 

The algorithm has been wrapped in two different network protocols. The original, simple interface is over
a simple TCP/IP socket. All configuration must be done by manually editing the groups.txt and variables.xml
files. Client task processes open a socket connection and issue simple one-word commands to the server.
The same executable offers command-line interaction, so that researchers may interact with the algorithm
manually.

A more modern approach is taken by the Spring Boot wrapper. This allows client tasks to communicate with the
server using the HTTP protocol.

## Compiling and Running

To obtain the project, install `git`, and execute this command:
`git clone https://github.com/chhotii-alex/prospective-randomizer.git`

Also install:
* Java Development Kit (jdk) version 17.0 or above
* Apache maven

To build the command-line or simple socket implementation, enter these commands in your Terminal,
shell, or PowerShell:

```
cd prospective-randomizer
cd pros-rand-lib
mvn package
```

Then it can be run with a command like this:
`java -cp target/pros-rand-lib-1.0-SNAPSHOT.jar org.sleepandcognition.prosrand.RandomizerServer -g ../groups.txt -r ../variables.xml`

You may rename `target/pros-rand-lib-1.0-SNAPSHOT.jar` to `server.jar` and copy it to wherever it is needed.

To build the Spring Boot version which uses the HTTP protocol, enter the commands:
```
cd prospective-randomizer
mvn install
```

It can then be started using this command:
```
mvn spring-boot:run -pl pros-rand-boot
```

Example code that demonstrates use of Prospective Randomizer appears [in this project](https://github.com/chhotii-alex/example-task).

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

## HTTP Interface

Rather than using a bare TCP/IP socket, for new work, it is recommended that the HTTP protocol is used to communicate
with the Prospective Randomizer server. The SpringBoot implementation, in `pros-rand-boot`, offers the style of HTTP protocol often (inaccuratedly) referred to as a RESTful API: each command is implemented as an endpoint or path, and the response, if any, is
JSON-formatted. Complete detailed specifications of each endpoint is available via the server's SwaggerUI interface,
available via (for example) `http://localhost:8080/swagger-ui/index.html`. API endpoints are roughly equivalent to
the CLI or socket commands listed above. However, there are a couple of interesting improvements.

The Spring Boot implementation allows you to run multiple protocols (each with their own list of groups and their own
set of variables) on the same server independently. The groups and variables (along with a name used to identify
the protocol) are specified in an intial POST message that tells the server to start the protocol. That protocol
name must be used in every future protocol-specific communication with the server.

Endpoints:

GET /version returns the software's version identifier

POST /{protocolName}/start The request body must be a JSON object with two keys: 'groupNames' and 'variableSpec'. 
The value for 'groupNames' must be an array of strings, giving the group names. The value for 'variableSpec' must be
and array of strings, giving feature names. Each feature is assumed to be numeric continuous. The given protocol is
started.

GET /{protocolName}/subject/{id}
Equivalent to "EXISTS" above. Responds with true if there is already record of a subject with this ID; otherwise
responds with a 404 NOT FOUND status.

POST /{protocolName}/subject/{id} The request body must be a JSON object with key-value pairs giving the feature
values for the subject whose id appears in the path.

POST /{protocolName}/subject/{id}/group The request body must be a JSON object with key-value pairs giving the feature
values for the subject whose id appears in the path. This furthermore causes the given subject to be assigned to a 
group and the response gives the name of that group. 

GET /{protocolName}/subject/{id}/group This causes the given subject to be assigned to a group, if needed, and the
response gives the name of that group. The subject's feature values must have already been submitted via one of the
POST messages listed above.

POST /{protocolName}/assignall Response body should be empty. This triggers assigning all unassigned subjects to 
groups.

POST /{protocolName}/subject/{id}/commit Response body should be empty. This causes the given subject to be 
"committed"&mdash;that is, any attempt to submit new feature values for the given subject will be forbidden.

GET /{protocolName}/subject/{id}/committed returns true or false depending on whether the given subject has been
committed.

GET /{protocolName}/subjects returns information about the given protocol's subjects

GET /{protocolName}/variables returns information about the given protocol's variables or feature labels

GET /{protocolName}/groups returns information about the given protocol's groups


## Limitations and future work
* As mentioned above, the "database" of subjects is not implemented as a real database; it's implemented as a simple text file, which is re-written after every transaction. This is okay for small local deployments&mdash;the number of subjects enrolled per unit time is not likely to be fast enough to run into the performance limitations of this approach. It does mean that care has to be taken to move the subject.txt file from machine to machine if the server is moved from one host to another. Generally this is not an issue with a small local study. However, this may make cloud deployment tricky. If the application is containerized, the local file system may not persist across re-starts, and re-starts can happen for various reasons (software crashes, load balancing, etc.) Ideally, SubjectDatabase would be implemented as a real database such as MySQL or Postgres.
* Likewise, relevant configuration of the study protocol such as groups and variables (features) would benefit from being persisted to a real database. Currently they are configured via local text files (for the socket/command-line implementation) or submitted at start-up from a web client (for the Sping Boot/API implementation) and then just held in memory. This is fragile in the face of any reboots, thus too fragile for cloud deployment.

# Results

We performed _in scilico_ experiments to simulate the use of this algorithm and test whether, when, and to what extent it would produce more desirable group randomizations.

### Using the BalancedRandomizer and having it distribute to groups based on one variable results in groups that are more similar than using AlternatingRandomizer.

We performed 100 simulated runs of each of these protocols: one continuous variable and 2 groups, one continuous variable and 3 groups,
one categorical variable and 2 groups, and one categorical variable and 3 groups.
In each case, we simulated enrolling 20 subjects in the protocol. In the cases where a continuous variable was used, the value of the variable was randomly selected from a Gaussian
distribution. When a categorical variable was used, an option was chosen with equal probability for each option.
For each simulated run, two simulated experiments were performed in parallel, which differed in how subjects 
were added to groups: in one, in the traditional manner, where subjects were assigned to groups in strictly alternating order ("Aternating"); and in the other, using the balanced
algorithm described above based on the one simulated variable ("Balanced").
After each simulated subject was submitted to the algorithm, their group assignment was not determined until after 4 additional subjects were submitted. Thus, the Balanced algorithm had
the benefit of more information about the overall sample when it made its decisions than if group assignments had been immediately demanded. (That is, GET came after several PUT commands,
rather than using PLACE.)
After each simulation, we measured the quality of the randomization of the subjects to the 2 or 3 groups.
When there were two groups and a continuous (numeric) feature was used, this measurement was the p-value of a non-paired t-test on the means of the feature values in each group.
When a continuous feature was used and there were three groups, a one-way ANOVA test of means was used.
When a categorical variable was used, the test was a chi-squared test that the distribution of feature levels did not differ between the 2 or 3 groups.
N.B., in this analysis, _higher_ p-values are more desirable&mdash;the more similar groups were, the higher the p-value. Lower p-values would indicate greater dissimilarity between groups at
baseline&mdash;exactly what the Balanced algorithm tries to avoid.

In the groups populated via the Alternating algorithm, these p-values were very close to the 0.5 that we would expect by chance (mean=0.48, median=0.45).

In the groups populated via the Balanced algorithm, these p-values were much higher (mean=0.92, median=0.95). For any given pair of simulations, the p-value resulting from the Balanced algorithm
was greater than from Alternating by 0.44 on average (paired t-test = -30.0,
p = $1.4 \times 10^{-104}$, df=399). The performance of the Balanced algorithm was not inevitibly superior in every case&mdash;the p-value for the Alternating randomizer was greater in 12% of the
simulated runs, because the Alternating algorithm will sometimes yield an ideal grouping by chance. However, the overall dramatic difference is apparent in Figure 1. 
<figure>
  <img src='fig1.png' width='350'>
  <figcaption>Figure 1</figcaption>
</figure>

Drilling down by
number of groups and type of variable, we see that the same pattern appears across these variations (figure 2).

<figure>
  <img src='fig2.png' width='350'>
  <figcaption>Figure 2</figcaption>
</figure>

What proportion of the time would the group distribution actually be a problem when using either algorithm? The Alternating algorithm yielded groups with a p-value of
less than 0.25 in 99 out of 400 runs (spot-on what we would expect from random chance.) For the Balanced algorithm, with the above-described parameters for the simulations, this never happened.

### Accumulating information about more subjects before doing a group assignment improves the algorithm's performance.

We did additional _in scilico_ experiments similar to those described above, but varying the interval between when any given subject's feature values were submitted to the algorithm and when their
group placement was requested. We allowed the algorithm to accumulate data on 0 to 9 additional subjects (but never more than 20 in total) after any one subject's data was submitted and before
their group placement was requested. This number of additional subjects is referred to as the _place interval_.

Increasing the place interval has a positive effect on p-values resulting from using the Balanced algorithm (see figure 3). A considerable advantage is gained by collecting data on just one
additional subjects before group assignment (place interval = 0 mean p-value = 0.86, place interval = 1 mean p-value = 0.90, difference = 0.048, unpaired t-test=-5.3, p= $1.2 \times 10^{-7}$, df=798).
Collecting data on
additional subjects before group assignment improves the algorithm's performance (Pearson correlation coefficent for p-value vs. place intervals between 1 and 9 = 0.95, $p=7.8 \times 10^{-5}$), but the
magnitude of improvement with each additional increment of place interval is not as large as the first (linear regression slope = 0.005, contrast with the slope between 0 and 1 = 0.048 above).

<figure>
  <img src='fig3.png' width='350'>
  <figcaption>Figure 3</figcaption>
</figure>

The implication of this is that when using the Prospective Randomizer, it is more likely that a study will have a good outcome (in terms of having well-matched groups at baseline) if one can have
subjects go though the protocol in parallel, a few at a time, and submit the baseline values for more than one subject before their group assignments; but one need not bend over backwards for this.
The advantage of running many subjects in parallel over just a few is negligible.

### When using more than one variable, p-values are still better for Balanced than Alternating, but less so.

We also simulated protocols in which more than one baseline feature was taken into account for group assignment. For each subject, 4 feature values (all continuous, all categorical, or 2 continuous and
2 categorical) were randomly generated. For various simulated protocols, 1 to 4 of the feature values were submitted to the Prospective Randomizer. When we looked at the p-values of the differences between groups with regards to individual feature values, increasing the number of variables submitted decreased the advantage of Balanced over Alternating for any one variable (see figure 4) 
(Pearson correlation coefficient = -0.09, $p = 2 \times 10^{-162}$).

<figure>
  <img src='fig4.png' width='350'>
  <figcaption>Figure 4</figcaption>
</figure>

### Using a measure of diversity reveals that using multiple variables results in overall more similar groups.

The previous analysis might be read as casting a bad light on the use of more than one variable. Submitting more than one variable does not degrade the performance of the algorithm, but it does diminish
the power of any one dimension to drive group assignment. Like sibings, the dimensions may not always agree, and have to share. The question is, by using more than one feature, are the subjects
_overall_ better randomized, taking all dimensions into account?

Human subjects are extremely diverse. _Diversity_ can be given an exact mathematical definition, related to measures of entropy. The mathematics of quantifying diversity has been well-developed
in the field of ecology [cite L&C]. Fortunately for us, the ecoologists have thought long and hard about how to quantify the partitioning of diversity&mdash;i.e. how much diversity there is
between subgroups ("subcommunities") of a larger overall group ("metacommunity") [cite Reeve].
This mathematical framework is proving to be useful beyond ecology [cite greylock paper, alpha paper]. A useful measure of what
we are trying to do that is provided by this framework is $\bar{R}$, the average redundancies of the subcommunities; in other words, to what extent are individuals in one group similar to
members of other groups. $\bar{R}$ equals 1.0 when the composition of each group is identical. Rather surprisingly, $\bar{R}$ can exceed 1.0 when typically a member of one group is more
similar to some members of other groups than any member of their own group.

We used the `greylock` python package [citation] to calculate $\bar{R}$ for the group composition of each simulated run of each protocol. Similarity between subjects was defined to take into account
values of all 4 of the features generated for each subject. Either 1, 2, 3, or 4 feature values were submitted to the algorithm consistently throughout each protocol.
As figure 5 shows, the more variables submitted to the algorithm, the higher $\bar{R}$ is on average (Pearson correlation coefficient = 0.25, p = 0).

<figure>
  <img src='fig5.png' width='350'>
  <figcaption>Figure 5</figcaption>
</figure>

[discuss Barsky's study]

my thesis?

# Conclusion







