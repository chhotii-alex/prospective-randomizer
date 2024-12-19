# prospective-randomizer
Java server that assigns subject to matched experimental groups

Experimental science seeks to infer causality by isolating the effect of an intervention,
uncontaminated by confounding factors.

Standard teaching on experimental design is, to quote Munnangi and Boktor 2024
[https://pubmed.ncbi.nlm.nih.gov/29262004/]:
"Randomized... control trials (RCT) are considered the gold standard of study design...
Randomization in RCT avoids confounding and minimizes selection bias.
This enables the researcher to have similar experimental and control groups,
thereby enabling them to isolate the effect of an intervention."

But is it in fact the _randomization_ that obtains all these advantages? Randomization as compared
to what?

An _observational_ study is likely to be plagued by confounding factors: for example, in a
case-control study contrasting heart disease with healthy subjects, it is difficult to isolate
the precise effect of one factor (such as exercise) given that the cases and controls are likely
to differ on many other factors as well (diet, genetics, socio-economic status, etc.) It is for
this reason that division of subjects into similar intervention and non-intervention groups is the
canonnical method of experimentally establishing causality.

But how do we obtain similar groups? Classically the role of _randomization_ is stressed. Essentially,
a coin is flipped; or subjects are alternatively assigned A-B-A-B, with the assumption that the
random factors that influence the exact date of subjects' entry into a study effectively scrambles their
ordering. This is in contrast to two notoriously bad study designs; one would be the application of some
fixed rule or bias to group assignment ("Shorter people will fit into the experimental rig with less bother,
so let's put the shorter people in the experimental group..."), or the replacement of a coin or
randomization schedule with a human decision, which allows for whim or bias, either conscious or
unconscious ("Hmm, she looks like the type who has done well on this treatment.") In either case, effects
of the experimental intervention are _not_ isolated, and are confounded by biases in group selection.
However, surely now in the
21st century these are straw men: only the most venially corrupt researcher would apply a static rule
or whim to group assignment (and given such corruption, anything is possible, including faking data at any
stage).

The existance of these _bad_ experimental designs demonstrate that it is easy to do _worse_ than
randomization in making groups similar and isolating intervention effects. But can we do _better_?

The law of large numbers dictates that with sufficiently large group sizes, the mean (of whatever feature)
of a randomly selected group
will approach the population mean, thus ensuring that all randomly selected groups will be similar.
In an ideal world all studies would have large N; however this ideal may be out of reach.
Many studies in fields such as psychology, education, circadian biology, etc. suffer the triple
challenges of stingier funding, more intensive research protocols per subject, and higher variance in
both features and
outcome measures. Economics have hampered researchers' ability to run large-N studies even when they have
the patience and motivation to want to. At smaller N, sampling error becomes a large problem.
[math and numbers here]

Sampling error is [relationship to] the variance in the population. Classically variance has been
minimized by selecting subjects from a small population&mdash;only selecting students between 18 and 22 at
a specific university, for example. However, universities have become increasingly accessible to more
diverse backgrounds, and granting agencies have noticed and taken a dim view of studies only on narrow
privileged populations, such as the all-male all-white elite university student cohort. Given the resource
constraints on many studies and the desire to have broadly-applicable results, many studies apply very few
exclusion criteria, and take nearly all comers as subjects&mdash;often a motley crew indeed. Thus, it becomes
increasingly probable that sampling error in a small-N study will result in baseline (pre-intervention)
differences between groups.

This can abolish the information-gaining value of a study. In Bayesian terms, we would hope that our
confidence in a hypothesis would be updated by the results of an experiment according to the formula:

probabilty(hypothesis given results)
    = probability(results given hypothesis) x probability(hypothesis) / probability(results)

If there is a difference between groups at baseline, the probability of a difference between groups
post-intervention is extremely high&mdash;regardless of the validity of the hypothesis. Thus, the
probability(results given hypothesis) and probability(results) terms cancel out, yielding almost no
power to update our confidence in the hypothesis.

Let's look at the interpretation difficulties that have plagued previous studies with inadvertent
baseline difference.
[Bob please supply some examples to discuss here]

## A new direction

Ideally, to avoid confounding factors, one would have identical subjects in each experimental group. In
some experiemnts this is nearly possible, by ordering a flat-pack of six-week-old male inbreed Wistar
lab rats from the same supplier, all kept in the same type of enclosure and fed the same type of chow
their entire lives.

In human studies, all measurable features of subjects will have some more or less broad distribution.
Without the resources to go out and search for a subject similar to a subject in group A to put into
group B&dash;in the typical arrangement of taking all comers&mdash;we cannot match subjects, but can
perhaps hope to match groups, at least in their means on selected features. However, when subjects are
added to the study one at a time (or a few at a time), it is difficult to accurately anticipate the mean
of the subjects who will enroll.

An algorithm to add subjects to groups one at a time to keep the mean of one feature approximately the
same between groups: Compare the feature value for each new subject to the mean of all subjects so far;
if that value is less than the overall mean, add the subject to the group with the highest mean, and
if the value is greater, add to the lowest-mean group.

It may be difficult to anticipate what one feature is most important to try to equalize. Fortunately, this
algorithm easily generalizes to multiple dimensions. The feature values in each dimension should be
normalized (centered
and scaled), by subtracting the current mean and dividing by the current standard deviation. A vector
consisting of the mean normalized value of each feature is calculated for each group. The dot product of
each group's vector and the vector of normalized features for the new subject is calculated, and the
group with the least dot prodcut is selected (that is, negative with the largest magnitude).

A drawback of equalizing more than one feature is that the more features are used, the poorer the
expected equalization on any one dimension.

[maybe work through an example with pictures with arrows]

Non-numeric (i.e. categorical) features can be converted to numeric by encoding as one-hot features. [Do
I need to explain this?]

While this approach could be applied manually (particularly if there is only one feature being equalized),
it is better to computerize the algorithm and leave humans out of the group-assignment loop, thus avoiding
both error and bias. In the realm of psychology, learning and memory, and education, many pre-intervention
assessments are (or can be computerized). Having the computerized task contact a server implmenting the
group allocation algorithm allows for the group allocation to be done automatically,
covertly, and within milliseconds of the calculation of the pre-intervention feature.

## Implementation

We have written a Java implementation of the described algorithm. Setup requires identifying a computer that
can run a Java server and be connected to via TCP/IP; this can be deployed in the cloud for multi-site
studies, but may more cheaply be run on a local workstation. Configuration requires specifying what
the groups are and what features are to be equalized across these groups. The algorithm's performance is
only good if the specified group sizes are equal or nearly equal.

The algorithm has been wrapped in two different network protocols. The original, simple interface is over
a simple TCP/IP socket. All configuration must be done by manually editing the groups.txt and variables.xml
files. Client task processes open a socket connection and issue simple one-letter commands to the server.

A more modern approach is taken by the Spring Boot wrapper. This allows client tasks to communicate with the
server using the HTTP protocol.

Example code that demonstrates use of each networking protocol [here] [describe]

## Results

simulation results

discuss Barsky's study

my thesis?

## Conclusion







